package br.com.clinica.model.projection;

import br.com.clinica.model.enums.AgendamentoStatus;

import java.time.OffsetDateTime;

public record AgendamentoResumo(
        Long id,
        Long pacienteId,
        String pacienteNome,
        Long dentistaId,
        String dentistaNome,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        AgendamentoStatus status,
        String observacao,
        OffsetDateTime criadoEm
) {}
