package com.zc.dal.model;

import java.util.Date;

public class UserInfo {
    private Long id;

    private Long userId;

    private String mobile;

    private Integer age;

    private String trueName;

    private Date createTime;

    private Date modifyTime;

    public UserInfo(Long id, Long userId, String mobile, Integer age, String trueName, Date createTime, Date modifyTime) {
        this.id = id;
        this.userId = userId;
        this.mobile = mobile;
        this.age = age;
        this.trueName = trueName;
        this.createTime = createTime;
        this.modifyTime = modifyTime;
    }

    public UserInfo() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile == null ? null : mobile.trim();
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getTrueName() {
        return trueName;
    }

    public void setTrueName(String trueName) {
        this.trueName = trueName == null ? null : trueName.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }
}