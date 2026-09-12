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
 *   GET  /api/dentistas            -> lista (aceita ?page=0&limit=20)
 *   GET  /api/dentistas/{id}       -> um dentista pelo id
 *   GET  /api/dentistas/cro/{cro}  -> um dentista pelo CRO
 *   POST /api/dentistas            -> cadastra
 *
 * Por que /cro/{cro} e nao /api/dentistas/{cro} direto?
 * Porque CRO em varios estados e so numero. Se o roteamento decidisse pelo
 * formato ("se der Long.parseLong e id, senao e CRO"), /api/dentistas/12345
 * seria eternamente ambiguo. Com o segmento fixo "cro" na frente, cada rota
 * tem um significado unico e o id continua sendo o identificador canonico.
 */
@WebServlet("/api/dentistas/*")
public class DentistaServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // getPathInfo() devolve o pedaco DEPOIS do mapeamento, ja decodificado:
        //   /api/dentistas             -> null
        //   /api/dentistas/            -> "/"
        //   /api/dentistas/42          -> "/42"
        //   /api/dentistas/cro/SP-1234 -> "/cro/SP-1234"
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            listar(req, resp);
            return;
        }

        // substring(1) tira a barra da frente; split quebra no resto.
        // split descarta os vazios do fim, entao "/42/" tambem vira ["42"].
        String[] partes = path.substring(1).split("/");

        if (partes.length == 1) {
            // Se nao for numero, o NumberFormatException sobe e o
            // BaseServlet devolve 400.
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
        int page = inteiro(req, "page", 0);
        int limit = inteiro(req, "limit", 20);

        List<DentistaResponse> body = app.dentistas().listar(page, limit).stream().map(DentistaResponse::de).toList();

        Json.escrever(resp, 200, body);
    }

    private void buscarPorId(long id, HttpServletResponse resp) throws IOException {
        // Se nao existir, o service lanca NaoEncontradoException e o
        // BaseServlet transforma em 404. Nao precisa de if aqui.
        Dentista d = app.dentistas().buscarPorId(id);
        Json.escrever(resp, 200, DentistaResponse.de(d));
    }

    private void buscarPorCro(String cro, HttpServletResponse resp) throws IOException {
        Dentista d = app.dentistas().buscarPorCro(cro);
        Json.escrever(resp, 200, DentistaResponse.de(d));
    }
}
