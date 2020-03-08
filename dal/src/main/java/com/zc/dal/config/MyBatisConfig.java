package com.zc.dal.config;

import com.zc.dal.plugin.encryption.interceptor.EncodeDecodeFieldInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by coderzc on 2019-07-04
 * <p>
 * 主要为了开启dao层接口扫描，因为别的项目开发时找不到@MapperScan这个注解会报错
 */

@Configuration
@MapperScan(basePackages = {"com.zc.dal.dao"})
public class MyBatisConfig {
    public MyBatisConfig() {
        System.out.println("------------------- MyBatisConfig配置Bean启动初始化 -------------------");
    }

    @Bean
    public EncodeDecodeFieldInterceptor getEncodeDecodeFieldInterceptor() {
        return new EncodeDecodeFieldInterceptor();
    }
}
