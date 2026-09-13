package br.com.clinica.dto;

import br.com.clinica.service.erro.ValidacaoException;

import java.time.LocalDate;

public record AtualizarPaciente(
        String nome,
        String email,
        String telefone,
        LocalDate nascimento
) {
    public  AtualizarPaciente {
        if (nome == null || nome.isBlank()) {
            throw new ValidacaoException("nome é obrigatório");
        }
        if (nascimento == null) {
            throw new ValidacaoException("nascimento é obrigatório");
        }
        if (nascimento.isAfter(java.time.LocalDate.now())) {
            throw new ValidacaoException("nascimento não pode ser no futuro");
        }
    }
}
