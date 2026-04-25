package com.empresa.compras.controller.dto;

public record DisponibilidadeInsumoDTO(
        Long insumoId,
        boolean disponivel,
        Integer quantidadeSolicitada,
        Integer quantidadeEstoque
) {
}
