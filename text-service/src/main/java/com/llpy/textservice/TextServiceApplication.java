package com.llpy.textservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
@EnableTransactionManagement
@ComponentScan(basePackages = {"com.llpy"})
public class TextServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TextServiceApplication.class, args);
    }

}
