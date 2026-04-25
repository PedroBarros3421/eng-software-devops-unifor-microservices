package com.empresa.contratos.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Contratos API",
                description = "API para gerenciamento e validação de contratos comerciais.",
                version = "v1",
                contact = @Contact(name = "Equipe de Arquitetura")
        )
)
public interface SwaggerCommon {
}
