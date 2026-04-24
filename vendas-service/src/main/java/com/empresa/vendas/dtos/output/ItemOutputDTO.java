package com.empresa.vendas.dtos.output;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ItemOutputDTO(
        UUID id,
        Long insumoId,
        String nomeInsumo,
        Integer quantidade,
        BigDecimal precoUnitario,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao

) {
}
