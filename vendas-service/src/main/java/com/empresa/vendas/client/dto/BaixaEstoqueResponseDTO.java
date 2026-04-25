package com.empresa.vendas.client.dto;

public record BaixaEstoqueResponseDTO(
        Long insumoId,
        Integer quantidadeBaixada,
        Integer quantidadeEstoqueAtual
) {
}
