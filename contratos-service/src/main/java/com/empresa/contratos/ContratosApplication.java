package com.empresa.contratos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(servers = { @Server(url = "/") })
@SpringBootApplication
public class ContratosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContratosApplication.class, args);
    }
}
