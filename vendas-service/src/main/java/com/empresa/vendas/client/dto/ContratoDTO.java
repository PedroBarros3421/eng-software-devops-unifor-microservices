package com.empresa.vendas.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ContratoDTO {

    private Long id;
    private String numero;
    private String nomeContratante;
    private BigDecimal valorTotal;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String status;
    private String termos;
}
