package com.test.bootmybatisnginxauth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.controller","com.service","com.po","com.util"})
@MapperScan(basePackages = {"com.mapper"})
public class BootMybatisNginxAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootMybatisNginxAuthApplication.class, args);
    }

}
