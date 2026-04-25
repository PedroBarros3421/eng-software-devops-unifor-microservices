CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS pedidos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome_cliente VARCHAR(255) NOT NULL,
    valor_total NUMERIC(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_pedido DATE NOT NULL,
    contrato_id BIGINT,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS itens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insumo_id BIGINT NOT NULL,
    nome_insumo VARCHAR(255) NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(19, 2) NOT NULL,
    pedido_id UUID NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_itens_pedidos
        FOREIGN KEY (pedido_id)
        REFERENCES pedidos (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_itens_pedido_id ON itens (pedido_id);

