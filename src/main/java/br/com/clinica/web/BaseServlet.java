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

public abstract class BaseServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseServlet.class);

    protected AppContext app;

    @Override
    public void init() throws ServletException {
        app = AppContext.de(getServletContext());
    }

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
        if (resp.isCommitted()) {
            log.error("Erro depois da resposta ja enviada", e);
            return;
        }

        switch (e) {
            case ValidacaoException v -> Json.erro(resp, 400, v.getMessage());
            case NaoEncontradoException n -> Json.erro(resp, 404, n.getMessage());
            case ConflitoException c -> Json.erro(resp, 409, c.getMessage());
            case NumberFormatException numberFormatException -> Json.erro(resp, 400, "Identificador invalido");
            case null, default -> {
                log.error("Erro nao tratado", e);
                Json.erro(resp, 500, "Erro interno");
            }
        }
    }

    protected int inteiro(HttpServletRequest req, String nome, int padrao) {
        String valor = req.getParameter(nome);
        if (valor == null || valor.isBlank()) return padrao;
        return Integer.parseInt(valor);   // NumberFormatException vira 400 la em cima
    }
}
