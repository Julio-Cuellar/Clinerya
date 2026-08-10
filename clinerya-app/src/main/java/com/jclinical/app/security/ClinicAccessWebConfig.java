package com.jclinical.app.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ClinicAccessWebConfig implements WebMvcConfigurer {

    private final ClinicAccessInterceptor clinicAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clinicAccessInterceptor)
                .addPathPatterns("/api/v1/clinics/**")
                .excludePathPatterns("/api/v1/clinics/*/history-templates/**");
    }
}
