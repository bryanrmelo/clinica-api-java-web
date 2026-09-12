package br.com.clinica.repository;

import br.com.clinica.model.Dentista;
import br.com.clinica.model.Paciente;
import br.com.clinica.model.projection.AgendamentoInserido;
import br.com.clinica.model.projection.AgendamentoResumo;
import br.com.clinica.dto.NovoAgendamento;
import br.com.clinica.model.Agendamento;
import br.com.clinica.model.enums.AgendamentoStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AgendamentoDAO {

    private static final String SQL_LISTAR = """
    SELECT a.id,
       p.id   AS paciente_id,
       p.nome AS paciente_nome,
       d.id   AS dentista_id,
       d.nome AS dentista_nome,
       a.inicio,
       a.fim,
       a.status,
       a.observacao,
       a.criado_em
    FROM agendamentos a
    INNER JOIN pacientes p ON p.id = a.paciente_id
    INNER JOIN dentistas d ON d.id = a.dentista_id
    ORDER BY a.inicio DESC, a.id
    LIMIT ? OFFSET ?
    """;
    private static final String SQL_POR_ID = """
    SELECT a.id,
        a.inicio,
        a.fim,
        a.status,
        a.observacao,
        a.criado_em,
        p.id         AS paciente_id,
        p.nome       AS paciente_nome,
        p.cpf        AS paciente_cpf,
        p.email      AS paciente_email,
        p.telefone   AS paciente_telefone,
        p.nascimento AS paciente_nascimento,
        p.criado_em  AS paciente_criado_em,
        d.id         AS dentista_id,
        d.nome       AS dentista_nome,
        d.cro        AS dentista_cro,
        d.criado_em  AS dentista_criado_em
    FROM agendamentos a
    INNER JOIN pacientes p ON p.id = a.paciente_id
    INNER JOIN dentistas d ON d.id = a.dentista_id
    WHERE a.id = ?
    """;
    private static final String SQL_INSERIR = """
    INSERT INTO agendamentos (paciente_id, dentista_id, inicio, fim, observacao)
    VALUES(?, ?, ?, ?, ?)
    RETURNING id, inicio, fim, status, observacao, criado_em
    """;

    public List<AgendamentoResumo> listar(Connection conn, int limite, int offset) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement(SQL_LISTAR)) {
            ps.setInt(1, limite);
            ps.setInt(2, offset);

            try(ResultSet rs = ps.executeQuery()) {
                List<AgendamentoResumo> resultado = new ArrayList<>();
                while(rs.next()) {
                    resultado.add(mapearResumo(rs));
                }
                return resultado;
            }
        }
    }

    public Optional<Agendamento> buscarPorId(Connection conn, Long id) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement(SQL_POR_ID)) {
            ps.setLong(1, id);

            try(ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public AgendamentoInserido cadastrar(Connection conn, NovoAgendamento cmd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR)) {
            ps.setLong(1, cmd.pacienteId());
            ps.setLong(2, cmd.dentistaId());
            ps.setObject(3, cmd.inicio());
            ps.setObject(4, cmd.fim());
            ps.setString(5, cmd.observacao());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapearInserido(rs);
            }
        }
    }

    /* MAPPING */
    private Agendamento mapear(ResultSet rs) throws SQLException {
        return new Agendamento(
                rs.getLong("id"),
                mapearPaciente(rs),
                mapearDentista(rs),
                rs.getObject("inicio", OffsetDateTime.class),
                rs.getObject("fim", OffsetDateTime.class),
                AgendamentoStatus.valueOf(rs.getString("status")),
                rs.getString("observacao"),
                rs.getObject("criado_em", OffsetDateTime.class)
        );
    }

    private Paciente mapearPaciente(ResultSet rs) throws SQLException {
        return new Paciente(
                rs.getLong("paciente_id"),
                rs.getString("paciente_nome"),
                rs.getString("paciente_cpf"),
                rs.getString("paciente_email"),
                rs.getString("paciente_telefone"),
                rs.getObject("paciente_nascimento", LocalDate.class),
                rs.getObject("paciente_criado_em", OffsetDateTime.class)
        );
    }

    private Dentista mapearDentista(ResultSet rs) throws SQLException {
        return new Dentista(
                rs.getLong("dentista_id"),
                rs.getString("dentista_nome"),
                rs.getString("dentista_cro"),
                rs.getObject("dentista_criado_em", OffsetDateTime.class)
        );
    }

    private AgendamentoResumo mapearResumo(ResultSet rs) throws SQLException {
        return new AgendamentoResumo(
                rs.getLong("id"),
                rs.getLong("paciente_id"),
                rs.getString("paciente_nome"),
                rs.getLong("dentista_id"),
                rs.getString("dentista_nome"),
                rs.getObject("inicio", OffsetDateTime.class),
                rs.getObject("fim", OffsetDateTime.class),
                AgendamentoStatus.valueOf(rs.getString("status")),
                rs.getString("observacao"),
                rs.getObject("criado_em", OffsetDateTime.class)
        );
    }

    private AgendamentoInserido mapearInserido(ResultSet rs) throws SQLException {
        return new AgendamentoInserido(
                rs.getLong("id"),
                rs.getObject("inicio", OffsetDateTime.class),
                rs.getObject("fim", OffsetDateTime.class),
                AgendamentoStatus.valueOf(rs.getString("status")),
                rs.getString("observacao"),
                rs.getObject("criado_em", OffsetDateTime.class)
        );
    }
}
