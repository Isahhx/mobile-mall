package com.macro.mall.test;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableDiscoveryClient
public class TestApplication {


    @Value("${server.port}")
    private int serverPort;

    @Value("${spring.application.name}")
    private String applicationName;

    @PostConstruct
    public void registerInstance() throws NacosException {
        System.out.println(applicationName);
        System.out.println( "127.0.0.1");
        System.out.println( serverPort);

    }


    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

}
