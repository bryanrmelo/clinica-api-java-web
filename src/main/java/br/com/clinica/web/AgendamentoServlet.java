package br.com.clinica.web;

import br.com.clinica.dto.*;
import br.com.clinica.model.Agendamento;
import br.com.clinica.model.Dentista;
import br.com.clinica.model.projection.AgendamentoResumo;
import br.com.clinica.web.support.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/agendamentos/*")
public class AgendamentoServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            listar(req, resp);
            return;
        }

        String[] partes = path.substring(1).split("/");

        if (partes.length == 1) {
            buscarPorId(Long.parseLong(partes[0]), resp);
        } else {
            Json.erro(resp, 404, "Recurso nao encontrado");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path != null && !path.equals("/")) {
            Json.erro(resp, 405, "POST nao e permitido neste caminho");
            return;
        }
        NovoAgendamento cmd = Json.ler(req, NovoAgendamento.class);
        Agendamento salvo = app.agendamentos().cadastrar(cmd);
        resp.setHeader("Location", req.getRequestURI() + "/" + salvo.id());
        Json.escrever(resp, 201, AgendamentoResponse.de(salvo));
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int page = inteiro(req, "page", 0);
        int limit = inteiro(req, "limit", 20);

        List<AgendamentoResumoResponse> body = app.agendamentos().listar(page, limit).stream().map(AgendamentoResumoResponse::de).toList();

        Json.escrever(resp, 200, body);
    }

    private void buscarPorId(long id, HttpServletResponse resp) throws IOException {
        // Se nao existir, o service lanca NaoEncontradoException e o
        // BaseServlet transforma em 404. Nao precisa de if aqui.
        Agendamento a = app.agendamentos().buscarPorId(id);
        Json.escrever(resp, 200, AgendamentoResponse.de(a));
    }
}
