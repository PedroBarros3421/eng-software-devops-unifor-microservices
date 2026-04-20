package com.empresa.contratos.dto;

public class ContratoStatusDTO {
    private Long id;
    private boolean valido;
    private String status;
    private String motivo;

    // Construtor
    public ContratoStatusDTO(Long id, boolean valido, String status, String motivo) {
        this.id = id;
        this.valido = valido;
        this.status = status;
        this.motivo = motivo;
    }

    // Getters e Setters (Essenciais para o Jackson converter para JSON)
    public Long getId() {
        return id;
    }

    public boolean isValido() {
        return valido;
    }

    public String getStatus() {
        return status;
    }

    public String getMotivo() {
        return motivo;
    }
}
