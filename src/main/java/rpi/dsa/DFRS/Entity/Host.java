package rpi.dsa.DFRS.Entity;

import com.alibaba.fastjson.annotation.JSONField;

public class Host {
    @JSONField(name = "tcp_start_port")
    private Integer tcpStartPort;

    @JSONField(name = "tcp_end_port")
    private Integer tcpEndPort;

    @JSONField(name = "udp_start_port")
    private Integer udpStartPort;

    @JSONField(name = "udp_end_port")
    private Integer udpEndPort;

    @JSONField(name = "ip_address")
    private String ipAddr;

    public Integer getTcpStartPort() {
        return tcpStartPort;
    }

    public void setTcpStartPort(Integer tcpStartPort) {
        this.tcpStartPort = tcpStartPort;
    }

    public Integer getTcpEndPort() {
        return tcpEndPort;
    }

    public void setTcpEndPort(Integer tcpEndPort) {
        this.tcpEndPort = tcpEndPort;
    }

    public Integer getUdpStartPort() {
        return udpStartPort;
    }

    public void setUdpStartPort(Integer udpStartPort) {
        this.udpStartPort = udpStartPort;
    }

    public Integer getUdpEndPort() {
        return udpEndPort;
    }

    public void setUdpEndPort(Integer udpEndPort) {
        this.udpEndPort = udpEndPort;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    @Override
    public String toString() {
        return "udpStartPort=" + udpStartPort +
                        ", udpEndPort=" + udpEndPort +
                        ", ipAddr='" + ipAddr + '\'';
    }
}
