package br.com.clinica.repository;

import br.com.clinica.dto.NovoDentista;
import br.com.clinica.model.Dentista;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DentistaDAO {

    private static final String SQL_LISTAR = """
            SELECT id, nome, cro, criado_em
                FROM dentistas
             ORDER BY nome
             LIMIT ? OFFSET ?
            """;

    private static final String SQL_INSERIR = """
            INSERT INTO dentistas (nome, cro)
            VALUES (?, ?)
            RETURNING id, nome, cro, criado_em
            """;

    private static final String SQL_EXISTE_CRO = """
            SELECT 1
            FROM dentistas
            WHERE cro = ?
            """;

    private static final String SQL_POR_ID = """
            SELECT id, nome, cro, criado_em
            FROM dentistas
            WHERE id = ?
            """;

    private static final String SQL_POR_CRO = """
            SELECT id, nome, cro, criado_em
            FROM dentistas
            WHERE cro = ?
            """;

    public List<Dentista> listar(Connection conn, int limite, int offset) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR)) {
            ps.setInt(1, limite);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<Dentista> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    public Optional<Dentista> buscarPorId(Connection conn, Long id) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement(SQL_POR_ID)) {
            ps.setLong(1, id);

            try(ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Dentista> buscarPorCro(Connection conn, String cro) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement(SQL_POR_CRO)) {
            ps.setString(1, cro);

            try(ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public Dentista inserir(Connection conn, NovoDentista cmd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR)) {
            ps.setString(1, cmd.nome());
            ps.setString(2, cmd.cro());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapear(rs);
            }
        }
    }

    public boolean existeCro(Connection conn, String cro) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement(SQL_EXISTE_CRO)) {
            ps.setString(1, cro);
            try(ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Dentista mapear(ResultSet rs) throws SQLException {
        return new Dentista(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("cro"),
                rs.getObject("criado_em", java.time.OffsetDateTime.class)
        );
    }


}
