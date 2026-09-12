package br.com.clinica.web;

import br.com.clinica.dto.DentistaResponse;
import br.com.clinica.dto.NovoDentista;
import br.com.clinica.model.Dentista;
import br.com.clinica.web.support.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Endpoints:
 *   GET /api/dentistas ≥ lista (?page=0&limit=20)
 *   GET /api/dentistas/{id} ≥ um dentista pelo id
 *   GET /api/dentistas/cro/{cro} ≥ um dentista pelo CRO
 *   POST /api/dentistas ≥ cadastra
 */
@WebServlet("/api/dentistas/*")
public class DentistaServlet extends BaseServlet {

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

        } else if (partes.length == 2 && partes[0].equals("cro")) {
            buscarPorCro(partes[1], resp);

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
        NovoDentista cmd = Json.ler(req, NovoDentista.class);
        Dentista salvo = app.dentistas().cadastrar(cmd);
        resp.setHeader("Location", req.getRequestURI() + "/" + salvo.id());
        Json.escrever(resp, 201, DentistaResponse.de(salvo));
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int pagina = inteiro(req, "pagina", 0);
        int limite = inteiro(req, "limite", 20);

        List<DentistaResponse> body = app.dentistas().listar(pagina, limite).stream().map(DentistaResponse::de).toList();

        Json.escrever(resp, 200, body);
    }

    private void buscarPorId(long id, HttpServletResponse resp) throws IOException {
        Dentista d = app.dentistas().buscarPorId(id);
        Json.escrever(resp, 200, DentistaResponse.de(d));
    }

    private void buscarPorCro(String cro, HttpServletResponse resp) throws IOException {
        Dentista d = app.dentistas().buscarPorCro(cro);
        Json.escrever(resp, 200, DentistaResponse.de(d));
    }
}
