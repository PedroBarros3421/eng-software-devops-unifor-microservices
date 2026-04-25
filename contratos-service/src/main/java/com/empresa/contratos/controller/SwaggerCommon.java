package com.empresa.contratos.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        servers = {
                @Server(url = "http://localhost:8082", description = "Contratos local")
        },
        info = @Info(
                title = "Contratos API",
                description = "API para gerenciamento e validação de contratos comerciais.",
                version = "v1",
                contact = @Contact(name = "Equipe de Arquitetura")
        )
)
public interface SwaggerCommon {
}
