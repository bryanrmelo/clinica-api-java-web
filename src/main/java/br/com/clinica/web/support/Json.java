package br.com.clinica.web.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Tudo que envolve JSON fica aqui. Sem isso, cada servlet repetiria as mesmas
 * cinco linhas de setContentType/setStatus/getWriter -- e uma hora alguem
 * esqueceria o charset e os acentos sairiam quebrados.
 *
 * E o equivalente manual do que o Spring faz com @RequestBody / @ResponseBody.
 */
public final class Json {

    /**
     * UM ObjectMapper para a aplicacao inteira.
     *
     * Motivo: criar um ObjectMapper e caro (ele monta e cacheia informacao de
     * reflexao de cada classe). Criar um por requisicao e um erro de
     * performance classico. Ele e thread-safe depois de configurado, entao
     * pode ser static e compartilhado sem medo.
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            // Ensina o Jackson a lidar com LocalDate / OffsetDateTime.
            // Sem este modulo, datas viram um objeto JSON esquisito ou estouram.
            .addModule(new JavaTimeModule())
            // Datas como texto ISO-8601 ("2026-03-15") em vez de numero.
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Campo desconhecido no JSON de entrada nao derruba a requisicao.
            // Deixa a API tolerante a clientes mandando lixo a mais.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private Json() {}

    /** Le o corpo da requisicao e transforma no record que voce pediu. */
    public static <T> T ler(HttpServletRequest req, Class<T> tipo) throws IOException {
        // getInputStream le os bytes crus do corpo HTTP.
        // ATENCAO: so pode ser lido UMA vez. Uma segunda chamada vem vazia,
        // porque o stream ja foi consumido.
        return MAPPER.readValue(req.getInputStream(), tipo);
    }

    /** Serializa o objeto e escreve na resposta com o status informado. */
    public static void escrever(HttpServletResponse resp, int status, Object corpo)
            throws IOException {

        // A ORDEM importa: status e headers tem que ser definidos ANTES de
        // escrever qualquer byte no corpo. Depois que a resposta e "commitada"
        // (o buffer vai para a rede), mudar o status nao tem mais efeito.
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        MAPPER.writeValue(resp.getOutputStream(), corpo);
    }

    /** Atalho para responder um erro no formato padrao. */
    public static void erro(HttpServletResponse resp, int status, String mensagem)
            throws IOException {
        escrever(resp, status, ErroResponse.de(status, mensagem));
    }
}
