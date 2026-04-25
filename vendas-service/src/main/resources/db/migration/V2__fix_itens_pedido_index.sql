DROP INDEX IF EXISTS idx_itens_pedido_id;

CREATE INDEX IF NOT EXISTS idx_itens_pedido_id ON itens (pedido_id);
