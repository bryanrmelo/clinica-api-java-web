package br.com.clinica.dto;

import br.com.clinica.service.erro.ValidacaoException;

import java.time.LocalDate;

public record AlterarPaciente(
        String nome,
        String email,
        String telefone,
        LocalDate nascimento
) {
    public  AlterarPaciente {
        if (nome != null && nome.isBlank()) {
            throw new ValidacaoException("nome não pode ser vazio");
        }
        if (nascimento != null && nascimento.isAfter(java.time.LocalDate.now())) {
            throw new ValidacaoException("nascimento não pode ser no futuro");
        }
    }
}
