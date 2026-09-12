package br.com.clinica.bootstrap;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O "main()" que uma aplicacao web nao tem.
 *
 * Numa aplicacao normal voce montaria tudo no main(). Aqui quem roda e o
 * Tomcat: a sua aplicacao e so um .war que ele carrega. A anotacao
 * @WebListener registra esta classe para ser avisada no ciclo de vida.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    // Campo (e nao variavel local) porque os dois metodos rodam em momentos
    // diferentes e o contextDestroyed precisa alcancar o mesmo objeto.
    private AppContext app;

    /**
     * Chamado UMA vez, quando a aplicacao sobe, antes de aceitar qualquer
     * requisicao. Se algo aqui estourar, o Tomcat nao publica a aplicacao --
     * o que e o comportamento certo: melhor nao subir do que subir quebrada.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        log.info("Subindo a aplicacao...");

        try {
            HikariDataSource ds = DataSourceFactory.criar();

            // Migrations ANTES de tudo: garante que o banco esta no formato que
            // o codigo espera. O Flyway consulta a tabela flyway_schema_history
            // e aplica so o que ainda falta.
            Flyway.configure()
                  .dataSource(ds)
                  .locations("classpath:db/migration")   // onde estao os .sql
                  .load()
                  .migrate();

            // So agora monta o grafo de objetos, com o pool pronto na mao.
            app = new AppContext(ds);

            // "event" e o aviso do Tomcat; dele tiramos o ServletContext, o mapa
            // compartilhado que todos os servlets vao enxergar depois.
            app.publicarEm(event.getServletContext());

            log.info("Aplicacao pronta.");

        } catch (RuntimeException e) {
            // Sem este bloco, o Tomcat engole o stack trace e o console mostra
            // apenas "One or more listeners failed to start" -- e voce vai
            // cacar o erro real em logs/localhost.AAAA-MM-DD.log.
            //
            // System.err de proposito: se o problema for justamente o
            // logging (SLF4J mal resolvido), nem o log.error apareceria.
            System.err.println(">>> FALHA NO STARTUP DA APLICACAO:");
            e.printStackTrace();

            // Relanca: a aplicacao NAO deve subir pela metade.
            // Melhor nao subir do que responder requisicao sem banco.
            throw e;
        }
    }

    /** Chamado quando a aplicacao para ou e redeployada. Lugar da faxina. */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // A checagem protege um caso real: se contextInitialized explodiu no
        // meio (banco fora do ar), "app" nunca foi preenchido -- e o Tomcat
        // chama este metodo assim mesmo.
        if (app != null) {
            app.fechar();
            log.info("Pool encerrado.");
        }
    }
}
