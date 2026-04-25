package com.empresa.compras.controller.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PedidoPatchInput {
    
    private String nomeFornecedor;
    private Integer quantidade;
    private LocalDate dataPedido;

}
