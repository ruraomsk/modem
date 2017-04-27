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
public class MultiModbusRegister {

    public MultiModbusRegister() {
        size = 1;
        deviceId=0;
        unitId=1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getFormat() {
        return format;
    }

    public int getFormatForType() {
        if (0 == type || 1 == type) {
            return 1;
        } else {
            return format;
        }
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int device) {
        this.deviceId = device;
    }
    @Override
    public String toString(){
        return getName()+" "+getDescription()+" ["+Integer.toString(getType())+"] "+Integer.toString(getFormat())+" "+Integer.toString(getAddress())+" "+Integer.toString(getSize());
    }
    private String name;
    private String description;
    private int type;
    private int format;
    private int address;
    private int size;
    private int unitId;
    private int deviceId;
}
