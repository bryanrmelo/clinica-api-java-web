package br.com.clinica.service.erro;

/** Choca com algo que ja existe (CPF repetido, horario ocupado) -> HTTP 409. */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
