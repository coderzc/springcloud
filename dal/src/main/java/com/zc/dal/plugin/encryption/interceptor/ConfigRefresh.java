package com.zc.dal.plugin.encryption.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.zc.dal.plugin.encryption.config.EncryptionConfig;
import com.zc.dal.plugin.encryption.config.MapperConfigModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@EnableScheduling
public class ConfigRefresh {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRefresh.class);

    @Autowired
    private EncryptionConfig encryptionConfig;

    @Scheduled(
            cron = "*/30 * * * * ?"
    )
    public void refreshConfig() {
        try {
            logger.info("start refresh encryption config");
            String encodeColumnConfig = encryptionConfig.getEncodeMapperConfig();
            if (StringUtils.isEmpty(encodeColumnConfig)) {
                logger.warn("encodeColumnConfig is empty");
            } else {
                EncodeDecodeColumnInterceptor.mapperConfigurationMap = JSON.parseObject(encodeColumnConfig, new TypeReference<HashMap<String, MapperConfigModel>>() {
                });
            }
        } catch (Exception e) {
            logger.error("load config error:", e);
        }
    }
}
