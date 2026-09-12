package br.com.clinica.model;

import java.time.OffsetDateTime;

public record Dentista(Long id, String nome, String cro, OffsetDateTime criadoEm) {}
