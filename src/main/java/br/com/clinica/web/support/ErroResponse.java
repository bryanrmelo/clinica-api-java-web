package br.com.clinica.web.support;

import java.time.OffsetDateTime;

/**
 * Formato unico de erro da API. Ter UM formato so faz muita diferenca para
 * quem consome: o cliente escreve um tratamento de erro e ele serve para tudo.
 */
public record ErroResponse(int status, String mensagem, OffsetDateTime timestamp) {

    public static ErroResponse de(int status, String mensagem) {
        return new ErroResponse(status, mensagem, OffsetDateTime.now());
    }
}
