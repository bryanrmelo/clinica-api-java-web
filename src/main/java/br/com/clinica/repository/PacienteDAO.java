package br.com.clinica.repository;

import br.com.clinica.dto.NovoPaciente;
import br.com.clinica.model.Paciente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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

    // SQL em constante: fica facil de achar, e o compilador junta as strings
    // em tempo de compilacao (custo zero em execucao).
    private static final String SQL_LISTAR = """
            SELECT id, nome, cpf, email, telefone, nascimento, criado_em
              FROM pacientes
             ORDER BY nome
             LIMIT ? OFFSET ?
            """;

    private static final String SQL_POR_ID = """
            SELECT id, nome, cpf, email, telefone, nascimento, criado_em
              FROM pacientes
             WHERE id = ?
            """;

    private static final String SQL_EXISTE_CPF = "SELECT 1 FROM pacientes WHERE cpf = ?";

    // RETURNING e especifico do Postgres e resolve um problema chato:
    // o INSERT ja devolve a linha gravada, com id e criado_em preenchidos
    // pelo banco. Sem isso seriam duas idas ao banco (INSERT + SELECT).
    private static final String SQL_INSERIR = """
            INSERT INTO pacientes (nome, cpf, email, telefone, nascimento)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, nome, cpf, email, telefone, nascimento, criado_em
            """;

    public List<Paciente> listar(Connection conn, int limite, int offset) throws SQLException {
        // try-with-resources: o que for declarado nos parenteses e fechado
        // automaticamente no fim do bloco, mesmo se der excecao.
        // Esquecer de fechar Statement e ResultSet vaza memoria no servidor
        // e cursores no banco -- e o vazamento classico de JDBC na mao.
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR)) {

            // PreparedStatement e o que impede SQL injection: o valor viaja
            // SEPARADO do texto do comando, entao nao ha como "escapar" dele.
            // Os indices comecam em 1, nao em 0.
            ps.setInt(1, limite);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<Paciente> resultado = new ArrayList<>();
                // rs.next() avanca uma linha e devolve false quando acabou.
                // O cursor comeca ANTES da primeira linha, por isso o while
                // ja lê a primeira na primeira chamada.
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
                // Optional em vez de devolver null: quem chama e OBRIGADO pelo
                // compilador a pensar no caso "nao achei".
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public boolean existeCpf(Connection conn, String cpf) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTE_CPF)) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();   // achou alguma linha?
            }
        }
    }

    public Paciente inserir(Connection conn, NovoPaciente cmd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR)) {
            ps.setString(1, cmd.nome());
            ps.setString(2, cmd.cpf());

            // Campos opcionais: setString(null) funciona no Postgres, mas
            // setNull com o tipo explicito e o jeito correto e portavel.
            if (cmd.email() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, cmd.email());

            if (cmd.telefone() == null) ps.setNull(4, Types.VARCHAR);
            else ps.setString(4, cmd.telefone());

            // O driver do Postgres converte LocalDate <-> DATE sozinho.
            // Antes do Java 8 isso exigia java.sql.Date e era um inferno.
            ps.setObject(5, cmd.nascimento());

            // executeQuery (e nao executeUpdate) porque o RETURNING faz o
            // INSERT devolver linhas, igual a um SELECT.
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapear(rs);
            }
        }
    }

    /**
     * ResultSet -> objeto Java, campo por campo.
     *
     * Escrever isto umas cinco vezes ensina mais sobre ORM do que qualquer
     * artigo: e EXATAMENTE este trabalho que o Hibernate automatiza.
     */
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
