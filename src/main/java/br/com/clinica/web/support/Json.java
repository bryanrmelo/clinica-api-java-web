package br.com.clinica.web.support;

import br.com.clinica.service.erro.ValidacaoException;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    /**
     * Le o corpo da requisicao e transforma no record que voce pediu.
     *
     * Erro de JSON e culpa do CLIENTE, entao vira ValidacaoException -> 400.
     * Sem esta traducao, a JsonProcessingException (que estende IOException)
     * escapava ate o default do tratar() e virava 500: o cliente nao entendia
     * o que fez de errado e o seu log enchia de "erro nao tratado" que nao
     * era erro seu. O caso mais comum aqui nao e nem JSON quebrado -- e
     * mandar "2026-03-15T09:00" num campo OffsetDateTime, que exige o fuso.
     */
    public static <T> T ler(HttpServletRequest req, Class<T> tipo) throws IOException {
        // getInputStream le os bytes crus do corpo HTTP.
        // ATENCAO: so pode ser lido UMA vez. Uma segunda chamada vem vazia,
        // porque o stream ja foi consumido -- por isso guardamos num byte[]
        // antes de entregar ao Jackson. De quebra, isso separa "corpo vazio"
        // de "corpo invalido", que dao mensagens bem diferentes.
        byte[] bruto = req.getInputStream().readAllBytes();

        if (bruto.length == 0) {
            throw new ValidacaoException("Corpo da requisicao ausente");
        }

        try {
            T corpo = MAPPER.readValue(bruto, tipo);

            // Corpo literalmente "null" e JSON valido, mas nao serve de comando.
            if (corpo == null) {
                throw new ValidacaoException("Corpo da requisicao ausente");
            }
            return corpo;

        } catch (JsonProcessingException e) {
            // getOriginalMessage() = so o motivo, sem o "at [Source: ...line: 1]"
            // que o Jackson anexa e que nao diz nada para quem chamou a API.
            throw new ValidacaoException("JSON invalido: " + e.getOriginalMessage());
        }
        // IOException "de verdade" (cliente derrubou a conexao no meio do envio)
        // NAO e capturada aqui de proposito: essa sobe e vira 500, corretamente.
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
