package com.edu.api.pop_cube_wallet.shared.infrastructure;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI popCubeWalletOpenAPI(
            @Value("${spring.application.name}") String appName,
            @Value("${app.version:0.0.1-SNAPSHOT}") String appVersion,
            @Value("${server.port:8080}") String serverPort) {

        return new OpenAPI()
                .info(new Info()
                        .title("Pop Cube Wallet API")
                        .version(appVersion)
                        .description("""
                                Digital Wallet REST API built as a DDD Modular Monolith \
                                following Hexagonal Architecture and CQRS patterns. \
                                Supports account management, P2P transfers, wallet operations, \
                                transaction statements, and immutable audit logging. \
                                Authentication is simplified via a User-ID header (MVP scope).""")
                        .contact(new Contact()
                                .name("Pop Cube Wallet Team")
                                .email("wallet-team@popcube.edu")
                                .url("https://github.com/pop-cube/pop_cube_wallet"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Architecture & Developer Guides")
                        .url("https://github.com/pop-cube/pop_cube_wallet#readme"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")));
    }
}
