package com.empresa.compras.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Compras API",
                description = "API para gerenciamento de insumos, estoque e pedidos de compra.",
                version = "v1",
                contact = @Contact(name = "Equipe de Arquitetura")
        )
)
public interface SwaggerCommon {
}
