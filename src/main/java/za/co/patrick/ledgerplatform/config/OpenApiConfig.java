package za.co.patrick.ledgerplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI ledgerPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event-Driven Ledger Platform API")
                        .description("REST API bootstrap for an event-driven ledger and journal posting platform.")
                        .version("v1")
                        .contact(new Contact().name("Patrick")));
    }
}
