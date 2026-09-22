package com.keyloop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI schedulerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Unified Service Scheduler API")
                .description("Resource-constrained appointment booking for dealership service departments.")
                .version("v1")
                .contact(new Contact().name("Keyloop Scheduler"))
                .license(new License().name("Apache-2.0")));
    }
}
