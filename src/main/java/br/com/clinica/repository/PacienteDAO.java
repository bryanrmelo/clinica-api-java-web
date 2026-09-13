package br.com.clinica.repository;

import br.com.clinica.dto.AtualizarPaciente;
import br.com.clinica.dto.NovoPaciente;
import br.com.clinica.model.Paciente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO = Data Access Object. Aqui SO existe SQL. Nenhuma regra de negocio.
 *
 * O detalhe mais importante da classe: todo metodo RECEBE a Connection em vez
 * de pegar uma do pool. Isso e o que permite ao service rodar varias chamadas
 * de DAO dentro da MESMA transacao -- se cada metodo pegasse sua propria
 * conexao, cada um estaria numa transacao separada e o rollback nao cobriria
 * o conjunto.
 *
 * A classe nao tem NENHUM campo. Isso a torna stateless e, por consequencia,
 * segura para ser compartilhada por todas as threads do Tomcat.
 */
public class PacienteDAO {

    private static final String TABLE_NAME = "pacientes";

    // SQL em constante: fica facil de achar, e o compilador junta as strings
    // em tempo de compilacao (custo zero em execucao).
    private static final String SQL_LISTAR = """
            SELECT id, nome, cpf, email, telefone, nascimento, criado_em
              FROM %s
             ORDER BY nome
             LIMIT ? OFFSET ?
            """.formatted(TABLE_NAME);

    private static final String SQL_POR_ID = """
            SELECT id, nome, cpf, email, telefone, nascimento, criado_em
              FROM %s
             WHERE id = ?
            """.formatted(TABLE_NAME);

    private static final String SQL_EXISTE_CPF = "SELECT 1 FROM %s WHERE cpf = ?".formatted(TABLE_NAME);

    // RETURNING e especifico do Postgres e resolve um problema chato:
    // o INSERT ja devolve a linha gravada, com id e criado_em preenchidos
    // pelo banco. Sem isso seriam duas idas ao banco (INSERT + SELECT).
    private static final String SQL_INSERIR = """
            INSERT INTO %s (nome, cpf, email, telefone, nascimento)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, nome, cpf, email, telefone, nascimento, criado_em
            """.formatted(TABLE_NAME);

    private static final String SQL_ATUALIZAR = """
            UPDATE %s 
            SET nome = ?, email = ?, telefone = ?, nascimento = ?
            WHERE id = ?
            RETURNING id, nome, cpf, email, telefone, nascimento, criado_em
            """.formatted(TABLE_NAME);

    public List<Paciente> listar(Connection conn, int limite, int offset) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR)) {
            ps.setInt(1, limite);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<Paciente> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    public Optional<Paciente> buscarPorId(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_POR_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public boolean existeCpf(Connection conn, String cpf) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTE_CPF)) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Paciente inserir(Connection conn, NovoPaciente cmd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR)) {
            ps.setString(1, cmd.nome());
            ps.setString(2, cmd.cpf());

            if (cmd.email() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, cmd.email());

            if (cmd.telefone() == null) ps.setNull(4, Types.VARCHAR);
            else ps.setString(4, cmd.telefone());

            ps.setObject(5, cmd.nascimento());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapear(rs);
            }
        }
    }

    public Optional<Paciente> atualizar(Connection conn, Long id, AtualizarPaciente cmd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR)) {
            ps.setString(1, cmd.nome());
            ps.setString(2, cmd.email());
            ps.setString(3, cmd.telefone());
            ps.setObject(4, cmd.nascimento());

            ps.setLong(5, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    private Paciente mapear(ResultSet rs) throws SQLException {
        return new Paciente(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("cpf"),
                rs.getString("email"),
                rs.getString("telefone"),
                // getObject com a classe alvo (Java 8+) evita conversao manual.
                rs.getObject("nascimento", java.time.LocalDate.class),
                rs.getObject("criado_em", java.time.OffsetDateTime.class)
        );
    }
}
