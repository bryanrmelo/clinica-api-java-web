package br.com.clinica.dto;

import br.com.clinica.model.Dentista;

import java.time.OffsetDateTime;

public record DentistaResponse(
        Long id,
        String nome,
        String cro,
        OffsetDateTime criadoEm
) {
    public static DentistaResponse de(Dentista d) {
        return new DentistaResponse(
                d.id(), d.nome(), d.cro(), d.criadoEm());
    }
}
