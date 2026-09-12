package br.com.clinica.bootstrap;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Cria o pool de conexoes lendo configuracao do AMBIENTE, nunca do codigo.
 *
 * Assim o mesmo .war roda identico em dev, homologacao e producao -- so as
 * variaveis de ambiente mudam. Senha comitada no Git e um problema serio e
 * dificil de desfazer (fica no historico para sempre).
 */
public final class DataSourceFactory {

    // Construtor privado: esta classe so tem metodos static, ninguem deve
    // dar "new DataSourceFactory()".
    private DataSourceFactory() {}

    public static HikariDataSource criar() {
        HikariConfig config = new HikariConfig();

        // Os defaults sao os valores do docker-compose.yml, para o projeto
        // subir sem configuracao nenhuma na sua maquina.
        config.setJdbcUrl(env("DB_URL", "jdbc:postgresql://localhost:5432/clinica"));
        config.setUsername(env("DB_USER", "clinica"));
        config.setPassword(env("DB_PASSWORD", "clinica"));

        // Tamanho do pool. Regra pratica: comece BAIXO. Mais conexoes nao
        // significa mais throughput -- cada conexao e um processo no Postgres,
        // e pool grande demais deixa o banco mais lento, nao mais rapido.
        config.setMaximumPoolSize(Integer.parseInt(env("DB_POOL_SIZE", "10")));

        // Quanto esperar por uma conexao livre antes de desistir.
        // Sem isso, sob carga, as threads ficam presas para sempre e a
        // aplicacao "trava" sem nenhum erro no log.
        config.setConnectionTimeout(5_000);

        config.setPoolName("clinica-pool");
        return new HikariDataSource(config);
    }

    /** Le a variavel de ambiente; se nao existir ou estiver vazia, usa o padrao. */
    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }
}
