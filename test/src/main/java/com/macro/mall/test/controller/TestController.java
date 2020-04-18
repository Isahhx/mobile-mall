package com.macro.mall.test.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author hhx
 * Date 2020/4/2 3:48 下午
 **/
@RestController
@RequestMapping("/config")
@RefreshScope
public class TestController {

    @Value("${server.port}")
    private String url;


    @RequestMapping("/test")
    public String test(){
        return url;
    }
}
