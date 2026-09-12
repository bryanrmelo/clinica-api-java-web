-- Impede dois agendamentos sobrepostos para o MESMO dentista,
-- no proprio banco, de forma atomica.
--
-- Por que isso importa: a checagem em Java ("existe conflito? nao? entao insere")
-- tem race condition. Duas requisicoes simultaneas podem passar as duas pelo
-- SELECT antes de qualquer INSERT acontecer, e as duas gravam. A constraint
-- abaixo e a unica garantia real -- o banco serializa a verificacao.
--
-- Isso e um assunto excelente para citar numa entrevista tecnica.

-- btree_gist permite misturar comparacao de igualdade (=) com sobreposicao (&&)
-- dentro do mesmo indice GiST.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE agendamentos
    ADD CONSTRAINT agendamentos_sem_conflito
    EXCLUDE USING gist (
        dentista_id WITH =,                      -- mesmo dentista
        tstzrange(inicio, fim, '[)') WITH &&     -- e intervalos que se cruzam
    )
    -- '[)' = inicio incluso, fim excluso. Assim 09:00-10:00 e 10:00-11:00
    -- NAO conflitam, que e o comportamento desejado numa agenda.
    WHERE (status <> 'CANCELADO');               -- cancelado nao ocupa horario
