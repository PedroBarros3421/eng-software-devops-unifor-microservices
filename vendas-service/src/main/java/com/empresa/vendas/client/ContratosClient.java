package com.empresa.vendas.client;

import com.empresa.vendas.client.dto.ContratoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign para comunicação com o contratos-service.
 * O nome "contratos-service" é resolvido via Eureka (Service Discovery).
 * A resiliência (Circuit Breaker, Retry) é aplicada na camada de serviço via anotações Resilience4j.
 */
@FeignClient(name = "contratos-service")
public interface ContratosClient {

    @GetMapping("/api/contratos/{id}")
    ContratoDTO buscarContratoPorId(@PathVariable("id") Long id);

    @GetMapping("/api/contratos/numero/{numero}")
    ContratoDTO buscarContratoPorNumero(@PathVariable("numero") String numero);
}
