package com.empresa.gestao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gestaoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestão Empresarial API")
                        .description("API REST do sistema de gestão de estoque, clientes, vendas e relatórios.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes("sessionCookie",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("GESTAO_SESSION")))
                .addSecurityItem(new SecurityRequirement().addList("sessionCookie"));
    }
}
