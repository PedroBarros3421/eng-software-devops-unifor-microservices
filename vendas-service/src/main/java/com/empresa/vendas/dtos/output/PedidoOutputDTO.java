package com.empresa.vendas.dtos.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PedidoOutputDTO(
        UUID id,
        String nomeCliente,
        BigDecimal valorTotal,
        LocalDate dataPedido,
        Long contratoId,
        List<ItemOutputDTO> itens,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
}
