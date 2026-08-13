package com.example.wms.admin;

import com.example.wms.admin.config.GatewayInternalTokenProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan("com.example.wms.admin.model.mapper")
@SpringBootApplication(scanBasePackages = "com.example.wms")
@EnableConfigurationProperties(GatewayInternalTokenProperties.class)
public class WmsAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(WmsAdminApplication.class, args);
    }

}
