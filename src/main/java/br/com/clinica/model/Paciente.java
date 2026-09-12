package br.com.clinica.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Espelha uma linha da tabela pacientes.
 *
 * "record" (Java 16+) gera construtor, getters, equals, hashCode e toString.
 * Os campos sao imutaveis -- exatamente o que queremos: um objeto que circula
 * entre threads e nao pode ser alterado nunca da problema de concorrencia.
 *
 * Repare que o getter chama-se nome() e nao getNome(): e assim que record funciona.
 */
public record Paciente(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDate nascimento,
        OffsetDateTime criadoEm
) {}
