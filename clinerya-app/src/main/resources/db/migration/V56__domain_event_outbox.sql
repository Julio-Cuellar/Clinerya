-- Outbox de eventos de dominio. Los servicios dejaban el evento directo en RabbitMQ desde dentro
-- de la transaccion del negocio: un rollback despues de publicar dejaba un evento fantasma, y un
-- broker caido perdia el evento aunque el cambio si se guardara. Ahora el evento se escribe aqui,
-- en la misma transaccion, y OutboxRelayScheduler lo entrega despues.
--
-- seq fija el orden de entrega. created_at no alcanza: los eventos de una misma transaccion
-- comparten la marca de tiempo. Los eventos entregados se borran; dead_at aparta los que ya no se
-- pueden reconstruir para que no bloqueen la cola.
CREATE SCHEMA IF NOT EXISTS messaging;

CREATE TABLE IF NOT EXISTS messaging.outbox_events (
    id           uuid PRIMARY KEY,
    seq          bigint GENERATED ALWAYS AS IDENTITY,
    routing_key  varchar(200) NOT NULL,
    payload_type varchar(300) NOT NULL,
    payload      text         NOT NULL,
    created_at   timestamp    NOT NULL,
    attempts     integer      NOT NULL DEFAULT 0,
    last_error   text,
    dead_at      timestamp
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_pending
    ON messaging.outbox_events (seq)
    WHERE dead_at IS NULL;
