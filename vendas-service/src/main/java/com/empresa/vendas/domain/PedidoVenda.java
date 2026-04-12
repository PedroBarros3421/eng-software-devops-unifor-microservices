package com.empresa.vendas.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "pedidos_venda")
@Data
@NoArgsConstructor
public class PedidoVenda {

    @Id
    private String id;

    private String nomeCliente;

    private List<ItemVenda> itens;

    private BigDecimal valorTotal;

    private StatusPedidoVenda status;

    private LocalDate dataPedido;

    // Referência ao contrato vinculado (opcional)
    private Long contratoId;

    public enum StatusPedidoVenda {
        PENDENTE, APROVADO, ENTREGUE, CANCELADO
    }
}
