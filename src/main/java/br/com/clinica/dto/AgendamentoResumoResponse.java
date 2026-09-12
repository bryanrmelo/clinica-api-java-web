package br.com.clinica.dto;

import br.com.clinica.model.enums.AgendamentoStatus;
import br.com.clinica.model.projection.AgendamentoResumo;

import java.time.OffsetDateTime;

public record AgendamentoResumoResponse(
        Long id,
        Long pacienteId,
        String pacienteNome,
        Long dentistaId,
        String dentistaNome,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        AgendamentoStatus status,
        String observacao
) {
    public static AgendamentoResumoResponse de(AgendamentoResumo r) {
        return new AgendamentoResumoResponse(
                r.id(),
                r.pacienteId(),
                r.pacienteNome(),
                r.dentistaId(),
                r.dentistaNome(),
                r.inicio(),
                r.fim(),
                r.status(),
                r.observacao()
        );
    }
}