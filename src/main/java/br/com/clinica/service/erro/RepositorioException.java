package br.com.clinica.service.erro;

/**
 * Falha tecnica de banco (conexao caiu, SQL invalido) -> vira HTTP 500.
 *
 * Existe por um motivo pratico: SQLException e "checked", ou seja, o Java
 * obriga a declarar ou tratar. Como doGet() nao pode declarar "throws
 * SQLException" (a assinatura e fixada pelo HttpServlet), o service embrulha
 * a SQLException nesta RuntimeException, que sobe livre.
 *
 * A causa original vai junto no "cause" -- por isso o log consegue mostrar o
 * stack trace completo do erro de banco.
 */
public class RepositorioException extends RuntimeException {
    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
