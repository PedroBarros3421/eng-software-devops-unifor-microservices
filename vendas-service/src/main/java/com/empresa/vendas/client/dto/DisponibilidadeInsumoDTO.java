package com.empresa.vendas.client.dto;

public record DisponibilidadeInsumoDTO(
        Long insumoId,
        boolean disponivel,
        Integer quantidadeSolicitada,
        Integer quantidadeEstoque
) {
}
