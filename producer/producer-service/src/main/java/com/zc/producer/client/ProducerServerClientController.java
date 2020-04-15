package com.zc.producer.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zc.api.model.InfoRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by coderzc on 2019-05-29
 */
@RestController
@RefreshScope
public class ProducerServerClientController {

    @Value("${server.port}")
    private String port;

    @Value("${myconfig.desc:not find config}")
    private String configTest;

    @RequestMapping(
            value = {"/queryInfoById"},
            method = {RequestMethod.POST}
    )
    public Map<String, Object> queryInfoById(InfoRequest request) {
        Map<String, Object> resultMap = new HashMap<>();

        String paramId = request.getId();
//        System.out.println("request.getIds："+request.getIds());
//        System.out.println("request.getParamInfo："+request.getParamInfo());

        String dataJson = "[{\"id\":1001,\"name\":\"张三\",\"age\":24},{\"id\":1002,\"name\":\"李四\",\"age\":25},{\"id\":1003,\"name\":\"王五\",\"age\":22},{\"id\":1004,\"name\":\"小明\",\"age\":19}]";
        JSONArray jsonArray = JSON.parseArray(dataJson);
        if (request.getId() == null) {// 返回所有数据
            resultMap.put("serviceMsg", request.getId() + " by producer " + port);
            resultMap.put("data", jsonArray);

        } else {//返回指定数据
//            jsonArray
//                    = jsonArray.stream()
//                    .filter(x -> paramId.equals(((JSONObject) x).getString("id")))
//                    .collect(Collectors.toCollection(JSONArray::new));

            List<Object> jsonList
                    = jsonArray.stream()
                    .filter(x -> paramId.equals(((JSONObject) x).getString("id")))
                    .collect(Collectors.toList());
            resultMap.put("serviceMsg", "all by producer " + port);
            resultMap.put("data", jsonList);
        }

//        String serializeJson = JSON.toJSONString(resultMap);
//        System.out.println(serializeJson);
        return resultMap;
    }


    @GetMapping("/get_config")
    public String getConfig() {
        return configTest;
    }
}
