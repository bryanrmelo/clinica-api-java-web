package br.com.clinica.dto;

import br.com.clinica.model.Paciente;

import java.time.LocalDate;

/**
 * O que a API DEVOLVE. Aqui voce escolhe o que expor.
 * Exemplo: o CPF sai mascarado, porque nao ha motivo para trafegar inteiro.
 */
public record PacienteResponse(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDate nascimento
) {
    /** Metodo de fabrica: converte o objeto do banco no objeto da resposta. */
    public static PacienteResponse de(Paciente p) {
        return new PacienteResponse(
                p.id(), p.nome(), mascarar(p.cpf()), p.email(), p.telefone(), p.nascimento());
    }

    private static String mascarar(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return "***." + cpf.substring(3, 6) + ".***-**";
    }
}
