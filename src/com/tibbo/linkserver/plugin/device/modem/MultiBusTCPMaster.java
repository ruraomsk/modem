/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tibbo.linkserver.plugin.device.modem;

/**
 *
 * @author Rura
 */
import com.tibbo.aggregate.common.Log;
import java.net.InetAddress;
import java.net.UnknownHostException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.util.BitVector;
import com.tibbo.aggregate.common.util.AggreGateThread;
import com.tibbo.aggregate.common.util.ThreadManager;
import java.util.LinkedList;
import java.util.List;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.procimg.SimpleInputRegister;
import net.wimpi.modbus.procimg.SimpleRegister;
// Referenced classes of package com.tibbo.linkserver.plugin.device.modbus.master:
//            ModbusMaster

public class MultiBusTCPMaster
{

    private TCPMasterConnection m_Connection;
    private InetAddress m_SlaveAddress;
    private ModbusTCPTransaction m_Transaction;
    private ReadCoilsRequest m_ReadCoilsRequest;
    private ReadInputDiscretesRequest m_ReadInputDiscretesRequest;
    private WriteCoilRequest m_WriteCoilRequest;
    private WriteMultipleCoilsRequest m_WriteMultipleCoilsRequest;
    private ReadMultipleRegistersRequest m_ReadMultipleRegistersRequest;
    private WriteSingleRegisterRequest m_WriteSingleRegisterRequest;
    private WriteMultipleRegistersRequest m_WriteMultipleRegistersRequest;
    private BitVector DataCoils;
    private BitVector DataDI;
    private short[] DataIR;
    private short[] DataHR;
    private int countIR, countHR;
    private boolean m_Reconnecting;
    private int m_Retries;
    public MultiBus param;
    private final int MaxLen = 100;
    private ReadMultiTCP thRead = null;
    private ThreadManager thrManager = new ThreadManager();
    public boolean flagStop = false;
    Integer port;
    String addr;
    public List<writeRegisters> quwery = null;
    public boolean isCoilsWrite = false;
    public boolean isHoldWrite = false;

    public MultiBusTCPMaster(String addr, int port, MultiBus PARAM)
    {
        this.port = port;
        this.param = PARAM;
        this.addr = addr;
        m_Reconnecting = false;
        m_Retries = 3;
        try
        {
//            Log.CORE.info("MultiBusTCPMaster in");
            m_SlaveAddress = InetAddress.getByName(addr);
            m_Connection = new TCPMasterConnection(m_SlaveAddress);
            m_Connection.setPort(port);
            m_Connection.setTimeout(1000);
            m_ReadCoilsRequest = new ReadCoilsRequest();
            m_ReadInputDiscretesRequest = new ReadInputDiscretesRequest();
            m_WriteCoilRequest = new WriteCoilRequest();
            m_WriteMultipleCoilsRequest = new WriteMultipleCoilsRequest();
            if (param.lenCoils > 0)
            {
                DataCoils = new BitVector(param.lenCoils);
            }
            if (param.lenDI > 0)
            {
                DataDI = new BitVector(param.lenDI);
            }
            if (param.lenIR > 0)
            {
                DataIR = new short[param.lenIR];
            }
            if (param.lenHR > 0)
            {
                DataHR = new short[param.lenHR];
            }
            for (int i = 0; i < param.lenIR; i++)
            {
                DataIR[i] = 0;
            }
            for (int i = 0; i < param.lenHR; i++)
            {
                DataHR[i] = 0;
            }
            //for (Register DataHR1 : DataHR) {
            //    DataHR1.setValue(0);
            //}
            m_ReadMultipleRegistersRequest = new ReadMultipleRegistersRequest();
            m_WriteSingleRegisterRequest = new WriteSingleRegisterRequest();
            m_WriteMultipleRegistersRequest = new WriteMultipleRegistersRequest();
            if (param.flagMass)
            {
                quwery = new LinkedList();
            }
            //Log.CORE.info("MultiBusTCPMaster out");
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void connect()
            throws Exception
    {

        if (m_Connection != null && !m_Connection.isConnected())
        {
            //Log.CORE.info("MultiBusTCPMaster connect " + addr + ":" + Integer.toString(port));

            m_Connection.connect();
            m_Transaction = new ModbusTCPTransaction(m_Connection);
            m_Transaction.setReconnecting(m_Reconnecting);
            m_Transaction.setRetries(m_Retries);
            if (!m_Connection.isConnected())
            {
                Log.CORE.info("MultiBusTCPMaster is not connect");
                return;
            }
            //readAllCoils();
            //readAllInputDiscretes();
            //readAllInputRegisters();
            //readAllMultipleRegisters();
            flagStop = false;
            if (thRead == null)
            {
                thRead = new ReadMultiTCP(this, thrManager);
            }
            //Log.CORE.info("MultiBusTCPMaster endconnect");
        }
    }

    public boolean isconnected()
    {
        return m_Connection.isConnected();
    }

    public synchronized void disconnect()
    {
        if (m_Connection != null)
        {
            m_Connection.close();
            m_Transaction = null;
            flagStop = true;
            //Log.CORE.info("MultiBusTCPMaster disconnect " + addr + ":" + Integer.toString(port));
            if (thRead != null)
            {
                thrManager.interruptThread(thRead);
                try
                {
                    thRead.join();
                }
                catch (InterruptedException ex)
                {
                    Log.CORE.info("MultiBusTCPMaster dont wait thread  " + addr + ":" + Integer.toString(port));
                }
                thRead = null;
            }
        }
    }

    public void setReconnecting(boolean b)
    {
        m_Reconnecting = b;
        if (m_Transaction != null)
        {
            m_Transaction.setReconnecting(b);
        }
    }

    public void setRetries(int retries)
    {
        m_Retries = retries;
        if (m_Transaction != null)
        {
            m_Transaction.setRetries(retries);
        }
    }

    public boolean isReconnecting()
    {
        return m_Reconnecting;
    }

    public synchronized BitVector readCoils(int unitid, int ref, int count)
    {
        BitVector bv = new BitVector(count);
        for (int i = 0; i < count; i++)
        {
            bv.setBit(i, DataCoils.getBit(i + ref));
        }
        return bv;
    }

    public synchronized void readAllCoils()
    {
        if (param.lenCoils == 0)
        {
            return;
        }
        if (!isconnected())
        {
            return;
        }
        m_ReadCoilsRequest.setUnitID(1);
        m_ReadCoilsRequest.setReference(0);
        m_ReadCoilsRequest.setBitCount(param.lenCoils);
        m_Transaction.setRequest(m_ReadCoilsRequest);
        try
        {
            m_Transaction.execute();
            DataCoils = ((ReadCoilsResponse) m_Transaction.getResponse()).getCoils();
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster readAllCoils" + ex.toString());
            clearTransaction();
        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster readAllCoils" + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }

    }

    public synchronized boolean writeCoil(int unitid, int ref, boolean state)
    {
        //if(param.flagMass) 
        //{
        //    writeCoilToQuwery(ref,state);
        //    DataCoils.setBit(ref, state);
        //    return state;
        //}
        //lock.lock();
        if (!isconnected())
        {
            return false;
        }
        m_WriteCoilRequest.setUnitID(unitid);
        m_WriteCoilRequest.setReference(ref);
        m_WriteCoilRequest.setCoil(state);
        m_Transaction.setRequest(m_WriteCoilRequest);
        //Log.CORE.info("MultiBusTCPMaster writeCoil ");
        try
        {
            m_Transaction.execute();
            DataCoils.setBit(ref, state);
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeCoil " + ex.toString());
            clearTransaction();
        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeCoil " + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();

        }
        finally
        {
            //lock.unlock();
        }
        return state;

    }

    public synchronized void writeMultipleCoils(int unitid, int ref, BitVector coils)
    {
        //if(param.flagMass) 
        //{
        //    writeCoilsToQuwery(ref,coils);
        //    for (int i = 0; i < coils.size(); i++)
        //    {
        //        DataCoils.setBit(ref + i, coils.getBit(i));
        //    }
        //    return ;
        //}
        if (!isconnected())
        {
            return;
        }

        try
        {
            m_WriteMultipleCoilsRequest.setUnitID(unitid);
            m_WriteMultipleCoilsRequest.setReference(ref);
            m_WriteMultipleCoilsRequest.setCoils(coils);
            m_WriteMultipleCoilsRequest.setDataLength(5 + coils.byteSize());
            m_Transaction.setRequest(m_WriteMultipleCoilsRequest);
            //Log.CORE.info("MultiBusTCPMaster writeCoil ");
            m_Transaction.execute();
            for (int i = 0; i < coils.size(); i++)
            {
                DataCoils.setBit(ref + i, coils.getBit(i));
            }
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeMultipleCoil" + ex.toString());
            clearTransaction();
        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeMultipleCoil" + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }
        finally
        {
        }
    }

    public synchronized BitVector readInputDiscretes(int unitid, int ref, int count)
    {
        BitVector bv = new BitVector(count);
        for (int i = 0; i < count; i++)
        {
            bv.setBit(i, DataDI.getBit(i + ref));
        }
        return bv;
    }

    public synchronized void readAllInputDiscretes()
    {
        if (param.lenDI == 0)
        {
            return;
        }
        if (!isconnected())
        {
            return;
        }
        m_ReadInputDiscretesRequest.setUnitID(1);
        m_ReadInputDiscretesRequest.setReference(0);
        m_ReadInputDiscretesRequest.setBitCount(param.lenDI);
        m_Transaction.setRequest(m_ReadInputDiscretesRequest);
        try
        {
            m_Transaction.execute();
            DataDI = ((ReadInputDiscretesResponse) m_Transaction.getResponse()).getDiscretes();
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster readAllInputDiscrets " + ex.toString());
            clearTransaction();
        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster readAllInputDiscrets " + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }
    }

    public synchronized InputRegister[] readInputRegisters(int unitid, int ref, int count)
    {
        InputRegister[] ir = new Register[count];
        for (int i = 0; i < count; i++)
        {
            ir[i] = new SimpleInputRegister(DataIR[ref + i]);
        }
        return ir;
    }

    public synchronized void readAllInputRegisters()
    {
        if (param.lenIR == 0)
        {
            return;
        }
        if (!isconnected())
        {
            return;
        }
        int ref = 0, count = param.lenIR, len;
        InputRegister[] ir = new InputRegister[MaxLen];
        try
        {
            while (count > 0)
            {
                ReadInputRegistersRequest m_ReadInputRegistersRequest;
                m_ReadInputRegistersRequest = new ReadInputRegistersRequest();
                len = (count > MaxLen) ? MaxLen : count;
                m_ReadInputRegistersRequest.setUnitID(1);
                m_ReadInputRegistersRequest.setReference(ref);
                m_ReadInputRegistersRequest.setWordCount(len);
                m_Transaction.setRequest(m_ReadInputRegistersRequest);
                m_Transaction.execute();
                ir = ((ReadInputRegistersResponse) m_Transaction.getResponse()).getRegisters();
                for (int i = 0; i < len; i++)
                {
                    DataIR[ref + i] = ir[i].toShort();
                }
                count -= MaxLen;
                ref += MaxLen;
            }
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster readAllInputRegisters " + ex.toString());

            clearTransaction();

        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster readAllInputRegisters " + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }
    }

    public synchronized void readAllMultipleRegisters()
    {
        if (param.lenHR == 0)
        {
            return;
        }
        if (!isconnected())
        {
            return;
        }
        int ref = 0, count = param.lenHR, len;
        Register[] hr = new Register[MaxLen];
        try
        {
            while (count > 0)
            {
                len = (count > MaxLen) ? MaxLen : count;
                m_ReadMultipleRegistersRequest.setUnitID(1);
                m_ReadMultipleRegistersRequest.setReference(ref);
                m_ReadMultipleRegistersRequest.setWordCount(len);
                m_Transaction.setRequest(m_ReadMultipleRegistersRequest);
                m_Transaction.execute();
                hr = ((ReadMultipleRegistersResponse) m_Transaction.getResponse()).getRegisters();
                for (int i = 0; i < len; i++)
                {
                    DataHR[ref + i] = hr[i].toShort();
                }
                count -= MaxLen;
                ref += MaxLen;
            }
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster readAllMultipleDiscrets " + ex.toString());
            clearTransaction();
        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster readAllMultipleDiscrets " + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }
    }

    public synchronized Register[] readMultipleRegisters(int unitid, int ref, int count)
    {
        //Log.CORE.info("MultiBusTCPMaster readMultipleRegisters in " + Integer.toString(ref) + " " + Integer.toString(count));
        Register[] hr = new Register[count];
        for (int i = 0; i < count; i++)
        {
            hr[i] = new SimpleRegister(DataHR[ref + i]);
        }
        return hr;
    }

    public synchronized void writeSingleRegister(int unitid, int ref, Register register)
    {
        if (param.flagMass)
        {
            writeSinleRegisterToQuwery(ref, register);
            DataHR[ref] = register.toShort();
            return;
        }
        //lock.lock();
        try
        {
            m_WriteMultipleRegistersRequest.setUnitID(unitid);
            m_WriteSingleRegisterRequest.setReference(ref);
            m_WriteSingleRegisterRequest.setRegister(register);
            m_Transaction.setRequest(m_WriteSingleRegisterRequest);
            m_Transaction.execute();
            DataHR[ref] = register.toShort();
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeSingleRegister " + ex.toString());
            clearTransaction();

        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeSingleRegister " + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }
        finally
        {
            //  lock.unlock();
        }
    }

    public synchronized void writeMultipleRegisters(int unitid, int ref, Register registers[])
    {
        //Log.CORE.info("MultiBusTCPMaster writeMultipleRegisters " + Integer.toString(ref) + " " + Integer.toString(registers.length));
        if (param.flagMass)
        {
            writeMultipleRegisterToQuwery(ref, registers);
            for (int i = 0; i < registers.length; i++)
            {
                DataHR[ref] = registers[i].toShort();
            }
            return;
        }
        //lock.lock();
        try
        {
            m_WriteMultipleRegistersRequest.setUnitID(unitid);
            m_WriteMultipleRegistersRequest.setReference(ref);
            m_WriteMultipleRegistersRequest.setRegisters(registers);
            m_Transaction.setRequest(m_WriteMultipleRegistersRequest);
            m_Transaction.execute();
            for (int i = 0; i < registers.length; i++)
            {
                DataHR[ref] = registers[i].toShort();
            }
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeMultipleRegisters " + ex.toString());
            clearTransaction();
        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeMultipleRegisters " + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }
        finally
        {
            //  lock.unlock();
        }
    }

    public void setTimeout(int timeout)
    {
        m_Connection.setTimeout(timeout);
    }

    private void clearTransaction()
    {
        m_Connection.close();
        Log.CORE.info("MultiBusTCPMaster clear " + m_SlaveAddress.toString() + ":" + port.toString());
        m_Transaction = null;
    }

    public synchronized void writeAllMultipleRegisters()
    {
        if (!isconnected())
        {
            return;
        }
        Register[] registers = new Register[DataHR.length];
        for (int i = 0; i < registers.length; i++)
        {
            registers[i] = new SimpleRegister(DataHR[i]);
        }

        try
        {
            m_WriteMultipleRegistersRequest.setUnitID(1);
            m_WriteMultipleRegistersRequest.setReference(0);
            m_WriteMultipleRegistersRequest.setRegisters(registers);
            m_Transaction.setRequest(m_WriteMultipleRegistersRequest);
            m_Transaction.execute();
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeAllMultipleRegisters " + ex.toString());
            clearTransaction();
        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeAllMultipleRegisters " + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }
        finally
        {
            //  lock.unlock();
        }
    }

    public synchronized void writeAllCoils()
    {
        //lock.lock();
        if (!isconnected())
        {
            return;
        }
        try
        {
            m_WriteMultipleCoilsRequest.setUnitID(1);
            m_WriteMultipleCoilsRequest.setReference(0);
            m_WriteMultipleCoilsRequest.setCoils(DataCoils);
            m_WriteMultipleCoilsRequest.setDataLength(10); /// ?????
            //Log.CORE.info("MultiBusTCPMaster writeAllCoil 10 bits");
            m_Transaction.setRequest(m_WriteMultipleCoilsRequest);
            m_Transaction.execute();
        }
        catch (ModbusIOException ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeAllCoil" + ex.toString());
            clearTransaction();
        }
        catch (Exception ex)
        {
            Log.CORE.info("MultiBusTCPMaster writeAllCoil" + ex.toString());
            Log.CORE.info("MultiBusTCPMaster on " + m_SlaveAddress.toString() + ":" + port.toString());
            clearTransaction();
        }
        finally
        {
            //  lock.unlock();
        }
    }

    public synchronized void readQuwery()
    {
        try
        {
            isCoilsWrite = false;
            isHoldWrite = false;
            for (writeRegisters wr : quwery)
            {
                if (wr.type)
                {
                    //Holding registers
                    isHoldWrite = true;
                    DataHR[wr.ref] = wr.register.toShort();
                } else
                {
                    isCoilsWrite = true;
                    DataCoils.setBit(wr.ref, (wr.register.toShort() != 0));
                }
            }
        }
        finally
        {
        }
        quwery.clear();
    }

    private synchronized void writeCoilToQuwery(int ref, boolean state)
    {
        quwery.add(new writeRegisters(ref, state));
    }

    private synchronized void writeCoilsToQuwery(int ref, BitVector coils)
    {
        for (int i = 0; i < coils.size(); i++)
        {
            writeCoilToQuwery(ref + i, coils.getBit(i));
        }
    }

    private synchronized void writeSinleRegisterToQuwery(int ref, Register register)
    {
        quwery.add(new writeRegisters(ref, register));
    }

    private synchronized void writeMultipleRegisterToQuwery(int ref, Register[] registers)
    {
        for (int i = 0; i < registers.length; i++)
        {
            writeSinleRegisterToQuwery(ref + i, registers[i]);
        }

    }

}

class ReadMultiTCP extends AggreGateThread
{

    private MultiBusTCPMaster fd;
    ThreadManager threadManager = null;

    public ReadMultiTCP(MultiBusTCPMaster fd, ThreadManager threadManager)
    {
        super(threadManager);
        this.threadManager = threadManager;
        threadManager.addThread(this);
        this.fd = fd;
        //Log.CORE.info("Запускаем поток! " + fd.addr + ":" + fd.port.toString());
        start();
    }

    @Override
    public void run()
    {
        do
        {
            //Log.CORE.info("В потоке");
            try
            {
                //Log.CORE.info("В потоке "+fd.addr + ":" + fd.port.toString());
                if (!fd.isconnected())
                {
                    break;
                }
                fd.readAllCoils();
                if (!fd.isconnected())
                {
                    break;
                }
                //Log.CORE.info("readAllCoils execute done" );
                fd.readAllInputDiscretes();
                if (!fd.isconnected())
                {
                    break;
                }
                //Log.CORE.info("readAllInpDif(!fd.isconnected()) break;isc execute done" );
                fd.readAllInputRegisters();
                if (!fd.isconnected())
                {
                    break;
                }
                //Log.CORE.info("readAllInpReg execute done" );
                fd.readAllMultipleRegisters();
                if (!fd.isconnected())
                {
                    break;
                }
                //Log.CORE.info("readAllHold execute done" );
                if (fd.param.flagMass)
                {
                    // Разгружаем очередь команд на запись coils & holding registers
                    //
                    fd.readQuwery();

                    // Производим запись регистров
                    if (fd.isCoilsWrite)
                    {
                        fd.writeAllCoils();
                    }
                    if (fd.isHoldWrite)
                    {
                        fd.writeAllMultipleRegisters();
                    }
                }
                AggreGateThread.sleep(fd.param.step);
            }
            catch (InterruptedException ex)
            {
                return;
            }
            finally
            {
            }
        }
        while (!isInterrupted());
    }
}
