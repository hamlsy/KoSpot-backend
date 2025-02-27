package com.kospot.kospot.global.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String GITHUB_URL = "https://github.com/hamlsy/KoSpot-backend";

    @Bean
    public OpenAPI api(){
        return new OpenAPI().info(swaggerInfo());
    }

    private Info swaggerInfo() {
        License license = new License();
        license.setUrl(GITHUB_URL);
        license.setName("KoSpot");

        return new Info().version("v0.0.1")
                .title("KoSpot Server Api")
                .description("KoSpot Server API Document")
                .license(license);
    }

}
