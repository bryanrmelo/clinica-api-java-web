package br.com.clinica.dto;

import java.time.OffsetDateTime;

public record NovoAgendamento(
        Long pacienteId,
        Long dentistaId,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        String observacao
) {
}
