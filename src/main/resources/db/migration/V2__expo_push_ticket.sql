-- Tickets devueltos por el servicio de push de Expo. ~15 minutos después de enviar se consulta
-- el recibo de cada uno: si FCM/APNs dicen que el token ya no existe, se borra el dispositivo.
CREATE TABLE expo_push_ticket (
    ticket_id  VARCHAR(64)  PRIMARY KEY,
    push_token VARCHAR(512) NOT NULL,
    creado_en  DATETIME(6)  NOT NULL
);
CREATE INDEX ix_expo_ticket_creado ON expo_push_ticket (creado_en);
