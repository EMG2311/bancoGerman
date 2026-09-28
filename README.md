# Backend de deudas P2P

Spring Boot 4.1 · Java 21 · MySQL 8 · Flyway · JWT (HS256).

## Correr

1. MySQL en `localhost:3306`. La base `deudas` se crea sola y Flyway aplica `db/migration`.
2. Credenciales: `src/main/resources/application-local.properties` (fuera de git), con
   `spring.datasource.password` y `app.jwt.secret` (≥ 32 caracteres). También se pueden pasar
   como variables de entorno `DB_PASSWORD` y `JWT_SECRET`.
3. `./mvnw spring-boot:run` → `http://localhost:8080`

Tests: `./mvnw test` (usan la base `deudas_test`).

## Reglas de negocio

- Una relación es **acreedor → deudor**. Deudas mutuas = dos relaciones.
- El acreedor invita por email; la relación queda `INVITADA` hasta que el deudor la **acepta**.
- `saldo` nunca es negativo. Un pago que supera lo que queda por pagar (saldo − pagos pendientes) da **422**.
- `requiereConfirmacion` (por relación, lo maneja el acreedor) solo afecta pagos **del deudor**:
  nacen `PENDIENTE` y el saldo baja recién cuando el acreedor confirma. No es retroactivo.
- El acreedor también puede registrar pagos recibidos (`TRANSFERENCIA` / `EFECTIVO`): nacen confirmados.
- Eliminar es borrado lógico. El deudor solo puede eliminar si no debe nada. Lo pendiente queda rechazado.
- El saldo es un caché del ledger; `GET /api/relaciones/{id}/saldo` muestra ambos y si coinciden.
- Permisos: si no sos parte de la relación → 404. Si sos parte pero del lado equivocado → 403.

## Endpoints

Todos menos `/api/auth/**` requieren `Authorization: Bearer <token>`.

| Método | Ruta | Quién | Body |
|---|---|---|---|
| POST | `/api/auth/registro` | — | `{nombre, email, telefono?, password}` |
| POST | `/api/auth/login` | — | `{email, password}` |
| GET / PATCH | `/api/usuarios/me` | — | `{nombre?, telefono?}` |
| POST | `/api/relaciones` | acreedor | `{emailDeudor, requiereConfirmacion?}` |
| GET | `/api/relaciones?rol=ACREEDOR\|DEUDOR&estado=` | parte | |
| GET | `/api/relaciones/{id}` | parte | |
| POST | `/api/relaciones/{id}/aceptar` · `/rechazar` | deudor | |
| PATCH | `/api/relaciones/{id}/confirmacion` | acreedor | `{requiereConfirmacion}` |
| DELETE | `/api/relaciones/{id}` | parte | |
| GET | `/api/relaciones/{id}/saldo` | parte | |
| GET | `/api/relaciones/{id}/movimientos?estado=&pagina=&tamanio=` | parte | |
| POST | `/api/relaciones/{id}/cargos` | acreedor | `{monto, descripcion?}` |
| POST | `/api/relaciones/{id}/pagos` | parte | `{monto, medioPago, descripcion?}` |
| POST | `/api/movimientos/{id}/confirmar` · `/rechazar` | acreedor | |
| POST | `/api/relaciones/{id}/solicitudes` | acreedor | `{monto, descripcion?}` |
| GET | `/api/relaciones/{id}/solicitudes?estado=` | parte | |
| POST | `/api/solicitudes/{id}/pagar` | deudor | `{medioPago}` |
| POST | `/api/solicitudes/{id}/rechazar` | deudor | |
| POST / GET | `/api/dispositivos` | — | `{pushToken, plataforma: ANDROID\|IOS\|WEB}` |
| DELETE | `/api/dispositivos/{id}` | dueño | |
| GET | `/api/notificaciones?soloNoLeidas=&pagina=&tamanio=` | — | |
| GET | `/api/notificaciones/no-leidas/cantidad` | — | |
| PATCH | `/api/notificaciones/{id}/leida` | — | |
| POST | `/api/notificaciones/leer-todas` | — | |

Errores en formato `application/problem+json` (`status`, `detail`, y `errores` por campo en los 400).

## Push

Proveedor configurable con `app.push.proveedor`:

- `expo` (default): envío real por el servicio de push de Expo, que entrega por FCM (Android) / APNs (iOS).
  La app registra su token (`ExponentPushToken[...]`) en `POST /api/dispositivos`.
- `log`: no manda nada, solo loguea.

Cómo funciona:

1. La operación de negocio (pago, cobro, invitación...) publica un evento.
2. Después del commit se guarda la `Notificacion` en el historial y el push sale **en otro hilo**
   (`PushDespachador`, `@Async`): la API no espera a Expo y si Expo falla la operación no se ve afectada.
3. `ExpoPushSender` manda a cada dispositivo del destinatario. Si Expo dice `DeviceNotRegistered`,
   el dispositivo se borra en el acto. Si lo acepta, se guarda el ticket (`expo_push_ticket`).
4. `ExpoRecibos` corre cada 5 minutos y revisa los recibos de los tickets con más de 15 minutos: ahí
   FCM/APNs informan tokens de apps desinstaladas (se borran) o credenciales faltantes (`InvalidCredentials`,
   se loguea como error: faltan las credenciales FCM en EAS, ver `../App/README.md`).

Si se activa "enhanced push security" en EAS, pasar el token en `EXPO_ACCESS_TOKEN`.
