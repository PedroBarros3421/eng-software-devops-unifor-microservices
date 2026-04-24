package com.empresa.compras.controller.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class InsumoPatchDTO {

    private String nome;
    private String descricao;
    private String unidadeMedida;
    private BigDecimal precoUnitario;
    private Integer quantidadeEstoque;
}
