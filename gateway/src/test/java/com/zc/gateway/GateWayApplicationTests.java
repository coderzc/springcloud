package com.zc.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GateWayApplicationTests {

    @Test
    public void contextLoads() {
    }


    public static void main(String[] args) {
        System.out.println(URI.create("forward:/fallback"));
    }
}
