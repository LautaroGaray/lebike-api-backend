package com.example.scaffold.config;

import com.example.scaffold.security.BearerTokenInterceptor;
import com.example.scaffold.security.SessionLifecycleListener;
import com.example.scaffold.security.TokenService;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final BearerTokenInterceptor bearerTokenInterceptor;

    public WebConfig(BearerTokenInterceptor bearerTokenInterceptor) {
        this.bearerTokenInterceptor = bearerTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bearerTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/modules/loadByUser");
    }

    @Bean
    public ServletListenerRegistrationBean<SessionLifecycleListener> sessionLifecycleListener(TokenService tokenService) {
        ServletListenerRegistrationBean<SessionLifecycleListener> bean = new ServletListenerRegistrationBean<>();
        bean.setListener(new SessionLifecycleListener(tokenService));
        return bean;
    }
}
