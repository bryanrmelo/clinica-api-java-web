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
 *   GET  /api/pacientes           -> lista (aceita ?pagina=0&tamanho=20)
 *   GET  /api/pacientes/{id}      -> um paciente
 *   POST /api/pacientes           -> cadastra
 *
 * O "/*" no fim do mapeamento significa "e tudo que vier depois".
 * Sem ele, /api/pacientes/42 daria 404: o Tomcat so casaria o caminho exato.
 * Com ele, TODAS as URLs abaixo de /api/pacientes caem neste mesmo servlet --
 * e separar os casos e trabalho seu.
 *
 * E exatamente isto que o @GetMapping("/{id}") do Spring resolve sozinho.
 */
@WebServlet("/api/pacientes/*")
public class PacienteServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // getPathInfo() devolve o pedaco da URL DEPOIS do mapeamento:
        //   /api/pacientes      -> null
        //   /api/pacientes/     -> "/"
        //   /api/pacientes/42   -> "/42"
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            listar(req, resp);
        } else {
            // substring(1) tira a barra da frente. Se nao for numero, o
            // NumberFormatException sobe e o BaseServlet devolve 400.
            long id = Long.parseLong(path.substring(1));
            buscar(id, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // POST so faz sentido na colecao, nao num item especifico.
        // Sem esta checagem, POST /api/pacientes/42 criaria um paciente novo
        // silenciosamente -- confuso para quem consome.
        String path = req.getPathInfo();
        if (path != null && !path.equals("/")) {
            Json.erro(resp, 405, "POST nao e permitido neste caminho");
            return;
        }

        // JSON do corpo -> record. Uma linha, gracas ao Json/Jackson.
        NovoPaciente cmd = Json.ler(req, NovoPaciente.class);

        // O servlet NAO valida nem conhece SQL: so traduz HTTP <-> objeto.
        // Toda a regra esta no service. Isso e o que mantem o servlet magro
        // e permite testar a regra sem subir servidor nenhum.
        Paciente salvo = app.pacientes().cadastrar(cmd);

        // 201 Created + header Location apontando para o recurso criado.
        // E o que o padrao REST manda -- e o que quase toda API esquece.
        resp.setHeader("Location", req.getRequestURI() + "/" + salvo.id());
        Json.escrever(resp, 201, PacienteResponse.de(salvo));
    }

    // ---------- metodos privados, so para organizar ----------

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int pagina  = inteiro(req, "pagina", 0);
        int tamanho = inteiro(req, "tamanho", 20);

        List<PacienteResponse> corpo = app.pacientes()
                .listar(pagina, tamanho)
                .stream()
                .map(PacienteResponse::de)   // converte cada Paciente em response
                .toList();

        Json.escrever(resp, 200, corpo);
    }

    private void buscar(long id, HttpServletResponse resp) throws IOException {
        // Se nao existir, o service lanca NaoEncontradoException e o
        // BaseServlet transforma em 404. Nao precisa de if aqui.
        Paciente p = app.pacientes().buscar(id);
        Json.escrever(resp, 200, PacienteResponse.de(p));
    }
}
