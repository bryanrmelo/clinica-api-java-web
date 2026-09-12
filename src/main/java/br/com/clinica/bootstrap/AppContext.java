package br.com.clinica.bootstrap;

import br.com.clinica.repository.AgendamentoDAO;
import br.com.clinica.repository.DentistaDAO;
import br.com.clinica.repository.PacienteDAO;
import br.com.clinica.service.AgendamentoService;
import br.com.clinica.service.DentistaService;
import br.com.clinica.service.PacienteService;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;

public final class AppContext {

    private static final String CHAVE = AppContext.class.getName();

    private final HikariDataSource dataSource;
    private final PacienteService pacienteService;
    private final DentistaService dentistaService;
    private final AgendamentoService agendamentoService;

    AppContext(HikariDataSource dataSource) {
        this.dataSource = dataSource;

        PacienteDAO pacienteDao = new PacienteDAO();
        DentistaDAO dentistaDAO = new DentistaDAO();
        AgendamentoDAO agendamentoDAO = new AgendamentoDAO();

        this.pacienteService = new PacienteService(pacienteDao, dataSource);
        this.dentistaService = new DentistaService(dentistaDAO, dataSource);
        this.agendamentoService = new AgendamentoService(agendamentoDAO, pacienteDao, dentistaDAO, dataSource);
    }

    public PacienteService pacientes() {
        return pacienteService;
    }
    public DentistaService dentistas() { return dentistaService; }
    public AgendamentoService agendamentos() { return agendamentoService; }

    void publicarEm(ServletContext ctx) {
        ctx.setAttribute(CHAVE, this);
    }

    public static AppContext de(ServletContext ctx) {

        AppContext app = (AppContext) ctx.getAttribute(CHAVE);

        if (app == null) {
            throw new IllegalStateException(
                    "AppContext ausente -- o AppContextListener nao rodou.");
        }
        return app;
    }

    void fechar() {
        dataSource.close();
    }
}
