package br.com.clinica.dto;

import br.com.clinica.model.Agendamento;
import br.com.clinica.model.enums.AgendamentoStatus;

import java.time.OffsetDateTime;

public record AgendamentoResponse(
        Long id,
        PacienteResponse paciente,
        DentistaResponse dentista,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        AgendamentoStatus status,
        String observacao,
        OffsetDateTime criadoEm
) {
    public static AgendamentoResponse de(Agendamento a) {
        return new AgendamentoResponse(
                a.id(), PacienteResponse.de(a.paciente()), DentistaResponse.de(a.dentista()), a.inicio(), a.fim(), a.status(), a.observacao(), a.criadoEm());
    }
}
