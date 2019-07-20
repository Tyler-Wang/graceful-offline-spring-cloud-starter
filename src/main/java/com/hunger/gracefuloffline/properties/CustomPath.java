package com.hunger.gracefuloffline.properties;

public class CustomPath {
    private String serverContextPath;
    
    private String managementContextPath;
    
    private Integer managementPort;

    public String getServerContextPath() {
        return serverContextPath;
    }

    public void setServerContextPath(String serverContextPath) {
        this.serverContextPath = serverContextPath;
    }

    public String getManagementContextPath() {
        return managementContextPath;
    }

    public void setManagementContextPath(String managementContextPath) {
        this.managementContextPath = managementContextPath;
    }

    public Integer getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(Integer managementPort) {
        this.managementPort = managementPort;
    }
}
