# clinica-api

API REST de agendamento odontologico em **Java Web puro**: Servlet + JDBC no Tomcat.
Sem Spring, sem JPA, sem CDI. Projeto de estudo para enxergar o que os frameworks
fazem por baixo.

## Stack

| Peca | Escolha | Por que |
|---|---|---|
| Runtime | Java 21 | records, switch pattern matching, text blocks |
| Container | Tomcat 11 (`.war`) | namespace `jakarta.*` |
| Web | `jakarta.servlet-api` | roteamento na mao, sem framework MVC |
| Banco | PostgreSQL 16 via Docker | |
| Acesso a dados | JDBC + HikariCP | sem ORM, SQL escrito a mao |
| Migrations | Flyway | versionamento do schema |
| JSON | Jackson | serializar na mao seria masoquismo |

## Antes de rodar

As versoes estao todas no bloco `<properties>` do `pom.xml`. **Confira se ainda
sao as atuais** em https://central.sonatype.com antes do primeiro build -- foram
escritas em setembro/2026 e nao passaram por compilacao.

Pontos de atencao:

- **Tomcat 10.1** usa `jakarta.servlet-api` **6.0.0**; **Tomcat 11** usa **6.1.0**.
  Trocar de servidor sem trocar essa versao da erro obscuro no deploy.
- **Jackson 2 e 3 convivem no war, de proposito.** O `Json.java` usa o 2.x
  (`com.fasterxml.jackson`); o Flyway 13 traz o 3.x (`tools.jackson`) por conta
  propria. Nao ha conflito -- groupId e pacote sao diferentes, cada um usa o seu.
  O custo e uns 2,5 MB a mais no war. Se ver `jackson-databind` duas vezes em
  `WEB-INF/lib`, e isso, e esta correto.
- **SLF4J travado em 2.x pelo `dependencyManagement`.** O HikariCP declara
  slf4j-api 1.7.36, incompativel com o Logback 1.5. Sem a trava, o 1.7 vence a
  resolucao e voce perde todos os logs. Nao remova aquele bloco.

## Duas pedras no caminho (ja resolvidas aqui)

Se voce recriar esse projeto do zero um dia, sao os dois erros que vao aparecer:

**`SQLException: No suitable driver`, com o postgresql.jar presente em
`WEB-INF/lib`.** O `DriverManager` varre os drivers disponiveis uma unica vez,
quando a classe e inicializada -- no Tomcat isso normalmente ja aconteceu antes
de o classloader da webapp existir, entao o seu driver nunca se registra.
Resolvido com `config.setDriverClassName("org.postgresql.Driver")`, que faz o
Hikari carregar a classe pelo classloader certo.

**`One or more listeners failed to start`, sem nenhum stack trace.** O Tomcat
manda o erro real para `logs/localhost.AAAA-MM-DD.log`, nao para o console da
IDE (`catalina.*.log` e do servidor; `localhost.*.log` e da sua aplicacao).
O `try/catch` com `System.err` no `AppContextListener` traz o stack trace de
volta para o console.

## Rodando

```bash
docker compose up -d          # sobe o Postgres na porta 5433 do host
docker compose ps             # confirme "0.0.0.0:5433->5432/tcp"
mvn clean package             # gera target/clinica-api.war
```

Testando o banco antes de subir o Tomcat (o `-p 5433` e obrigatorio, senao o
psql vai na 5432 e voce testa o banco errado):

```bash
psql -h localhost -p 5433 -U clinica -d clinica -c "select 1"
```

Depois, ou voce copia o `.war` para `webapps/` de um Tomcat instalado, ou --
melhor no dia a dia -- configura um *Run Configuration* do tipo Tomcat na IDE
apontando para o war explodido, e ganha hot reload.

O Flyway roda sozinho no startup: nao precisa criar tabela na mao.

### Configuracao

Tudo por variavel de ambiente, com defaults que batem com o `docker-compose.yml`:

| Variavel | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/clinica` |
| `DB_USER` | `clinica` |
| `DB_PASSWORD` | `clinica` |
| `DB_POOL_SIZE` | `10` |

O mesmo `.war` roda em dev e em producao; so o ambiente muda.

## Endpoints prontos

```bash
# cadastrar
curl -i -X POST http://localhost:8080/clinica-api/api/pacientes \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Maria Souza","cpf":"12345678901","email":"maria@ex.com","nascimento":"1990-05-14"}'

# listar (pagina e limite valem para pacientes, dentistas e agendamentos)
curl "http://localhost:8080/clinica-api/api/pacientes?pagina=0&limite=20"

# buscar
curl -i http://localhost:8080/clinica-api/api/pacientes/1

# erros
curl -i http://localhost:8080/clinica-api/api/pacientes/999   # 404
curl -i http://localhost:8080/clinica-api/api/pacientes/abc   # 400
```

O `clinica-api` no meio da URL e o *context path* (vem do `<finalName>`).
Se voce fizer deploy como `ROOT.war`, some.

## TODO (a sua parte)

Feito ate aqui: CRUD de leitura + POST nas tres entidades, transacao no
agendamento, constraint de conflito de horario.

### 1. Escrita: PUT e DELETE

- [ ] `PUT /api/pacientes/{id}` -- atualizar
- [ ] `DELETE /api/pacientes/{id}` -- **decida antes de codar:** paciente com
      agendamento tem FK apontando para ele e o banco recusa (`23503`).
      Ou vira 409 ("tem agendamentos"), ou vira soft delete (coluna `ativo`)
- [ ] `PUT` e `DELETE` em dentista -- mesmo dilema do FK
- [ ] `doPut`/`doDelete` ausentes devolvem 405 em **HTML** (padrao do
      `HttpServlet`), nao no formato `ErroResponse`. Some sozinho conforme
      voce implementa

### 2. Cancelamento e remarcacao

- [ ] Cancelar agendamento -- `UPDATE ... SET status = 'CANCELADO'`.
      A constraint `agendamentos_sem_conflito` tem `WHERE status <> 'CANCELADO'`,
      entao o horario se libera sozinho. **Decida antes:** `POST /{id}/cancelar`
      (acao, regra explicita) ou `PATCH /{id}` com o novo status (mais REST)
- [ ] Bloquear transicao invalida: nao se cancela `CONCLUIDO` nem `CANCELADO`
      de novo -> `ConflitoException` -> 409
- [ ] `PUT /api/agendamentos/{id}` (remarcar) -- o mais dificil: `UPDATE` dentro
      de transacao, e a `EXCLUDE` pode estourar `23P01` contra OUTRO agendamento

### 3. Bugs e inconsistencias conhecidos

- [x] ~~JSON malformado / corpo vazio devolvia 500~~ -- agora 400 no `Json.ler`
- [x] ~~`?tamanho` no paciente vs `?limite` nos outros~~ -- padronizado em `limite`
- [ ] `PacienteService.cadastrar` nao trata `unique_violation` (`23505`).
      Duas requisicoes simultaneas com o mesmo CPF passam as duas pelo
      `existeCpf` e a segunda leva 500 em vez de 409. O `DentistaService`
      ja faz certo -- e so copiar
- [ ] `/api/pacientes/1/extra` devolve 400 (`Long.parseLong` estoura); dentista
      e agendamento devolvem 404. O `PacienteServlet` e o mais antigo e nao
      acompanhou o `split("/")` que os outros dois adotaram
- [ ] Mensagem errada quando `inicio == fim`: diz "inicio nao pode ser depois
      de fim" (`AgendamentoService.validar`)

### 4. Regras de negocio que faltam

- [ ] Paciente pode estar em duas cadeiras ao mesmo tempo: a `EXCLUDE` cobre
      `dentista_id`, nao `paciente_id`. Fecha com uma `V3` e uma segunda
      constraint, mesma tecnica
- [ ] `GET /api/agendamentos` nao aceita filtro nenhum, mas o indice
      `idx_agendamentos_dentista_inicio` foi criado justamente para
      "agenda do dentista X no dia Y" -- a consulta mais usada numa clinica.
      (E `ORDER BY inicio DESC` numa agenda e discutivel: o natural e o
      proximo primeiro)
- [ ] Nada impede agendar no passado, nem limita duracao
- [ ] Sem autenticacao: a lista de pacientes e publica, o que anula o
      mascaramento de CPF do `PacienteResponse`

### 5. Testes (o `pom` ja traz JUnit 5 + Testcontainers)

Ordem sugerida, do mais barato ao mais caro:

- [ ] `validar()` puro -- JUnit sozinho, sem banco. Primeiro verde em 5 minutos
- [ ] DAO com Testcontainers -- sobe um Postgres real, roda o Flyway
- [ ] Service com transacao -- "cancelei, o horario aceita outro agendamento"
      so da para provar contra banco de verdade

> Armadilha: o surefire so roda arquivos terminados em `Test`, `Tests` ou
> `TestCase`. Motivo numero 1 de "escrevi o teste e o Maven diz 0 tests".

Em cada passo, registre o service novo no construtor do `AppContext`.

## Mapa mental: o que cada camada faz

```
HTTP  ->  Servlet   traduz HTTP <-> objeto. Nao valida, nao sabe SQL.
      ->  Service   valida, decide, CONTROLA A TRANSACAO.
      ->  DAO       so SQL. Recebe a Connection, nunca pega do pool.
      ->  Postgres
```

## A armadilha numero um

Os objetos do `AppContext` sao criados **uma vez** e usados por **todas as
threads** ao mesmo tempo -- o Tomcat atende requisicoes em paralelo reusando
os mesmos servlets e services.

Entao service e DAO precisam ser *stateless*: nada de guardar dado da
requisicao atual num campo da classe. So variavel local dentro do metodo.
Vindo do PHP, onde cada requisicao vive isolada, esse e o erro mais facil de
cometer e o mais dificil de diagnosticar -- funciona perfeito em teste e da
resultado errado sob carga.

## Onde o Spring entra depois

| Voce escreveu a mao | O Spring faz com |
|---|---|
| `AppContext` | `ApplicationContext` + `@Service`, `@Component` |
| `getPathInfo()` + `if` | `@GetMapping("/{id}")` |
| `Json.ler` / `Json.escrever` | `@RequestBody` / `@ResponseBody` |
| `tratar()` no `BaseServlet` | `@ControllerAdvice` + `@ExceptionHandler` |
| `setAutoCommit(false)` + `commit`/`rollback` | `@Transactional` |
| `validar()` na mao | Bean Validation (`@NotBlank`, `@Past`) |
| `PacienteDAO` inteiro | Spring Data JPA |

Depois de fazer os dois, voce consegue explicar cada linha da coluna da direita.
