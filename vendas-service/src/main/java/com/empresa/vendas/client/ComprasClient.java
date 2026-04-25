package com.empresa.vendas.client;

import com.empresa.vendas.client.dto.BaixaEstoqueInputDTO;
import com.empresa.vendas.client.dto.BaixaEstoqueResponseDTO;
import com.empresa.vendas.client.dto.DisponibilidadeInsumoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "compras-service", path = "/api/compras")
public interface ComprasClient {

    @GetMapping("/insumos/{id}/disponibilidade")
    DisponibilidadeInsumoDTO consultarDisponibilidade(
            @PathVariable("id") Long id,
            @RequestParam("quantidade") Integer quantidade
    );

    @PostMapping("/insumos/{id}/baixa")
    BaixaEstoqueResponseDTO baixarEstoque(
            @PathVariable("id") Long id,
            @RequestBody BaixaEstoqueInputDTO input
    );
}
