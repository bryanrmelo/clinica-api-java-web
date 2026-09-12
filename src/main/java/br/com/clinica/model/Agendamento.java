package br.com.clinica.model;

import br.com.clinica.model.enums.AgendamentoStatus;

import java.time.OffsetDateTime;

public record Agendamento(Long id, Paciente paciente, Dentista dentista, OffsetDateTime inicio, OffsetDateTime fim, AgendamentoStatus status, String observacao, OffsetDateTime criadoEm) { }
