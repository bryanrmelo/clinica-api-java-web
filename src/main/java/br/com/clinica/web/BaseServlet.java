package br.com.clinica.web;

import br.com.clinica.bootstrap.AppContext;
import br.com.clinica.service.erro.ConflitoException;
import br.com.clinica.service.erro.NaoEncontradoException;
import br.com.clinica.service.erro.ValidacaoException;
import br.com.clinica.web.support.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Classe base dos servlets. Existe para nao repetir duas coisas em cada um:
 * pegar as dependencias e traduzir excecao em status HTTP.
 *
 * "abstract": nao pode ser instanciada sozinha, so serve para ser herdada.
 */
public abstract class BaseServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseServlet.class);

    // "protected": enxergado por esta classe e por quem herdar dela.
    protected AppContext app;

    /**
     * O Tomcat chama init() uma vez, depois de criar o servlet e antes da
     * primeira requisicao.
     *
     * Por que nao no construtor? Porque quem da "new" no servlet e o Tomcat,
     * usando o construtor SEM argumentos -- voce nao tem como passar nada ali.
     */
    @Override
    public void init() throws ServletException {
        // getServletContext() vem do HttpServlet, que ja herdamos.
        // Devolve o mesmo mapa que o listener preencheu no startup.
        app = AppContext.de(getServletContext());
    }

    /**
     * service() e o metodo que o Tomcat chama a CADA requisicao; a versao do
     * HttpServlet olha o verbo HTTP e redireciona para doGet/doPost/etc.
     *
     * Interceptando aqui e chamando super, ganhamos um try/catch que cobre
     * todos os verbos de uma vez. E o papel do @ControllerAdvice do Spring.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            super.service(req, resp);
        } catch (Exception e) {
            tratar(e, resp);
        }
    }

    private void tratar(Exception e, HttpServletResponse resp) throws IOException {
        // Se a resposta ja foi enviada, nao da para trocar o status.
        // Sem esta checagem voce ganharia um IllegalStateException confuso
        // por cima do erro original.
        if (resp.isCommitted()) {
            log.error("Erro depois da resposta ja enviada", e);
            return;
        }

        // "instanceof" com variavel (Java 16+): testa e ja declara a variavel
        // do tipo certo, sem cast.
        if (e instanceof ValidacaoException v) {
            Json.erro(resp, 400, v.getMessage());

        } else if (e instanceof NaoEncontradoException n) {
            Json.erro(resp, 404, n.getMessage());

        } else if (e instanceof ConflitoException c) {
            Json.erro(resp, 409, c.getMessage());

        } else if (e instanceof NumberFormatException) {
            // Veio /api/pacientes/abc -- id nao numerico.
            Json.erro(resp, 400, "Identificador invalido");

        } else {
            // Qualquer outra coisa e bug ou falha de infra.
            // O log leva o stack trace completo; o cliente recebe so uma
            // mensagem generica. Vazar detalhe interno em resposta de erro
            // e uma falha de seguranca comum.
            log.error("Erro nao tratado", e);
            Json.erro(resp, 500, "Erro interno");
        }
    }

    /** Le um parametro de query string numerico, com valor padrao. */
    protected int inteiro(HttpServletRequest req, String nome, int padrao) {
        String valor = req.getParameter(nome);
        if (valor == null || valor.isBlank()) return padrao;
        return Integer.parseInt(valor);   // NumberFormatException vira 400 la em cima
    }
}
