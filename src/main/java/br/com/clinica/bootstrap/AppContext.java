package br.com.clinica.bootstrap;

import br.com.clinica.repository.AgendamentoDAO;
import br.com.clinica.repository.DentistaDAO;
import br.com.clinica.repository.PacienteDAO;
import br.com.clinica.service.AgendamentoService;
import br.com.clinica.service.DentistaService;
import br.com.clinica.service.PacienteService;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;

/**
 * O registro de objetos da aplicacao -- o "container de injecao de
 * dependencia" feito a mao. E o papel do ApplicationContext do Spring,
 * so que montado por voce em vez de descoberto por reflexao.
 *
 * O construtor abaixo e, na pratica, a documentacao da arquitetura:
 * bate o olho e ve o sistema inteiro montado, em ordem.
 */
public final class AppContext {

    // A chave usada no mapa do ServletContext. Derivada do nome da classe,
    // entao nao ha risco de typo e nao colide com nenhuma biblioteca.
    private static final String CHAVE = AppContext.class.getName();

    private final HikariDataSource dataSource;
    private final PacienteService pacienteService;
    private final DentistaService dentistaService;
    private final AgendamentoService agendamentoService;

    /**
     * Sem "public": so o AppContextListener, que esta no mesmo pacote,
     * consegue instanciar. Um servlet nao cria outro por engano.
     *
     * Recebe o pool pronto em vez de cria-lo: isso ja e injecao de dependencia.
     */
    AppContext(HikariDataSource dataSource) {
        this.dataSource = dataSource;

        // A corrente e montada de baixo para cima: primeiro quem nao depende
        // de ninguem (DAOs), depois quem depende deles (services).
        PacienteDAO pacienteDao = new PacienteDAO();
        DentistaDAO dentistaDAO = new DentistaDAO();
        AgendamentoDAO agendamentoDAO = new AgendamentoDAO();

        // Os DAOs sao variaveis locais de proposito. Eles nao somem no fim do
        // construtor, porque os services guardam referencia. So nao ficam
        // acessiveis de fora -- ninguem deve falar com um DAO sem passar
        // pelo service, que e quem controla a transacao.
        this.pacienteService = new PacienteService(pacienteDao, dataSource);
        this.dentistaService = new DentistaService(dentistaDAO, dataSource);
        this.agendamentoService = new AgendamentoService(agendamentoDAO, pacienteDao, dentistaDAO, dataSource);

        // Quando voce criar dentista e agendamento, e aqui que eles entram:
        //
        // DentistaDao dentistaDao = new DentistaDao();
        // AgendamentoDao agendamentoDao = new AgendamentoDao();
        // this.agendamentoService =
        //         new AgendamentoService(agendamentoDao, pacienteDao, dentistaDao, dataSource);
    }

    // Acesso tipado: sem cast, sem string. Se voce renomear o metodo,
    // a IDE acusa em todos os lugares na hora.
    public PacienteService pacientes() {
        return pacienteService;
    }
    public DentistaService dentistas() { return dentistaService; }
    public AgendamentoService agendamentos() { return agendamentoService; }

    /** Guarda ESTE objeto no mapa compartilhado do Tomcat. So o listener chama. */
    void publicarEm(ServletContext ctx) {
        ctx.setAttribute(CHAVE, this);
    }

    /**
     * O caminho inverso: dado o ServletContext, devolve o AppContext.
     *
     * "static" porque quem chama ainda nao tem um AppContext em maos -- e
     * justamente o que esta tentando obter. Usa-se como AppContext.de(ctx).
     */
    public static AppContext de(ServletContext ctx) {
        // getAttribute devolve Object (o mapa aceita qualquer coisa), entao o
        // cast e inevitavel. O ganho e que ele aparece UMA vez, aqui dentro,
        // em vez de espalhado por todos os servlets.
        AppContext app = (AppContext) ctx.getAttribute(CHAVE);

        if (app == null) {
            // Falhar aqui, com mensagem clara, e muito melhor do que um
            // NullPointerException confuso la no meio de uma requisicao.
            throw new IllegalStateException(
                    "AppContext ausente -- o AppContextListener nao rodou.");
        }
        return app;
    }

    /** Devolve as conexoes ao Postgres e mata as threads do Hikari. */
    void fechar() {
        dataSource.close();
    }
}
