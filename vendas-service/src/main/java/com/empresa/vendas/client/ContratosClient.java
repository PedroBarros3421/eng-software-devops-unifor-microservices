package com.empresa.vendas.client;

import com.empresa.vendas.client.dto.ContratoDTO;
import com.empresa.vendas.client.dto.ContratoStatusResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "contratos-service", path = "/api/contratos")
public interface ContratosClient {

    @GetMapping("/{id}")
    ContratoDTO buscarContratoPorId(@PathVariable("id") Long id);

    @GetMapping("/numero/{numero}")
    ContratoDTO buscarContratoPorNumero(@PathVariable("numero") String numero);

    @GetMapping("/{id}/validacao")
    ContratoStatusResponseDTO validarContrato(@PathVariable("id") Long id);

}
