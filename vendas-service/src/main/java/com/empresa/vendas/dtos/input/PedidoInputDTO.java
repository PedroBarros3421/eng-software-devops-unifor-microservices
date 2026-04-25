package com.empresa.vendas.dtos.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PedidoInputDTO(
        String nomeCliente,
        LocalDate dataPedido,
        Long contratoId,
        List<ItemInputDTO> itens
) {
}
