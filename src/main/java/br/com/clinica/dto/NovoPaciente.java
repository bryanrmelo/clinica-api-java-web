package br.com.clinica.dto;

import java.time.LocalDate;

/**
 * O que o cliente MANDA no POST. Repare que nao tem id nem criadoEm:
 * quem decide esses dois e o banco, nao quem chama a API.
 *
 * Por que nao reusar a classe Paciente? Porque entrada e saida mudam por
 * motivos diferentes. Se amanha a tabela ganhar uma coluna interna, o
 * Paciente muda e o contrato publico da API continua igual.
 */
public record NovoPaciente(
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDate nascimento
) {}
