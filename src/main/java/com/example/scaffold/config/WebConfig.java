package com.example.scaffold.config;

import com.example.scaffold.security.BearerTokenInterceptor;
import com.example.scaffold.security.SessionLifecycleListener;
import com.example.scaffold.security.TokenService;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final BearerTokenInterceptor bearerTokenInterceptor;

    public WebConfig(BearerTokenInterceptor bearerTokenInterceptor) {
        this.bearerTokenInterceptor = bearerTokenInterceptor;
    }

    @Bean
    @Profile("local")
    public CorsFilter localCorsFilter() {
        CorsConfiguration configuration = new CorsConfiguration() {
            @Override
            public String checkOrigin(String requestOrigin) {
                return isLocalOrigin(requestOrigin) ? requestOrigin : null;
            }
        };
        configuration.setAllowCredentials(true);
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("PATCH");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");
        configuration.addAllowedHeader(HttpHeaders.AUTHORIZATION);
        configuration.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
        configuration.addAllowedHeader(BearerTokenInterceptor.HEADER_MODULE_MAIN_ID);
        configuration.addAllowedHeader(BearerTokenInterceptor.HEADER_ACTION);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }

    private static boolean isLocalOrigin(String origin) {
        return origin != null && origin.matches("https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?");
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
