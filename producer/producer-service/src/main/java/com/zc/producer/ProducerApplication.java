package com.zc.producer;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import com.zc.dal.config.MyBatisConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.zc"})
@EnableDiscoveryClient
//@ImportAutoConfiguration(MyBatisConfig.class)
@EnableDubbo(scanBasePackages = "com.zc.producer.service")
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

}
