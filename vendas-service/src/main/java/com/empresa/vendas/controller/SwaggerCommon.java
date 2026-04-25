package com.empresa.vendas.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        servers = {
                @Server(url = "http://localhost:8083", description = "Vendas local")
        },
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "Vendas API",
                description = "API para gerenciamento de vendas e pedidos"
        ))
public interface SwaggerCommon {
}
