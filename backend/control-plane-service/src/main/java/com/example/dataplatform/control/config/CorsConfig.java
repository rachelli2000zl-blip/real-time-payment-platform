package com.example.dataplatform.control.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final ControlPlaneProperties properties;

    public CorsConfig(ControlPlaneProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = Arrays.stream(properties.getSecurity().getAllowedOrigins().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.length == 0 ? new String[]{"*"} : allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
