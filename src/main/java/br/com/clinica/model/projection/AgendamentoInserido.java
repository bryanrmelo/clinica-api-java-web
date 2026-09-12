package br.com.clinica.model.projection;

import br.com.clinica.model.enums.AgendamentoStatus;

import java.time.OffsetDateTime;

public record AgendamentoInserido(
        Long id,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        AgendamentoStatus status,
        String observacao,
        OffsetDateTime criadoEm
) {
}
