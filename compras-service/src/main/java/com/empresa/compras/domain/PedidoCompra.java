package com.empresa.compras.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.empresa.compras.domain.enuns.StatusPedido;

@Entity
@Table(name = "pedidos_compra")
@Data
@NoArgsConstructor
public class PedidoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_fornecedor", nullable = false)
    private String nomeFornecedor;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_total", nullable = false)
    private BigDecimal precoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;

}
