package com.zc.api.clients.producer;

import com.zc.api.model.InfoRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

/**
 * Created by coderzc on 2019-02-20
 */

@FeignClient("PRODUCER-SERVICE")
public interface ProducerServerClient {
    @RequestMapping(
            value = {"/queryInfoById"},
            method = {RequestMethod.POST}
    )
    Map<String, Object> queryInfoById(@RequestBody InfoRequest request);

}
