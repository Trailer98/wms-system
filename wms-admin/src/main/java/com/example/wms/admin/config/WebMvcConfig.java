package com.example.wms.admin.config;

import com.example.wms.admin.security.GatewayUserContextInterceptor;
import com.example.wms.admin.security.LegacyAdminAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final GatewayUserContextInterceptor gatewayUserContextInterceptor;
    private final LegacyAdminAccessInterceptor legacyAdminAccessInterceptor;

    public WebMvcConfig(
            GatewayUserContextInterceptor gatewayUserContextInterceptor,
            LegacyAdminAccessInterceptor legacyAdminAccessInterceptor) {
        this.gatewayUserContextInterceptor = gatewayUserContextInterceptor;
        this.legacyAdminAccessInterceptor = legacyAdminAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewayUserContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login");
        registry.addInterceptor(legacyAdminAccessInterceptor)
                .addPathPatterns(
                        "/users",
                        "/users/**",
                        "/roles",
                        "/roles/**",
                        "/permissions",
                        "/permissions/**");
    }
}
