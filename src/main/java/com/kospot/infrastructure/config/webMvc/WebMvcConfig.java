package com.kospot.infrastructure.config.webMvc;

import com.kospot.infrastructure.security.resolver.CustomAuthenticationPrincipalArgumentResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String[] corsFrontPaths;
    private final CustomAuthenticationPrincipalArgumentResolver customAuthenticationPrincipalArgumentResolver;

    public WebMvcConfig(@Value("${app.cors.front-path}")  String corsFrontPath, CustomAuthenticationPrincipalArgumentResolver customAuthenticationPrincipalArgumentResolver) {
        // "https://kospot.kr,https://www.kospot.kr,https://d3..." 이런 식으로 들어오면 split
        this.corsFrontPaths = Arrays.stream(corsFrontPath.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        this.customAuthenticationPrincipalArgumentResolver = customAuthenticationPrincipalArgumentResolver;
        log.info("CORS origins from env = {}", Arrays.toString(this.corsFrontPaths));

    }

    //resolver
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(customAuthenticationPrincipalArgumentResolver);
    }

    //CORS setting
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedOriginPatterns(corsFrontPaths)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

    }
}