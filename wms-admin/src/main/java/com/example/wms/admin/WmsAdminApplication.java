package com.example.wms.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.wms.admin.model.mapper")
@SpringBootApplication(scanBasePackages = "com.example.wms")
public class WmsAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(WmsAdminApplication.class, args);
    }

}
