package com.projeto.banco.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.projeto.banco.security.AuthInterceptor;
import com.projeto.banco.service.AuthService;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthService authService;

    public WebConfig(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(authService)).addPathPatterns(
                "/api/pedidos", "/api/pedidos/**",
                "/api/auth/me", "/api/auth/logout",
                "/api/meus-cupons",
                "/api/avaliacoes",
                "/api/clientes", "/api/clientes/**");
    }
}
