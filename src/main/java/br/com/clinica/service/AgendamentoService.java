package br.com.clinica.service;

import br.com.clinica.dto.NovoAgendamento;
import br.com.clinica.model.Agendamento;
import br.com.clinica.model.Dentista;
import br.com.clinica.model.Paciente;
import br.com.clinica.model.projection.AgendamentoInserido;
import br.com.clinica.model.projection.AgendamentoResumo;
import br.com.clinica.repository.AgendamentoDAO;
import br.com.clinica.repository.DentistaDAO;
import br.com.clinica.repository.PacienteDAO;
import br.com.clinica.service.erro.ConflitoException;
import br.com.clinica.service.erro.NaoEncontradoException;
import br.com.clinica.service.erro.RepositorioException;
import br.com.clinica.service.erro.ValidacaoException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AgendamentoService {

    /** exclusion_violation do Postgres: a constraint agendamentos_sem_conflito da V2. */
    private static final String EXCLUSION_VIOLATION = "23P01";

    /** foreign_key_violation: pacienteId ou dentistaId apontando para linha inexistente. */
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private final AgendamentoDAO dao;
    private final PacienteDAO pacienteDao;
    private final DentistaDAO dentistaDAO;
    private final DataSource dataSource;

    public AgendamentoService(AgendamentoDAO dao, PacienteDAO pacienteDao, DentistaDAO dentistaDAO, DataSource dataSource) {
        this.dao = dao;
        this.pacienteDao = pacienteDao;
        this.dentistaDAO = dentistaDAO;
        this.dataSource = dataSource;
    }

    public List<AgendamentoResumo> listar(int pagina, int tamanho) {
        int limite = Math.clamp(tamanho, 1, 100);
        int offset = Math.max(pagina, 0) * limite;

        try(Connection conn = dataSource.getConnection();) {
            return dao.listar(conn, limite, offset);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar agendamentos", e);
        }
    }

    public Agendamento buscarPorId(Long id) {
        try(Connection conn = dataSource.getConnection();) {
            return dao.buscarPorId(conn, id)
                    .orElseThrow(() -> new NaoEncontradoException(
                            "Agendamento com ID: " + id + " nao encontrado"));
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar agendamento com ID:" + id, e);
        }
    }

    public Agendamento cadastrar(NovoAgendamento req) {
        validar(req);

        try(Connection conn = dataSource.getConnection();) {
            conn.setAutoCommit(false);

            try {
                // valida se existe o paciente e o dentista
                Paciente paciente = pacienteDao.buscarPorId(conn, req.pacienteId()).orElseThrow(() -> new ValidacaoException("pacienteId " + req.pacienteId() + " nao existe"));
                Dentista dentista = dentistaDAO.buscarPorId(conn, req.dentistaId()).orElseThrow(() -> new ValidacaoException("dentistaId " + req.dentistaId() + " nao existe"));

                AgendamentoInserido inserido = dao.cadastrar(conn, req);
                Agendamento novoAgendamento = new Agendamento(
                        inserido.id(),
                        paciente,
                        dentista,
                        inserido.inicio(),
                        inserido.fim(),
                        inserido.status(),
                        inserido.observacao(),
                        inserido.criadoEm()
                );

                conn.commit();
                return novoAgendamento;
            } catch(Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            if (EXCLUSION_VIOLATION.equals(e.getSQLState())) {
                throw new ConflitoException(
                        "Ja existe um agendamento nesse horario para este dentista");
            }
            if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
                throw new ValidacaoException(mensagemDeFk(e, req));
            }
            throw new RepositorioException("Falha ao cadastrar agendamento", e);
        }
    }

    private String mensagemDeFk(SQLException e, NovoAgendamento req) {
        String detalhe = String.valueOf(e.getMessage());

        if (detalhe.contains("paciente_id")) {
            return "pacienteId " + req.pacienteId() + " nao existe";
        }
        if (detalhe.contains("dentista_id")) {
            return "dentistaId " + req.dentistaId() + " nao existe";
        }
        return "pacienteId ou dentistaId nao existe";
    }

    // validacao
    private void validar(NovoAgendamento req) {
        if (req == null) {
            throw new ValidacaoException("Corpo da requisição ausente");
        }
        if (req.pacienteId() == null) {
            throw new ValidacaoException("pacienteId é obrigatório");
        }
        if (req.dentistaId() == null) {
            throw new ValidacaoException("dentistaId é obrigatório");
        }
        if (req.inicio() == null) {
            throw new ValidacaoException("inicio é obrigatório");
        }
        if (req.fim() == null) {
            throw new ValidacaoException("fim é obrigatório");
        }

        if(req.inicio().isAfter(req.fim()) || req.inicio().isEqual(req.fim())) {
            throw new ValidacaoException("inicio não pode ser depois de fim");
        }

    }
}
