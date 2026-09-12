-- Nome do arquivo segue a convencao rigida do Flyway:
--   V + numero + DOIS underscores + descricao + .sql
-- O Flyway cria a tabela flyway_schema_history e registra o que ja rodou,
-- entao subir a aplicacao dez vezes aplica cada script uma vez so.
--
-- REGRA DE OURO: migration ja aplicada NUNCA se edita. Precisa mudar? Cria V3.
-- Editar altera o checksum e o Flyway se recusa a subir (protecao, nao chateacao).

CREATE TABLE pacientes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    cpf         CHAR(11)     NOT NULL UNIQUE,
    email       VARCHAR(150),
    telefone    VARCHAR(20),
    nascimento  DATE         NOT NULL,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE dentistas (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    cro         VARCHAR(20)  NOT NULL UNIQUE,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE agendamentos (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id  BIGINT      NOT NULL REFERENCES pacientes(id),
    dentista_id  BIGINT      NOT NULL REFERENCES dentistas(id),

    -- TIMESTAMPTZ (com fuso) e nao TIMESTAMP: guarda o instante absoluto.
    -- Sem fuso voce descobre o problema no primeiro horario de verao.
    inicio       TIMESTAMPTZ NOT NULL,
    fim          TIMESTAMPTZ NOT NULL,

    status       VARCHAR(20) NOT NULL DEFAULT 'AGENDADO',
    observacao   TEXT,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Constraint de dominio: o banco recusa intervalo invertido,
    -- independente de bug na aplicacao.
    CONSTRAINT agendamento_intervalo_valido CHECK (fim > inicio),
    CONSTRAINT agendamento_status_valido
        CHECK (status IN ('AGENDADO', 'CONFIRMADO', 'CONCLUIDO', 'CANCELADO'))
);

-- Indices para as consultas que voce vai fazer sempre:
-- "agenda do dentista X no dia Y" e "historico do paciente Z".
CREATE INDEX idx_agendamentos_dentista_inicio ON agendamentos (dentista_id, inicio);
CREATE INDEX idx_agendamentos_paciente        ON agendamentos (paciente_id);
