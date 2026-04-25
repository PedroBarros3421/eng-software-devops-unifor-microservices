package com.empresa.vendas.client.dto;

public record ContratoStatusResponseDTO(
         Long id,
         boolean valido,
         String status,
         String motivo
) {
}
