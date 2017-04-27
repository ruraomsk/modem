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
public class ModBusDevice {

    private String prefix;
    private String IPaddr;
    private int port;

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return the IPaddr
     */
    public String getIPaddr() {
        return IPaddr;
    }

    /**
     * @param IPaddr the IPaddr to set
     */
    public void setIPaddr(String IPaddr) {
        this.IPaddr = IPaddr;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }
}
