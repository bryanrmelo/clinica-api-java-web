package br.com.clinica.web;

import br.com.clinica.dto.NovoPaciente;
import br.com.clinica.dto.PacienteResponse;
import br.com.clinica.model.Paciente;
import br.com.clinica.web.support.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Endpoints:
 *   GET /api/pacientes ≥ lista (?pagina=0&limite=20)
 *   GET /api/pacientes/{id} ≥ busca pelo id
 *   POST /api/pacientes ≥ cadastra
 */
@WebServlet("/api/pacientes/*")
public class PacienteServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            listar(req, resp);
        } else {
            long id = Long.parseLong(path.substring(1));
            buscar(id, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();
        if (path != null && !path.equals("/")) {
            Json.erro(resp, 405, "POST nao e permitido neste caminho");
            return;
        }

        NovoPaciente cmd = Json.ler(req, NovoPaciente.class);

        Paciente salvo = app.pacientes().cadastrar(cmd);

        resp.setHeader("Location", req.getRequestURI() + "/" + salvo.id());
        Json.escrever(resp, 201, PacienteResponse.de(salvo));
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int pagina = inteiro(req, "pagina", 0);
        int limite = inteiro(req, "limite", 20);

        List<PacienteResponse> corpo = app.pacientes()
                .listar(pagina, limite)
                .stream()
                .map(PacienteResponse::de)
                .toList();

        Json.escrever(resp, 200, corpo);
    }

    private void buscar(long id, HttpServletResponse resp) throws IOException {
        Paciente p = app.pacientes().buscar(id);
        Json.escrever(resp, 200, PacienteResponse.de(p));
    }
}
