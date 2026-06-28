package za.co.patrick.ledgerplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI ledgerPlatformOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("basicAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .info(new Info()
                        .title("Event-Driven Ledger Platform API")
                        .description("REST API bootstrap for an event-driven ledger and journal posting platform.")
                        .version("v1")
                        .contact(new Contact().name("Patrick")));
    }
}
