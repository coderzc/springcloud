package com.zc.dal.plugin.encryption.config;


public interface EncryptionConfig {

    String ENCODE_SWITCH = "encryption.switch";

    String ENCODE_MAPPER_CONFIG = "encryption.mapper.config";

    Boolean getEncryptionAllSwitch();

    String getEncodeMapperConfig();
}
