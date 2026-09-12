package br.com.clinica.dto;

import java.time.LocalDate;

public record NovoPaciente(
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDate nascimento
) {}
