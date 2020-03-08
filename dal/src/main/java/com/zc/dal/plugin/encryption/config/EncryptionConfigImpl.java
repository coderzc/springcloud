package com.zc.dal.plugin.encryption.config;

import org.springframework.stereotype.Component;

@Component("encryptionConfig")
public class EncryptionConfigImpl implements EncryptionConfig {


    @Override
    public Boolean getEncryptionAllSwitch() {
        return false;
    }

    @Override
    public String getEncodeMapperConfig() {
        return "";
    }
}
