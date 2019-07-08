package com.zc.api.model;

public class ParamInfo {
    private String param;

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    @Override
    public String toString() {
        return "ParamInfo{" +
                "param='" + param + '\'' +
                '}';
    }
}
