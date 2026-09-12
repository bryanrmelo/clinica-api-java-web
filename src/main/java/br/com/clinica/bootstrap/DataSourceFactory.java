package br.com.clinica.bootstrap;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DataSourceFactory {

    private DataSourceFactory() {}

    public static HikariDataSource criar() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env("DB_URL", "jdbc:postgresql://localhost:5433/clinica"));
        config.setUsername(env("DB_USER", "clinica"));
        config.setPassword(env("DB_PASSWORD", "clinica"));
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(Integer.parseInt(env("DB_POOL_SIZE", "10")));
        config.setConnectionTimeout(5_000);
        config.setPoolName("clinica-pool");
        return new HikariDataSource(config);
    }

    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }
}
