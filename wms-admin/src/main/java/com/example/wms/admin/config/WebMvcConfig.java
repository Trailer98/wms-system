package com.example.wms.admin.config;

import com.example.wms.admin.security.GatewayUserContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final GatewayUserContextInterceptor gatewayUserContextInterceptor;

    public WebMvcConfig(GatewayUserContextInterceptor gatewayUserContextInterceptor) {
        this.gatewayUserContextInterceptor = gatewayUserContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewayUserContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login");
    }
}
