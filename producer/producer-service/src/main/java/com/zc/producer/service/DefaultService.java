package com.zc.producer.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.zc.api.service.producer.DemoService;

@Service(group = "defaultService")
public class DefaultService implements DemoService {
    @Override
    public String sayName(String name) {
        return "hi~," + name;
    }
}
