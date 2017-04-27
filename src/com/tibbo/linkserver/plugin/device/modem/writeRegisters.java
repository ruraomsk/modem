/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tibbo.linkserver.plugin.device.modem;

import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;

/**
 *
 * @author Юрий
 */
public class writeRegisters
{

    public boolean type = false;   // false- Coils true Holding
    public int ref;
    public Register register;

    public writeRegisters(int ref, Register register)
    {
        this.type = true;   // false- Coils true Holding
        this.ref = ref;
        this.register = register;
        
    }

    public writeRegisters( int ref, boolean state)
    {
        this.type = false;   // false- Coils true Holding
        this.ref = ref;
        this.register = new SimpleRegister(( short)(state ? 1 : 0));
    }
}
