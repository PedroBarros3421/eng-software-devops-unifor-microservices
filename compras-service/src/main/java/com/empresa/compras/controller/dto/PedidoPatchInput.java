package com.empresa.compras.controller.dto;

import java.time.LocalDate;

import com.empresa.compras.domain.enuns.StatusPedido;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PedidoPatchInput {
    
    private String nomeFornecedor;
    private Integer quantidade;
    private StatusPedido status;
    private LocalDate dataPedido;

}
