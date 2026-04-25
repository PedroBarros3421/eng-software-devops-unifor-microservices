package com.empresa.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Gestão Comercial API Gateway",
                description = "Ponto central de documentação OpenAPI dos microsserviços do sistema.",
                version = "v1",
                contact = @Contact(name = "Equipe de Arquitetura")
        )
)
public class SwaggerGatewayConfig {
}
