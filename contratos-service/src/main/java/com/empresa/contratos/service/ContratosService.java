package com.empresa.contratos.service;

import com.empresa.contratos.domain.Contrato;
import com.empresa.contratos.dto.ContratoStatusDTO;
import com.empresa.contratos.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ContratosService {

    private final ContratoRepository contratoRepository;

    public List<Contrato> listarTodos() {
        return contratoRepository.findAll();
    }

    public List<Contrato> listarAtivos() {
        return contratoRepository.findByStatus(Contrato.StatusContrato.ATIVO);
    }

    public Contrato buscarPorId(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado: " + id));
    }

    public Contrato buscarPorNumero(String numero) {
        return contratoRepository.findByNumero(numero)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado: " + numero));
    }

    @Transactional
    public Contrato salvar(Contrato contrato) {
        return contratoRepository.save(contrato);
    }

    @Transactional
    public Contrato atualizarStatus(Long id, Contrato.StatusContrato novoStatus) {
        Contrato contrato = buscarPorId(id);
        contrato.setStatus(novoStatus);
        return contratoRepository.save(contrato);
    }

    @Transactional
    public void excluir(Long id) {
        contratoRepository.deleteById(id);
    }

    public ContratoStatusDTO validarContrato(Long id) {

        LocalDate hoje = LocalDate.now();

        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado: " + id));

        if (Contrato.StatusContrato.ATIVO.equals(contrato.getStatus()) 
                && contrato.getDataFim().isBefore(hoje)) {
            atualizarStatus(id, Contrato.StatusContrato.ENCERRADO);

            return new ContratoStatusDTO(id, false, contrato.getStatus().name(),
                    "A vigência do contrato terminou em " + contrato.getDataFim());
        }

        if (!Contrato.StatusContrato.ATIVO.equals(contrato.getStatus())) {
            return new ContratoStatusDTO(id, false, contrato.getStatus().name(), "Contrato não está ativo");
        }

        return new ContratoStatusDTO(id, true, "ATIVO", null);
    }
}
