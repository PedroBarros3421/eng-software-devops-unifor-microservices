package com.empresa.vendas.dtos.input;

import java.math.BigDecimal;

public record ItemInputDTO(
        Long insumoId,
        String nomeInsumo,
        Integer quantidade,
        BigDecimal precoUnitario

) {
}
