package br.com.clinica.service;

import br.com.clinica.dto.NovoDentista;
import br.com.clinica.model.Dentista;
import br.com.clinica.repository.DentistaDAO;
import br.com.clinica.service.erro.ConflitoException;
import br.com.clinica.service.erro.NaoEncontradoException;
import br.com.clinica.service.erro.RepositorioException;
import br.com.clinica.service.erro.ValidacaoException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

public class DentistaService {

    /** unique_violation do Postgres. Ver o comentario em cadastrar(). */
    private static final String UNIQUE_VIOLATION = "23505";

    private final DentistaDAO dao;
    private final DataSource dataSource;

    public DentistaService(DentistaDAO dao, DataSource dataSource) {
        this.dao = dao;
        this.dataSource = dataSource;
    }

    public List<Dentista> listar(int pagina, int tamanho) {
        int limite = Math.clamp(tamanho, 1, 100);
        int offset = Math.max(pagina, 0) * limite;

        try(Connection conn = dataSource.getConnection();) {
            return dao.listar(conn, limite, offset);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar dentista", e);
        }

    }

    public Dentista buscarPorId(Long id) {
        try(Connection conn = dataSource.getConnection();) {
            return dao.buscarPorId(conn, id)
                    .orElseThrow(() -> new NaoEncontradoException(
                            "Dentista " + id + " nao encontrado"));
        } catch (SQLException e) {
            // Nao encontrado ja virou 404 no orElseThrow acima. Chegar aqui
            // significa banco fora do ar, e o log tem que dizer isso.
            throw new RepositorioException("Falha ao buscar dentista " + id, e);
        }
    }

    public Dentista buscarPorCro(String cro) {
        try(Connection conn = dataSource.getConnection();) {
            return dao.buscarPorCro(conn, cro)
                    .orElseThrow(() -> new NaoEncontradoException(
                            "Dentista " + cro + " nao encontrado"));
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar dentista de CRO " + cro, e);
        }
    }

    public Dentista cadastrar(NovoDentista req) {
        validar(req);

        try(Connection conn = dataSource.getConnection();) {
            conn.setAutoCommit(false);

            try {
                if(dao.existeCro(conn, req.cro())) {
                    throw new ConflitoException("Ja existe um dentista com esse CRO");
                }

                Dentista salvo = dao.inserir(conn, req);

                conn.commit();
                return salvo;
            } catch(Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch(SQLException e) {
            // O existeCro acima NAO e garantia: duas requisicoes simultaneas
            // com o mesmo CRO passam as duas pelo SELECT antes de qualquer
            // INSERT acontecer. Quem segura de verdade e o UNIQUE da tabela
            // (V1__cria_tabelas.sql) -- mesma licao da V2 para agendamentos.
            //
            // Traduzindo o SQLSTATE aqui, o perdedor da corrida recebe o mesmo
            // 409 do caminho normal em vez de um 500 generico.
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new ConflitoException("Ja existe um dentista com esse CRO");
            }
            throw new RepositorioException("Falha ao cadastrar dentista", e);
        }
    }

    // validacao
    private void validar(NovoDentista req) {
        if (req == null) {
            throw new ValidacaoException("Corpo da requisicao ausente");
        }
        if (req.nome() == null || req.nome().isBlank()) {
            throw new ValidacaoException("nome e obrigatorio");
        }
        if (req.cro() == null || req.cro().isBlank()) {
            throw new ValidacaoException("cro e obrigatorio");
        }
    }
}
