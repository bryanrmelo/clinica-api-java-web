package br.com.clinica.service;

import br.com.clinica.dto.AtualizarPaciente;
import br.com.clinica.dto.NovoPaciente;
import br.com.clinica.model.Paciente;
import br.com.clinica.repository.PacienteDAO;
import br.com.clinica.service.erro.ConflitoException;
import br.com.clinica.service.erro.NaoEncontradoException;
import br.com.clinica.service.erro.RepositorioException;
import br.com.clinica.service.erro.ValidacaoException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class PacienteService {

    private final PacienteDAO dao;
    private final DataSource dataSource;

    public PacienteService(PacienteDAO dao, DataSource dataSource) {
        this.dao = dao;
        this.dataSource = dataSource;
    }

    public List<Paciente> listar(int pagina, int tamanho) {
        int limite = Math.clamp(tamanho, 1, 100);
        int offset = Math.max(pagina, 0) * limite;

        try (Connection conn = dataSource.getConnection()) {
            return dao.listar(conn, limite, offset);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar pacientes", e);
        }
    }

    public Paciente buscar(long id) {
        try (Connection conn = dataSource.getConnection()) {
            return dao.buscarPorId(conn, id)
                      .orElseThrow(() -> new NaoEncontradoException(
                              "Paciente " + id + " nao encontrado"));
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar paciente " + id, e);
        }
    }

    public Paciente cadastrar(NovoPaciente cmd) {
        validar(cmd);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (dao.existeCpf(conn, cmd.cpf())) {
                    throw new ConflitoException("Já existe paciente com este CPF");
                }

                Paciente salvo = dao.inserir(conn, cmd);

                conn.commit();
                return salvo;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RepositorioException("Falha ao cadastrar paciente", e);
        }
    }

    public Paciente atualizar(Long id, AtualizarPaciente req) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                Paciente salvo = dao.atualizar(conn, id, req).orElseThrow(() -> new NaoEncontradoException("Paciente " + id + " nao encontrado"));
                conn.commit();
                return salvo;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RepositorioException("Falha ao atualizar o paciente #" + id, e);
        }
    }

    // validação
    private void validar(NovoPaciente cmd) {
        if (cmd == null) {
            throw new ValidacaoException("Corpo da requisição ausente");
        }
        if (cmd.nome() == null || cmd.nome().isBlank()) {
            throw new ValidacaoException("nome é obrigatório");
        }
        if (cmd.cpf() == null || !cmd.cpf().matches("\\d{11}")) {
            throw new ValidacaoException("cpf deve ter 11 dígitos, sem pontuação");
        }
        if (cmd.nascimento() == null) {
            throw new ValidacaoException("nascimento é obrigatório");
        }
        if (cmd.nascimento().isAfter(java.time.LocalDate.now())) {
            throw new ValidacaoException("nascimento não pode ser no futuro");
        }
    }
}
