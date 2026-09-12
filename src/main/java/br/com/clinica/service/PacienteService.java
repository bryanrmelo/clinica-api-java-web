package br.com.clinica.service;

import br.com.clinica.dto.NovoPaciente;
import br.com.clinica.model.Paciente;
import br.com.clinica.repository.PacienteDao;
import br.com.clinica.service.erro.ConflitoException;
import br.com.clinica.service.erro.NaoEncontradoException;
import br.com.clinica.service.erro.RepositorioException;
import br.com.clinica.service.erro.ValidacaoException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Camada de regra de negocio. Tres responsabilidades, e so estas tres:
 *   1. validar a entrada
 *   2. orquestrar chamadas ao DAO
 *   3. controlar a TRANSACAO
 *
 * Os dois campos abaixo sao "final" e chegam prontos pelo construtor: a classe
 * nunca da new em nada. E isso que permite trocar o DAO por um falso num teste.
 *
 * IMPORTANTE: nenhum campo guarda dados de requisicao. Um unico objeto destes
 * atende todas as requisicoes simultaneas do Tomcat -- se voce guardar o
 * "paciente atual" num campo, duas requisicoes paralelas vao se atropelar.
 */
public class PacienteService {

    private final PacienteDao dao;
    private final DataSource dataSource;

    public PacienteService(PacienteDao dao, DataSource dataSource) {
        this.dao = dao;
        this.dataSource = dataSource;
    }

    // ---------- LEITURA: nao precisa de transacao explicita ----------

    public List<Paciente> listar(int pagina, int tamanho) {
        // Paginacao com teto: sem isso, um GET sem parametro poderia puxar
        // um milhao de linhas e derrubar a memoria da aplicacao.
        int limite = Math.min(Math.max(tamanho, 1), 100);
        int offset = Math.max(pagina, 0) * limite;

        // getConnection() NAO abre conexao nova: pega uma emprestada do pool.
        // O close() do try-with-resources tambem nao fecha de verdade --
        // devolve ao pool. Por isso e barato e por isso NUNCA se pode esquecer:
        // conexao nao devolvida some do pool para sempre.
        try (Connection conn = dataSource.getConnection()) {
            return dao.listar(conn, limite, offset);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar pacientes", e);
        }
    }

    public Paciente buscar(long id) {
        try (Connection conn = dataSource.getConnection()) {
            return dao.buscarPorId(conn, id)
                      // orElseThrow: se o Optional estiver vazio, lanca.
                      // O servlet transforma isso em HTTP 404.
                      .orElseThrow(() -> new NaoEncontradoException(
                              "Paciente " + id + " nao encontrado"));
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar paciente " + id, e);
        }
    }

    // ---------- ESCRITA: aqui a transacao importa ----------

    public Paciente cadastrar(NovoPaciente cmd) {
        validar(cmd);

        try (Connection conn = dataSource.getConnection()) {

            // Por padrao o JDBC roda em autoCommit: cada comando e sua propria
            // transacao, commitada na hora. Desligando, voce assume o controle:
            // daqui ate o commit, tudo e uma operacao unica e indivisivel.
            conn.setAutoCommit(false);

            try {
                if (dao.existeCpf(conn, cmd.cpf())) {
                    throw new ConflitoException("Ja existe paciente com este CPF");
                }

                Paciente salvo = dao.inserir(conn, cmd);

                // So agora o dado fica visivel para as outras conexoes.
                conn.commit();
                return salvo;

            } catch (Exception e) {
                // Qualquer falha desfaz TUDO que aconteceu depois do
                // setAutoCommit(false). Com uma unica operacao isso parece
                // exagero -- mas e o mesmo esqueleto que o AgendamentoService
                // vai usar, onde varios INSERTs precisam cair juntos.
                conn.rollback();
                throw e;   // relanca: quem trata o status HTTP e o servlet
            } finally {
                // A conexao volta para o pool e vai ser reusada por outra
                // requisicao. Se ficasse com autoCommit desligado, a proxima
                // requisicao herdaria esse estado e gravaria nada sem commit.
                // Bug dificilimo de achar. Por isso o finally.
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RepositorioException("Falha ao cadastrar paciente", e);
        }
    }

    // ---------- validacao ----------

    private void validar(NovoPaciente cmd) {
        if (cmd == null) {
            throw new ValidacaoException("Corpo da requisicao ausente");
        }
        if (cmd.nome() == null || cmd.nome().isBlank()) {
            throw new ValidacaoException("nome e obrigatorio");
        }
        if (cmd.cpf() == null || !cmd.cpf().matches("\\d{11}")) {
            throw new ValidacaoException("cpf deve ter 11 digitos, sem pontuacao");
        }
        if (cmd.nascimento() == null) {
            throw new ValidacaoException("nascimento e obrigatorio");
        }
        if (cmd.nascimento().isAfter(java.time.LocalDate.now())) {
            throw new ValidacaoException("nascimento nao pode ser no futuro");
        }
    }
}
