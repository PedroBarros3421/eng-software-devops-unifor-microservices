package com.empresa.contratos.service;

import com.empresa.contratos.domain.Contrato;
import com.empresa.contratos.dto.ContratoStatusDTO;
import com.empresa.contratos.repository.ContratoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratosServiceTest {

    @Mock
    private ContratoRepository contratoRepository;

    @InjectMocks
    private ContratosService contratosService;

    @Test
    void deveRetornarInvalidoQuandoContratoAindaNaoIniciou() {
        Contrato contrato = novoContrato(Contrato.StatusContrato.ATIVO,
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(10));

        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        ContratoStatusDTO response = contratosService.validarContrato(1L);

        assertFalse(response.isValido());
        assertEquals("ATIVO", response.getStatus());
        assertEquals("Contrato ainda não iniciou. Vigência a partir de " + contrato.getDataInicio(), response.getMotivo());
    }

    @Test
    void deveEncerrarContratoQuandoEstiverVencido() {
        Contrato contrato = novoContrato(Contrato.StatusContrato.ATIVO,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1));

        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContratoStatusDTO response = contratosService.validarContrato(1L);

        assertFalse(response.isValido());
        assertEquals("ENCERRADO", response.getStatus());
        assertEquals(Contrato.StatusContrato.ENCERRADO, contrato.getStatus());
    }

    @Test
    void deveRetornarValidoQuandoContratoEstiverAtivoEDentroDaVigencia() {
        Contrato contrato = novoContrato(Contrato.StatusContrato.ATIVO,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(10));

        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        ContratoStatusDTO response = contratosService.validarContrato(1L);

        assertEquals(1L, response.getId());
        assertEquals("ATIVO", response.getStatus());
        assertNull(response.getMotivo());
    }

    private Contrato novoContrato(Contrato.StatusContrato status, LocalDate dataInicio, LocalDate dataFim) {
        Contrato contrato = new Contrato();
        contrato.setId(1L);
        contrato.setNumero("CTR-001");
        contrato.setNomeContratante("Cliente");
        contrato.setValorTotal(BigDecimal.TEN);
        contrato.setDataInicio(dataInicio);
        contrato.setDataFim(dataFim);
        contrato.setStatus(status);
        contrato.setTermos("Termos");
        return contrato;
    }
}
