/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tibbo.linkserver.plugin.device.modem;

import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.validator.LimitsValidator;
import com.tibbo.aggregate.common.datatable.validator.ValidatorHelper;
import com.tibbo.aggregate.common.device.*;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.common.util.AggreGateThread;
import com.tibbo.aggregate.common.util.ThreadManager;

import com.tibbo.linkserver.plugin.device.modem.item.DataType;
import java.sql.Timestamp;
import java.util.*;
import ruraomsk.list.ru.strongsql.DescrValue;
import ruraomsk.list.ru.strongsql.ParamSQL;
import ruraomsk.list.ru.strongsql.SetData;
import ruraomsk.list.ru.strongsql.SetValue;
import ruraomsk.list.ru.strongsql.StrongSql;

/**
 * Драйвер для Aggregate интерфейса MultiBus
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class ModemDeviceDriver extends AbstractDeviceDriver {

    public ModemDeviceDriver() {
        super("multibus", VFT_CONNECTION_PROPERTIES);
    }

    @Override
    public void setupDeviceContext(DeviceContext deviceContext)
            throws ContextException {
        super.setupDeviceContext(deviceContext);
        deviceContext.setDefaultSynchronizationPeriod(10000L);
        VariableDefinition vd = new VariableDefinition("connectionProperties", VFT_CONNECTION_PROPERTIES, true, true, "connectionProperties", ContextUtils.GROUP_ACCESS);
        vd.setIconId("var_connection");
        vd.setHelpId("ls_drivers_multibus");
        vd.setWritePermissions(ServerPermissionChecker.getManagerPermissions());
        deviceContext.addVariableDefinition(vd);
        vd = new VariableDefinition("registers", VFT_REGISTERS, true, true, "Регистры", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);
        vd = new VariableDefinition("perfect", VFT_PERFECT, true, true, "Эталон", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);
        vd = new VariableDefinition("devices", VFT_DEVICES, true, true, "Устройства", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);
        vd = new VariableDefinition("SSD", VFT_SSD, true, true, "Верхний уроень", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);
        vd = new VariableDefinition("SQLProperties", VFT_SQL, true, true, "Сохранение дампа", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);
        deviceContext.setDeviceType("multibus");
        makeRegisters();
    }

    @Override
    public List<VariableDefinition> readVariableDefinitions(DeviceEntities entities) throws ContextException, DeviceException, DisconnectionException {

        ensureRegisters();

        return createDeviceVariableDefinitions(registers);
    }

    @Override
    public boolean isUseDeviceSideValuesCache() {
        return super.isUseDeviceSideValuesCache(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void accessSettingUpdated(String name) {
        if (name.equals("devices") || name.equals("perfect")) {
            makeRegisters();
        }
        super.accessSettingUpdated(name); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<FunctionDefinition> readFunctionDefinitions(DeviceEntities entities) throws ContextException, DeviceException, DisconnectionException {
        List res = new LinkedList();
        FieldFormat iff;

        TableFormat inputFormat;
        FieldFormat off;
        TableFormat outputFormat;
        FunctionDefinition fd;
        iff = FieldFormat.create("ReConnect", FieldFormat.INTEGER_FIELD, "Номер канала для переподключения");
        inputFormat = new TableFormat(1, 1, iff);
        off = FieldFormat.create("ConnectField", FieldFormat.STRING_FIELD, "Результат переподключения");
        outputFormat = new TableFormat(1, 1, off);
        fd = new FunctionDefinition("ReConnect", inputFormat, outputFormat, "Произвести переподключение канала", ContextUtils.GROUP_DEFAULT);
        res.add(fd);

        iff = FieldFormat.create("SyncPeriod", FieldFormat.BOOLEAN_FIELD, "Установить период опроса");
        inputFormat = new TableFormat(1, 1, iff);
        off = FieldFormat.create("Syncronized", FieldFormat.STRING_FIELD, "Результат синхронизации");
        outputFormat = new TableFormat(1, 1, off);
        fd = new FunctionDefinition("SyncPeriod", inputFormat, outputFormat, "Установить синхронизацию", ContextUtils.GROUP_DEFAULT);
        res.add(fd);

        iff = FieldFormat.create("isConnect", FieldFormat.BOOLEAN_FIELD, "Подтвердите Запрос состояния");
        inputFormat = new TableFormat(1, 1, iff);
        outputFormat = new TableFormat(true);
        outputFormat.addField(FieldFormat.create((new StringBuilder()).append("<IPaddr><S><D=").append("IP address устройства").append(">").toString()));
        outputFormat.addField(FieldFormat.create((new StringBuilder()).append("<port><I><A=502><D=").append("Номер порта").append(">").toString()));
        outputFormat.addField(FieldFormat.create((new StringBuilder()).append("<Status><S><D=").append("Статус соединения").append(">").toString()));
        fd = new FunctionDefinition("Status", inputFormat, outputFormat, "Состояние подключения", ContextUtils.GROUP_DEFAULT);
        res.add(fd);

        inputFormat = new TableFormat(1, 1);
        inputFormat.addField(FieldFormat.create("<name><S><D=Имя переменной>"));
        inputFormat.addField(FieldFormat.create("<from><D><D=Начало периода>"));
        inputFormat.addField(FieldFormat.create("<to><D><D=Конец периода>"));
        TableFormat history = new TableFormat(true);
        history.addField(FieldFormat.create("<series><S><D=Имя серии>"));
        history.addField(FieldFormat.create("<x><D><D=Время>"));
        history.addField(FieldFormat.create("<y><F><D=Значение>"));
        fd = new FunctionDefinition("history", inputFormat, history, "История переменной", ContextUtils.GROUP_DEFAULT);
        res.add(fd);

        return res;
    }

    @Override
    public DataTable executeFunction(FunctionDefinition fd, CallerController caller, DataTable parameters) throws ContextException, DeviceException, DisconnectionException {
        if (fd.getName().equals("ReConnect")) {

            int chanel = parameters.rec().getInt("ReConnect");
            DataTable devs = null;
            try {
                devs = super.getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
            } catch (ContextException ex) {
                Log.CORE.info("Devices not found");
                return new DataTable(fd.getOutputFormat(), "Переподлючение не производилось");
            }
            Integer error = 0;
            Integer device = 0;
            for (DataRecord recdev : devs) {
                String IPaddres = recdev.getString("IPaddr");
                int port = recdev.getInt("port");
                //Log.CORE.info("Device in " + device.toString()+" "+IPaddres);
                if ((device == chanel) || (chanel < 0)) {
                    try {
                        if (!(controller[device] != null && controller[device].isconnected())) {
                            controller[device] = MultiBusTCPController.tcpController(IPaddres, port, PARAM);
                            controller[device].connect();
                        }
                    } catch (Exception ex) {
                        //Log.CORE.info("Device Error " + device.toString() + " " + ex.getMessage());
                        controller[device] = null;
                        error++;
                    }
                }
                device++;
            }
            return new DataTable(fd.getOutputFormat(), "Переподлючение производилось. Ошибок " + error.toString());
        }
        if (fd.getName().equals("SyncPeriod")) {

            boolean flag = parameters.rec().getBoolean("SyncPeriod");
            if (!flag) {
                return new DataTable(fd.getOutputFormat(), "Синхронизация не запрашивалась");
            }

            DataTable devs = null;
            try {
                devs = super.getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
            } catch (ContextException ex) {
                throw new ContextException("Devices not found");
            }

            DataTable perf = null;
            try {
                perf = super.getDeviceContext().getVariable("perfect", getDeviceContext().getCallerController());
            } catch (ContextException ex) {
                throw new ContextException("Perfect not found");
            }

            DataTable sync = null;
            try {
                sync = super.getDeviceContext().getVariable("settingSyncOptions", getDeviceContext().getCallerController());
            } catch (ContextException ex) {
                throw new ContextException("settingSyncOptions not found");
            }

            for (DataRecord recdev : devs) {
                String prefix = recdev.getString("prefix");
                for (DataRecord recperf : perf) {

                    String namereg = recperf.getString("name");
                    long period = recperf.getLong("period");
                    namereg = prefix + namereg;
                    for (DataRecord recsync : sync) {
                        if (namereg.equals(recsync.getString("name"))) {
                            if (period < 1L) {
                                recsync.setValue("historyRate", -1);
                                recsync.setValue("syncPeriod", null);
                            } else {
                                recsync.setValue("historyRate", 0);
                                recsync.setValue("syncPeriod", period);
                            }
                            break;
                        }
                    }
                    String nameConnect = prefix + "Connect";
                    for (DataRecord recsync : sync) {
                        if (nameConnect.equals(recsync.getString("name"))) {
                            recsync.setValue("syncPeriod", PARAM.step * 2);
                            break;
                        }
                    }

                }
            }
            super.getDeviceContext().setVariable("settingSyncOptions", getDeviceContext().getCallerController(), sync);
            return new DataTable(fd.getOutputFormat(), "Синхронизация переменных произведена.");
        }
        if (fd.getName().equals("Status")) {

            boolean flag = parameters.rec().getBoolean("isConnect");
            if (!flag) {
                return new DataTable(fd.getOutputFormat(), "Статус соедениния не запрашивался");
            }
            DataTable devs = null;
            try {
                devs = super.getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
            } catch (ContextException ex) {
                Log.CORE.info("Devices not found");
            }
            DataTable res = new DataTable(fd.getOutputFormat());

            Integer error = 0;
            Integer device = 0;
            for (DataRecord recdev : devs) {
                DataRecord resrec = new DataRecord(res.getFormat());
                String IPaddres = recdev.getString("IPaddr");
                int port = recdev.getInt("port");
                resrec.setValue("IPaddr", IPaddres);
                resrec.setValue("port", port);
                //Log.CORE.info("Device in " + device.toString()+" "+IPaddres);

                if ((controller[device] != null && controller[device].isconnected())) {
                    resrec.setValue("Status", "Подключено и есть обмен");
                } else if (controller[device] == null) {
                    resrec.setValue("Status", "Отсутствовала связь на момент запуска");
                } else {
                    resrec.setValue("Status", "Во время обмена обнаружены ошибки связи");
                }
                res.addRecord(resrec);
                device++;
            }
            return res;
        }
        if (fd.getName().equalsIgnoreCase("history")) {
            String svar = parameters.rec().getString("name");
            Timestamp dfrom = new Timestamp(parameters.rec().getDate("from").getTime());
            Timestamp dto = new Timestamp(parameters.rec().getDate("to").getTime());
//        aThis.lastfunction = svar + " " + dfrom.toString() + ":" + dto.toString();
            DataTable regData = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
            DataTable result = new DataTable(fd.getOutputFormat());
            Integer count = keyMap.get(svar);
            if (count == null) {
                return result;
            }
            ArrayList<SetValue> asv = sqlseek.seekData(dfrom, dto, svar);
            for (SetValue sv : asv) {
                if (sv.getTime() == 0L) {
                    continue;
                }
                DataRecord rec = result.addRecord();
                rec.setValue("series", svar);
                rec.setValue("x", new Timestamp(sv.getTime()));
                rec.setValue("y", sv.getFloatValue());
            }
            return result;
        }

        return super.executeFunction(fd, caller, parameters); //To change body of generated methods, choose Tools | Templates.
    }

    public static final String VF_ADDRESS = "address";
    public static final String VF_NAME = "name";
    public static final String VF_REGISTERS_TYPE = "type";
    public static final String VF_REGISTERS_FORMAT = "format";
    public static final String VF_UNIT_ID = "unitId";
    public static final String VF_DESCRIPTION = "description";
    public static final String VF_SIZE = "size";
    public static final String VF_DEVICE = "deviceId";

    private void makeRegisters() {
        try {
            //Log.CORE.info("makeRegisters in");
            makeParam();
            DeviceContext deviceContext = getDeviceContext();
            DataTable reg = deviceContext.getVariable("registers", getDeviceContext().getCallerController());
            DataTable devs = deviceContext.getVariable("devices", getDeviceContext().getCallerController());
            DataTable perfs = deviceContext.getVariable("perfect", getDeviceContext().getCallerController());
            DataTable SSD = deviceContext.getVariable("SSD", getDeviceContext().getCallerController());

            int device = 0;
            DataTable SSDs = new DataTable(SSD.getFormat());
            DataTable regs = new DataTable(reg.getFormat());
            for (DataRecord recdev : devs) {
                String prefix = recdev.getString("prefix");
                for (DataRecord recperf : perfs) {
                    DataRecord recreg = new DataRecord(regs.getFormat());
                    recreg.setValue(VF_NAME, prefix + recperf.getString(VF_NAME));
                    recreg.setValue(VF_DESCRIPTION, prefix + recperf.getValue(VF_DESCRIPTION));
                    recreg.setValue(VF_REGISTERS_TYPE, recperf.getValue(VF_REGISTERS_TYPE));
                    recreg.setValue(VF_REGISTERS_FORMAT, recperf.getValue(VF_REGISTERS_FORMAT));
                    recreg.setValue(VF_ADDRESS, recperf.getValue(VF_ADDRESS));
                    recreg.setValue(VF_SIZE, recperf.getValue(VF_SIZE));
                    recreg.setValue(VF_UNIT_ID, recperf.getValue(VF_UNIT_ID));
                    recreg.setValue(VF_DEVICE, device);
                    regs.addRecord(recreg);
                    if (recperf.getBoolean("SSD")) {
                        DataRecord recSSD = new DataRecord(SSDs.getFormat());
                        recSSD.setValue(VF_NAME, prefix + recperf.getString(VF_NAME));
                        recSSD.setValue(VF_REGISTERS_TYPE, recperf.getValue(VF_REGISTERS_TYPE));
                        recSSD.setValue(VF_REGISTERS_FORMAT, recperf.getValue(VF_REGISTERS_FORMAT));
                        SSDs.addRecord(recSSD);
                    }
                }
                DataRecord recreg = new DataRecord(regs.getFormat());
                recreg.setValue(VF_NAME, prefix + "Connect");
                recreg.setValue(VF_DESCRIPTION, prefix + "Connect" + " variable");
                recreg.setValue(VF_REGISTERS_TYPE, 1);
                recreg.setValue(VF_REGISTERS_FORMAT, 2);
                recreg.setValue(VF_ADDRESS, -1);
                recreg.setValue(VF_SIZE, 1);
                recreg.setValue(VF_UNIT_ID, 1);
                recreg.setValue(VF_DEVICE, device);
                regs.addRecord(recreg);
                device++;
            }
            //Log.CORE.info("makeRegisters out");
            deviceContext.setVariable("registers", getDeviceContext().getCallerController(), regs);
            deviceContext.setVariable("SSD", getDeviceContext().getCallerController(), SSDs);
        } catch (ContextException ex) {
            Log.CORE.info("Not access to variables regs or devices or perfect!");
        }

        try {
            DataTable regData = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
            registers = DataTableConversion.beansFromTable(regData, Class.forName("com.tibbo.linkserver.plugin.device.modem.MultiModbusRegister"), VFT_REGISTERS, true);
            keyMap = new HashMap<>();
            Integer count = 0;
            for (DataRecord reg : regData) {
                String name = reg.getString("name");
                keyMap.put(name, count++);
            }
        } catch (ClassNotFoundException ex) {
            Log.CORE.info("Not Class MultiModbusRegister!");
        } catch (ContextException ex) {
            Log.CORE.info("Ошибка в makeRegisters " + ex.getMessage());
        }

    }

    private boolean isWritable(int registerType) {
        switch (registerType) {
            case 1: // '\001'
                return false;

            case 0: // '\0'
                return true;

            case 2: // '\002'
                return false;

            case 3: // '\003'
                return true;
        }
        throw new IllegalArgumentException((new StringBuilder()).append("Unknown register type: ").append(registerType).toString());
    }

    private List createDeviceVariableDefinitions(List registers)
            throws ContextException {
        List res = new LinkedList();
        VariableDefinition vd;
        for (Iterator i$ = registers.iterator(); i$.hasNext(); res.add(vd)) {
            MultiModbusRegister register = (MultiModbusRegister) i$.next();
            String name = register.getName();
            int registerType = register.getType();
            int format = register.getFormatForType();
            Character type = (Character) FieldFormat.getClassToTypeMap().get(DataType.getJavaType(format));
            FieldFormat ff = FieldFormat.create(name, type.charValue(), register.getDescription());
            TableFormat rf = new TableFormat(ff);
            rf.setMinRecords(1);
            rf.setMaxRecords(register.getSize());
            rf.setUnresizable(true);
            vd = new VariableDefinition(name, rf, true, isWritable(registerType), register.getDescription(), "remote");
        }
        return res;
    }
    public static final String VF_STEP = "step";
    public static final String VF_LENCOILS = "lenCoils";
    public static final String VF_LENDI = "lenDI";
    public static final String VF_LENIR = "lenIR";
    public static final String VF_LENHR = "lenHR";
    public static final String VF_FLAGMASS = "flagMass";
    public MultiBus PARAM = new MultiBus();
    HashMap<String, Integer> keyMap;
    public final MultiBusTCPController[] controller = new MultiBusTCPController[100];

    @Override
    public void connect()
            throws DeviceException {
        makeParam();
        DataRecord connProps = null;
        try {
            connProps = getDeviceContext().getVariable("connectionProperties", getDeviceContext().getCallerController()).rec();
        } catch (ContextException ex) {
            throw new DeviceException("connectionProperties not found");
        }
        PARAM.flagMass = connProps.getBoolean(VF_FLAGMASS);
        PARAM.lenCoils = connProps.getInt(VF_LENCOILS);
        PARAM.lenDI = connProps.getInt(VF_LENDI);
        PARAM.lenIR = connProps.getInt(VF_LENIR);
        PARAM.lenHR = connProps.getInt(VF_LENHR);
        PARAM.step = connProps.getInt(VF_STEP);
        for (int i = 0; i < controller.length; i++) {
            controller[i] = null;
        }
        DataTable devs = null;
        try {
            devs = super.getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
        } catch (ContextException ex) {
            throw new DeviceException("Devices not found");
        }
        Integer device = 0;
        for (DataRecord recdev : devs) {
            String IPaddres = recdev.getString("IPaddr");
            int port = recdev.getInt("port");
            //Log.CORE.info("Device in " + device.toString());
            try {
                controller[device] = MultiBusTCPController.tcpController(IPaddres, port, PARAM);
                controller[device].connect();
            } catch (Exception ex) {
                //Log.CORE.info("Device Error " + device.toString() + " " + ex.getMessage());
                controller[device] = null;
            }
            //Log.CORE.info("IPDevice out" + IPaddres);
            device++;
        }
        if (thRead == null) {
            thRead = new MultiReconect(this, thrManager);
        }

        super.connect();
        try {
            ParamSQL param = new ParamSQL();
            DataRecord rec;
            rec = getDeviceContext().getVariable("SQLProperties", getDeviceContext().getCallerController()).rec();
            param.JDBCDriver = rec.getString("JDBCDriver");
            param.url = rec.getString("url");
            param.user = rec.getString("user");
            param.password = rec.getString("password");
            param.myDB = rec.getString("table");
            boolean initSQL = rec.getBoolean("initSQL");
            long longSQL = rec.getLong("longSQL");
            long stepSQL = rec.getLong("stepSQL");
            DataTable regData = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
            String Tabledescription = rec.getString("description");
            if (initSQL) {
                Log.CORE.error("Создаем базу " + Tabledescription);
                ArrayList<DescrValue> arraydesc = new ArrayList<>();
                int count = 0;
                for (DataRecord reg : regData) {
                    String name = reg.getString("name");
                    String description = reg.getString("description");
                    int type = 0;
                    switch (reg.getInt("format")) {
                        case 2:
                            if (reg.getInt("type") < 2) {
                                type = 0;
                            } else {
                                type = 1;
                            }
                            break;
                        case 4:
                            type = 3;
                            break;
                        case 8:
                            type = 2;
                            break;
                    }
                    arraydesc.add(new DescrValue(name, description, type));
                }
                new StrongSql(param, arraydesc, Tabledescription);
                Log.CORE.error("Создали базу " + Tabledescription);
                DataTable cp = getDeviceContext().getVariable("SQLProperties", getDeviceContext().getCallerController());
                cp.rec().setValue("initSQL", false);
                getDeviceContext().setVariable("SQLProperties", getDeviceContext().getCallerController(), cp);

            }
            sqldata = new StrongSql(param, stepSQL);
            sqlseek = new StrongSql(param);
            Log.CORE.error("Connect .....");
        } catch (ContextException ex) {
            throw new DeviceException("SQL error!");
        }

    }
    StrongSql sqldata = null;
    StrongSql sqlseek = null;
    private final ThreadManager thrManager = new ThreadManager();
    private MultiReconect thRead = null;

    @Override
    public void disconnect() throws DeviceException {
        for (MultiBusTCPController controller1 : controller) {
            if (controller1 != null) {
                try {
                    controller1.disconnect();
                } catch (Exception ex) {
                    Log.CORE.info("disconnect " + ex.getMessage());
                }
            }
        }
        if ((thrManager != null) & (thRead != null)) {
            thrManager.interruptThread(thRead);
            thRead = null;
        }
        super.disconnect();
        sqldata.disconnect();
        sqlseek.disconnect();
    }

    private void ensureRegisters()
            throws ContextException {
        try {
            if (registers != null) {
                return;
            }
            DataTable regData = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
            registers = DataTableConversion.beansFromTable(regData, Class.forName("com.tibbo.linkserver.plugin.device.modem.MultiModbusRegister"), VFT_REGISTERS, true);
            keyMap = new HashMap<>();
            Integer count = 0;
            for (DataRecord reg : regData) {
                String name = reg.getString("name");
                keyMap.put(name, count++);
            }
        } catch (ClassNotFoundException ex) {
            Log.CORE.info("Not Class MultiModbusRegister!");
        }

    }

    @Override
    public DataTable readVariableValue(VariableDefinition vd, CallerController caller)
            throws ContextException, DeviceException, DisconnectionException {

        ensureRegisters();

        MultiModbusRegister reg = getRegister(vd.getName());
        if (controller[reg.getDeviceId()] == null) {
            DataTable res = new DataTable(vd.getFormat());
            res.addRecord(0);
            return res;
        }
        try {
            if (reg.getAddress() < 0) {
                DataTable res = new DataTable(vd.getFormat());
                res.addRecord(controller[reg.getDeviceId()].isconnected());
                return res;
            }
            //Log.CORE.info("Read Value "+reg.toString());
            return controller[reg.getDeviceId()].readValue(vd.getFormat(), reg);
        } catch (Exception ex) {

            throw new DeviceException((new StringBuilder()).append("Failed to get current value of '").append(reg.getName()).append("' (").append(reg.getDescription()).append(") register: ").append(ex.getMessage()).toString(), ex);
        }
    }

    @Override
    public void writeVariableValue(VariableDefinition vd, CallerController caller, DataTable value, DataTable deviceValue)
            throws ContextException, DeviceException, DisconnectionException {

        ensureRegisters();
        MultiModbusRegister reg = getRegister(vd.getName());
        if (controller[reg.getDeviceId()] == null) {
            //Log.CORE.info("null dev "+reg.toString());
            return;
        }
        //Log.CORE.info("write var "+reg.toString());

        try {
            //Log.CORE.info("Write Value "+reg.toString());
            controller[reg.getDeviceId()].writeValue(reg.getUnitId(), value, reg);
        } catch (Exception ex) {
            throw new DeviceException((new StringBuilder()).append("Failed to set value of '").append(reg.getName()).append("' (").append(reg.getDescription()).append(") register: ").append(ex.getMessage()).toString(), ex);
        }
    }

    private void makeParam() {
        try {
            DeviceContext deviceContext = getDeviceContext();
            DataTable perfs;
            perfs = deviceContext.getVariable("perfect", getDeviceContext().getCallerController());
            int lenCoils = 0;
            int lenDI = 0;
            int lenHR = 0;
            int lenIR = 0;
            int type, format, address, size;
            for (DataRecord recperf : perfs) {
                type = recperf.getInt(VF_REGISTERS_TYPE);
                format = recperf.getInt(VF_REGISTERS_FORMAT);
                address = recperf.getInt(VF_ADDRESS);
                size = recperf.getInt(VF_SIZE);

                if (format >= 4 && format <= 9) {
                    size *= 2;
                }
                if (format >= 11 && format <= 15) {
                    size *= 4;
                }
                if (format == 17) {
                    size *= 2;
                }
                int right = address + size;
                switch (type) {
                    case 0:
                        if (right > lenCoils) {
                            lenCoils = right;
                        }
                        break;
                    case 1:
                        if (right > lenDI) {
                            lenDI = right;
                        }
                        break;
                    case 2:
                        if (right > lenIR) {
                            lenIR = right;
                        }
                        break;
                    case 3:
                        if (right > lenHR) {
                            lenHR = right;
                        }
                        break;
                }

            }
            boolean flagMass = getDeviceContext().getVariable("connectionProperties", getDeviceContext().getCallerController()).rec().getBoolean(VF_FLAGMASS);
            if (flagMass) {
                flagMass = (lenIR < 100) && (lenHR < 100);
            }
            getDeviceContext().setVariableField("connectionProperties", VF_FLAGMASS, flagMass, getDeviceContext().getCallerController());
            getDeviceContext().setVariableField("connectionProperties", VF_LENCOILS, lenCoils, getDeviceContext().getCallerController());
            getDeviceContext().setVariableField("connectionProperties", VF_LENDI, lenDI, getDeviceContext().getCallerController());
            getDeviceContext().setVariableField("connectionProperties", VF_LENIR, lenIR, getDeviceContext().getCallerController());
            getDeviceContext().setVariableField("connectionProperties", VF_LENHR, lenHR, getDeviceContext().getCallerController());
        } catch (ContextException ex) {
            Log.CORE.info("connectionProperties or registers not found" + ex.getMessage());
        }

    }

    private MultiModbusRegister getRegister(String name)
            throws ContextException {
        ensureRegisters();
        for (Iterator idx = registers.iterator(); idx.hasNext();) {
            MultiModbusRegister register = (MultiModbusRegister) idx.next();
            if (register.getName().equals(name)) {
                return register;
            }
        }
        return null;
    }

    private long lastTime;

    @Override
    public void finishSynchronization() throws DeviceException, DisconnectionException {
        try {
            DataTable tregs = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());

            SetData sd = new SetData(new Timestamp(System.currentTimeMillis()));
            for (DataRecord recregs : tregs) {
                String vname = recregs.getString("name");
                DataTable tvar = getDeviceContext().getVariable(vname, getDeviceContext().getCallerController());
                Object obj = tvar.getRecord(0).getValue(0);
                sd.AddValue(new SetValue(vname, obj));
            }
            if (!sd.datas.isEmpty()) {
                sqldata.addValues(sd);
            }
        } catch (ContextException ex) {
            Log.CORE.error("ContextException " + ex.getMessage());
            throw new DeviceException(ex);
        }
        super.finishSynchronization(); //To change body of generated methods, choose Tools | Templates.
    }

    private static Map registerTypeSelectionValues() {
        Map types = new LinkedHashMap();
        types.put(0, "Дискретный выход (Coil)");
        types.put(1, "Дискретный вход (Discrete Input)");
        types.put(2, "Входной регистр (Input Register)");
        types.put(3, "Выходной регистр (Holding Register)");
        return types;
    }

    private static Map registerFormatSelectionValues() {
        Map reg = new LinkedHashMap();
        reg.put(2, "2-байтный Int Unsigned");
        reg.put(3, "2-байтный Int Signed");
        reg.put(4, "4-байтный Int Unsigned");
        reg.put(5, "4-байтный Int Signed");
        reg.put(6, "4-байтный Int Unsigned Swapped");
        reg.put(7, "4-байтный Int Signed Swapped");
        reg.put(8, "4-байтный Float");
        reg.put(9, "4-байтный Float Swapped");
        reg.put(11, "8-байтный Int Signed");
        reg.put(13, "8-байтный IntSignedSwapped");
        reg.put(14, "8-байтный Float");
        reg.put(15, "8-байтный FloatSwapped");
        reg.put(16, "2-байтный Byte Bcd");
        reg.put(17, "4-байтный Bcd");
        reg.put(18, "Символьный");
        reg.put(19, "Строковый");
        return reg;
    }

    private static final TableFormat VFT_CONNECTION_PROPERTIES;
    private static final TableFormat VFT_REGISTERS;
    private static final TableFormat VFT_PERFECT;
    private static final TableFormat VFT_DEVICES;
    private static final TableFormat VFT_SSD;
    private static final TableFormat VFT_SQL;
    private List registers;

    static {
        VFT_CONNECTION_PROPERTIES = new TableFormat(1, 1);

        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create((new StringBuilder()).append("<type><I><D=").append("MultibusVersion").append("><S=<").append("modbusTcp").append("=").append(0).append(">>").toString()));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create((new StringBuilder()).append("<step><I><A=1000><D=").append("Период опроса устройств").append(">").toString()));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create((new StringBuilder()).append("<flagMass><B><A=false><D=").append("Производить массовую запись").append(">").toString()));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create((new StringBuilder()).append("<lenCoils><I><D=").append("Размерность Coils").append(">").toString()));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create((new StringBuilder()).append("<lenDI><I><D=").append("Размерность Digital Input").append(">").toString()));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create((new StringBuilder()).append("<lenIR><I><D=").append("Размерность Input Registers").append(">").toString()));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create((new StringBuilder()).append("<lenHR><I><D=").append("Размерность Holding Registers").append(">").toString()));

        VFT_REGISTERS = new TableFormat(true);
        FieldFormat ff = FieldFormat.create((new StringBuilder()).append("<name><S><D=").append("Имя").append(">").toString());
        ff.getValidators().add(ValidatorHelper.NAME_LENGTH_VALIDATOR);
        ff.getValidators().add(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
        VFT_REGISTERS.addField(ff);
        ff = FieldFormat.create((new StringBuilder()).append("<description><S><D=").append("Описание").append(">").toString());
        ff.getValidators().add(new LimitsValidator(1, 200));
        ff.getValidators().add(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
        VFT_REGISTERS.addField(ff);
        ff = FieldFormat.create((new StringBuilder()).append("<type><I><D=").append("Тип").append(">").toString());
        ff.setSelectionValues(registerTypeSelectionValues());
        VFT_REGISTERS.addField(ff);
        ff = FieldFormat.create((new StringBuilder()).append("<format><I><D=").append("Формат").append(">").toString());
        ff.setSelectionValues(registerFormatSelectionValues());
        VFT_REGISTERS.addField(ff);
        VFT_REGISTERS.addField(FieldFormat.create((new StringBuilder()).append("<address><I><D=").append("Адрес регистра (десятичный)").append(">").toString()));
        VFT_REGISTERS.addField(FieldFormat.create((new StringBuilder()).append("<size><I><A=1><D=").append("Размер").append("><V=<L=1 255>>").toString()));
        VFT_REGISTERS.addField(FieldFormat.create((new StringBuilder()).append("<unitId><I><A=1><D=").append("unitId").append(">").toString()));
        VFT_REGISTERS.addField(FieldFormat.create((new StringBuilder()).append("<deviceId><I><A=1><D=").append("Номер устройства").append(">").toString()));
        String ref = "format#enabled";
        String exp = "{type} == 2 || {type} == 3";
        VFT_REGISTERS.addBinding(ref, exp);

        VFT_PERFECT = new TableFormat(true);
        ff = FieldFormat.create((new StringBuilder()).append("<name><S><D=").append("Имя").append(">").toString());
        ff.getValidators().add(ValidatorHelper.NAME_LENGTH_VALIDATOR);
        ff.getValidators().add(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
        VFT_PERFECT.addField(ff);
        ff = FieldFormat.create((new StringBuilder()).append("<description><S><D=").append("Описание").append(">").toString());
        ff.getValidators().add(new LimitsValidator(Integer.valueOf(1), Integer.valueOf(200)));
        ff.getValidators().add(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
        VFT_PERFECT.addField(ff);
        ff = FieldFormat.create((new StringBuilder()).append("<type><I><D=").append("Тип").append(">").toString());
        ff.setSelectionValues(registerTypeSelectionValues());
        VFT_PERFECT.addField(ff);
        ff = FieldFormat.create((new StringBuilder()).append("<format><I><D=").append("Формат").append(">").toString());
        ff.setSelectionValues(registerFormatSelectionValues());
        VFT_PERFECT.addField(ff);
        VFT_PERFECT.addField(FieldFormat.create((new StringBuilder()).append("<address><I><D=").append("Адрес регистра (десятичный)").append(">").toString()));
        VFT_PERFECT.addField(FieldFormat.create((new StringBuilder()).append("<size><I><A=1><D=").append("Размер").append("><V=<L=1 255>>").toString()));
        VFT_PERFECT.addField(FieldFormat.create((new StringBuilder()).append("<unitId><I><A=1><D=").append("unitId").append(">").toString()));
        VFT_PERFECT.addField(FieldFormat.create((new StringBuilder()).append("<period><L><A=-1><D=").append("Время опроса сигнала").append(">").toString()));
        VFT_PERFECT.addField(FieldFormat.create((new StringBuilder()).append("<SSD><B><A=false><D=").append("Передавать на станционный уровень").append(">").toString()));
        ref = "format#enabled";
        exp = "{type} == 2 || {type} == 3";
        VFT_PERFECT.addBinding(ref, exp);

        VFT_DEVICES = new TableFormat(true);
        VFT_DEVICES.addField(FieldFormat.create((new StringBuilder()).append("<prefix><S><D=").append("Префикс к имени ").append(">").toString()));
        VFT_DEVICES.addField(FieldFormat.create((new StringBuilder()).append("<IPaddr><S><D=").append("IP address устройства").append(">").toString()));
        VFT_DEVICES.addField(FieldFormat.create((new StringBuilder()).append("<port><I><A=502><D=").append("Номер порта").append(">").toString()));

        VFT_SSD = new TableFormat(true);
        VFT_SSD.addField(FieldFormat.create((new StringBuilder()).append("<name><S><D=").append("Имя перемнной ").append(">").toString()));
        VFT_SSD.addField(FieldFormat.create((new StringBuilder()).append("<type><I><D=").append("Тип").append(">").toString()));
        VFT_SSD.addField(FieldFormat.create((new StringBuilder()).append("<format><I><D=").append("Формат").append(">").toString()));

        VFT_SQL = new TableFormat(1, 1);
        VFT_SQL.addField(FieldFormat.create("<url><S><A=jdbc:postgresql://localhost:5432/cyclebuff><D=Url базы данных дампов>"));
        VFT_SQL.addField(FieldFormat.create("<JDBCDriver><S><A=org.postgresql.Driver><D=Драйвер базы данных>"));
        VFT_SQL.addField(FieldFormat.create("<table><S><A=buffer><D=Таблица дампа>"));
        VFT_SQL.addField(FieldFormat.create("<description><S><A=Переменные с устройства><D=Описание таблицы хранения>"));
        VFT_SQL.addField(FieldFormat.create("<user><S><D=Пользователь>"));
        VFT_SQL.addField(FieldFormat.create("<password><S><D=Пароль>"));
        VFT_SQL.addField(FieldFormat.create("<stepSQL><L><A=5000><D=Интервал сохранения переменных в БД >"));
        VFT_SQL.addField(FieldFormat.create("<initSQL><B><A=true><D=Создавать БД при первом запуске>"));
        VFT_SQL.addField(FieldFormat.create("<longSQL><L><A=5000000><D=Размер кольцевой таблицы БД>"));
    }

    class MultiReconect extends AggreGateThread {

        private ModemDeviceDriver fd;
        ThreadManager threadManager = null;

        public MultiReconect(ModemDeviceDriver fd, ThreadManager threadManager) {
            super(threadManager);
            this.threadManager = threadManager;
            threadManager.addThread(this);
            this.fd = fd;
            start();
        }

        @Override
        public void run() {
            do {
                //Log.CORE.info("поток!");
                DataTable devs = null;
                try {
                    devs = fd.getDeviceContext().getVariable("devices", fd.getDeviceContext().getCallerController());
                } catch (ContextException ex) {
                    Log.CORE.info("not devices");
                }
                Integer device = 0;
                for (DataRecord recdev : devs) {
                    String IPaddres = recdev.getString("IPaddr");
                    int port = recdev.getInt("port");
                    //Log.CORE.info("Device in " + device.toString()+" "+IPaddres);
                    try {
                        if (!(fd.controller[device] != null && fd.controller[device].isconnected())) {
                            Log.CORE.info("Перезапускаем " + IPaddres.toString() + ":" + Integer.toString(port));

                            if (fd.controller[device] == null) {
                                fd.controller[device] = MultiBusTCPController.tcpController(IPaddres, port, fd.PARAM);
                            }

                            fd.controller[device].disconnect();
                            AggreGateThread.sleep(1000);
                            fd.controller[device].connect();
                        }
                    } catch (Exception ex) {
                        //Log.CORE.info("Device Error " + device.toString() + " " + ex.getMessage());
                        fd.controller[device] = null;
                    }
                    device++;
                }
                try {
                    AggreGateThread.sleep(10000);
                } catch (InterruptedException ex) {
                    //Log.CORE.info("stop driver ");
                    return;
                }
            } while (!isInterrupted());
        }
    }
}
