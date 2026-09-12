package br.com.clinica.bootstrap;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    private AppContext app;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        log.info("Subindo a aplicacao...");

        try {
            HikariDataSource ds = DataSourceFactory.criar();

            Flyway.configure()
                  .dataSource(ds)
                  .locations("classpath:db/migration")   // onde estao os .sql
                  .load()
                  .migrate();

            app = new AppContext(ds);

            app.publicarEm(event.getServletContext());

            log.info("Aplicacao pronta.");

        } catch (RuntimeException e) {
            System.err.println(">>> FALHA NO STARTUP DA APLICACAO:");
            e.printStackTrace();

            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (app != null) {
            app.fechar();
            log.info("Pool encerrado.");
        }
    }
}
