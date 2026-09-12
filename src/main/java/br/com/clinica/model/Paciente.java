package br.com.clinica.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Paciente(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDate nascimento,
        OffsetDateTime criadoEm
) {}
