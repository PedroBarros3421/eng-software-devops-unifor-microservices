package com.empresa.compras.controller.dto;

public record BaixaEstoqueResponseDTO(
        Long insumoId,
        Integer quantidadeBaixada,
        Integer quantidadeEstoqueAtual
) {
}
