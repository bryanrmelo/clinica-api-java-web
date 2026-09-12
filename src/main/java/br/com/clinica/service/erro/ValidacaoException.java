package br.com.clinica.service.erro;

/**
 * Dado que o cliente mandou esta errado -> vira HTTP 400.
 *
 * Estende RuntimeException (e nao Exception) de proposito: assim nao e preciso
 * declarar "throws" em cada metodo do caminho. A excecao sobe sozinha ate o
 * BaseServlet, que decide o status HTTP.
 */
public class ValidacaoException extends RuntimeException {
    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}
