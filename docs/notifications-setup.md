# Correo y notificaciones externas (RF-001, RF-003, RF-051)

Este documento describe cómo terminar de configurar los envíos reales de
**correos** (verificación de cuenta y restablecimiento de contraseña) y de
**mensajes** (notificación in-app + correo + push FCM).

El código ya está implementado y es funcional sin credenciales: en desarrollo el
transporte de correo es `log` y el push está desactivado, así que la aplicación
arranca igual que antes. Para producción solo hay que aportar configuración.

## 0. Estado de este workspace

- Proyecto Firebase: **`coolivinapp`** (cuenta `ddrmhk1@gmail.com`).
- App Web registrada y configurada en `client/src/environments/environment*.ts` y
  `client/public/firebase-messaging-sw.js`.
- Firestore: base `(default)` en `nam5`, con reglas cerradas desplegadas
  (`firestore.rules`). El backend usará el Admin SDK, que no pasa por las reglas.
- APIs `firestore.googleapis.com` y `fcm.googleapis.com` habilitadas.
- Cuenta de servicio: `firebase-adminsdk-fbsvc@coolivinapp.iam.gserviceaccount.com`
  (rol `roles/firebase.sdkAdminServiceAgent`, que incluye FCM y Firestore).
  Clave generada en `api/secrets/firebase-service-account.json` (ignorada por
  git, permisos `600`).
- Backend local: `api/.env` se carga automáticamente con
  `DotenvEnvironmentPostProcessor` (ignorado por git). El correo usa **Mailtrap**
  y el push FCM está habilitado.
- Mailtrap: cuenta `Daniel` (id `2839528`), dominio verificado
  **`rangelm.site`**. Remitente `no-reply@rangelm.site`; token en `api/.env`.
  Envío real verificado desde el backend (registro `201` + `message_ids` de
  Mailtrap). El dominio demo `hello@demomailtrap.co` está agotado: no usarlo.
- Sandbox de Mailtrap disponible: inbox **`My Sandbox`** (id `4920541`) para
  pruebas sin entregar correos reales.
- **Facturación Firebase (Blaze): desactivada.** No hace falta para Mailtrap ni
  para FCM; solo la extensión *Trigger Email* la requiere (opción C).
- Pendiente: clave VAPID para push web (se genera en la consola y es pública).
  El resto del push ya está configurado.

Enlaces directos:

- Web Push certificates:
  <https://console.firebase.google.com/project/coolivinapp/settings/cloudmessaging>
- Cuentas de servicio:
  <https://console.firebase.google.com/project/coolivinapp/settings/serviceaccounts/adminsdk>
- Firestore:
  <https://console.firebase.google.com/project/coolivinapp/firestore>

## 1. Correo

El puerto `EmailSender` de `identity` delega en `shared.mail.MailGateway`. El
proveedor se elige con `app.mail.provider`:

| Valor      | Implementación                | Uso                                                        |
| ---------- | ----------------------------- | ---------------------------------------------------------- |
| `log`      | `LoggingMailGateway`          | Desarrollo: el token/link aparece en el log del backend.   |
| `mailtrap` | `MailtrapMailGateway`         | Mailtrap Email Sending API (recomendado en este proyecto). |
| `smtp`     | `SmtpMailGateway`             | Google Workspace / Gmail SMTP o cualquier relay SMTP.      |
| `firebase` | `FirestoreMailGateway`        | Extensión oficial *Trigger Email from Firestore*.          |

### Opción A — Mailtrap (recomendada)

1. En Mailtrap, entra en **Sending Domains** y verifica el dominio desde el que
   enviarás (registros DNS). En este workspace ya está verificado
   `rangelm.site`.
2. Copia el **API token** de la cuenta (Sending Domains → API tokens). En este
   workspace ya está en `api/.env` como `MAILTRAP_TOKEN`.
3. Configura el backend (normalmente en `api/.env`, no lo commitees):

```bash
MAIL_PROVIDER=mailtrap
MAIL_FROM=no-reply@rangelm.site
MAIL_FROM_NAME=ColivinApp
MAILTRAP_TOKEN=<api-token>
MAILTRAP_SANDBOX=false
APP_PUBLIC_BASE_URL=https://app.tu-dominio.com
```

4. Para probar sin enviar a bandejas reales, usa el sandbox de Mailtrap:

```bash
MAILTRAP_SANDBOX=true
MAILTRAP_INBOX_ID=4920541   # My Sandbox de esta cuenta
MAIL_FROM=hello@demomailtrap.co
```

El sandbox no entrega a destinatarios reales; el mensaje aparece en la bandeja
de Mailtrap. Para producción vuelve a `MAILTRAP_SANDBOX=false` y usa un
remitente del dominio verificado.

5. Comprueba los envíos en <https://mailtrap.io/sending/email_logs>.

> El SDK es `io.mailtrap:mailtrap-java:1.3.0`. El gateway está en
> `shared/mail/MailtrapMailGateway.kt` y el mapeo en
> `shared/mail/MailtrapMailMapper.kt`.

### Opción B — Google Workspace / Gmail SMTP (alternativa)

1. Activa la verificación en dos pasos en la cuenta de Google.
2. Crea una **contraseña de aplicación** (Cuenta de Google → Seguridad →
   Contraseñas de aplicaciones).
3. Configura el backend:

```bash
MAIL_PROVIDER=smtp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-cuenta@tu-dominio.com
MAIL_PASSWORD=<contraseña-de-aplicación>
MAIL_FROM=no-reply@tu-dominio.com
MAIL_FROM_NAME=ColivinApp
APP_PUBLIC_BASE_URL=https://app.tu-dominio.com
```

> Para un dominio propio, usa el SMTP de Google Workspace
> (`smtp.gmail.com`, usuario completo del dominio). Para otros proveedores
> (Mailtrap, Resend, SendGrid) cambia `MAIL_HOST`, `MAIL_PORT` y credenciales;
> el código no cambia.

### Opción C — Firebase Trigger Email (opcional, requiere Blaze)

> Requiere el plan **Blaze** (Cloud Functions/extensiones). En este proyecto la
> facturación está desactivada, así que la extensión no se puede instalar
> todavía. Si activas Blaze, estos son los pasos.

1. Crea un proyecto de Firebase y habilita **Firestore**.
2. Instala la extensión
   [`firebase/firestore-send-email`](https://firebase.google.com/products/extensions/firebase-firestore-send-email)
   y configura `MAIL_COLLECTION=mail` y `SMTP_CONNECTION_URI` (puede ser el SMTP
   de Google Workspace o cualquier otro).
3. Crea una **cuenta de servicio** (Configuración del proyecto → Cuentas de
   servicio → Generar nueva clave privada) y guarda el JSON fuera del repo.
4. Configura el backend:

```bash
MAIL_PROVIDER=firebase
APP_PUBLIC_BASE_URL=https://app.tu-dominio.com
FIREBASE_ENABLED=true
FIREBASE_PROJECT_ID=tu-proyecto
FIREBASE_CREDENTIALS=/ruta/absoluta/service-account.json
```

El backend escribe en la colección `mail` el documento que la extensión espera:

```json
{
  "to": ["persona@example.com"],
  "message": {
    "subject": "Verifica tu correo en ColivinApp",
    "text": "...",
    "html": "..."
  }
}
```

> También se admiten credenciales por defecto de Google (ADC) si no defines
> `FIREBASE_CREDENTIALS` (por ejemplo, en Cloud Run con una service account
> adjunta).

## 2. Push con Firebase Cloud Messaging

### 2.1 Proyecto y credenciales

1. En Firebase console, crea (o reutiliza) el proyecto.
2. Añade una **app Web** (Configuración del proyecto → Tus apps → Web).
3. En **Cloud Messaging → Certificados push web**, genera un par de claves
   (VAPID). Guarda la clave pública.
4. Genera una **cuenta de servicio** para el backend (ver opción B de correo).
5. Habilita el backend:

```bash
PUSH_ENABLED=true
FIREBASE_ENABLED=true
FIREBASE_PROJECT_ID=tu-proyecto
FIREBASE_CREDENTIALS=/ruta/absoluta/service-account.json
```

### 2.2 Frontend

Rellena los dos ficheros de entorno y el service worker con la configuración
pública de la app Web (la misma en los tres sitios):

- `client/src/environments/environment.ts` (desarrollo)
- `client/src/environments/environment.prod.ts` (producción)
- `client/public/firebase-messaging-sw.js` (background)

Campos: `apiKey`, `authDomain`, `projectId`, `storageBucket`,
`messagingSenderId`, `appId` y `vapidKey` (este último solo en los entornos).

La configuración Web de Firebase es pública: no es un secreto y no concede
permisos de administración. **Nunca** pongas la cuenta de servicio en el
frontend.

Con los valores vacíos, el push queda deshabilitado de forma limpia: la app
funciona y la tarjeta «Notificaciones push» muestra «No disponible».

### 2.3 Flujo de usuario

1. El usuario entra en **Notificaciones** y pulsa **Activar notificaciones**.
2. El navegador pide permiso (siempre por gesto del usuario).
3. El SDK obtiene un token FCM y el frontend lo registra en
   `POST /api/v1/users/me/push-tokens` con `{ "token": "...", "platform": "WEB" }`.
4. Al recibir un mensaje, el backend envía un push **sin contenido sensible**
   (título genérico + `conversationId`) a todos los tokens del destinatario.
5. En primer plano, la app muestra una `Notification`; en segundo plano la
   gestiona `firebase-messaging-sw.js`.
6. «Desactivar en este dispositivo» borra el token de FCM y del backend.

### 2.4 Endpoints nuevos

| Método   | Ruta                                     | Descripción                              |
| -------- | ---------------------------------------- | ---------------------------------------- |
| `POST`   | `/api/v1/users/me/push-tokens`           | Registra/reasigna el token del dispositivo. |
| `DELETE` | `/api/v1/users/me/push-tokens?token=...` | Elimina el token del usuario (idempotente). |

El token es único por instalación: si otra cuenta inicia sesión en el mismo
navegador, el token se reasigna. El backend nunca devuelve el token en las
respuestas.

## 3. Notificación por correo de mensajes nuevos

Además de la notificación in-app, `MessageEmailNotifier` envía un correo
genérico al destinatario con un enlace a `/mensajes/{id}`. No incluye el cuerpo
del mensaje ni datos de la reserva. Se puede desactivar sin tocar el código:

```bash
MAIL_MESSAGE_NOTIFICATIONS=false
```

Los envíos externos se ejecutan **después del commit** de la transacción del
mensaje (`@TransactionalEventListener(AFTER_COMMIT)`), de modo que un mensaje
revertido nunca genera correo ni push. Un fallo del proveedor se registra y no
bloquea el otro canal.

## 4. Verificación local

Sin `api/.env`, el perfil `dev` usa el transporte `log` y no inicializa Firebase:
la aplicación arranca sin credenciales.

En este workspace, `api/.env` ya está configurado con Mailtrap y FCM:

```bash
cd api
docker compose up -d
./gradlew bootRun
# En otra terminal:
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"tu-correo@gmail.com","password":"Password123!"}'
```

La respuesta `201` y una línea como esta confirman el envío real:

```text
[mail:mailtrap] sent category=account ids=[...]
```

Para volver al transporte de desarrollo, pon `MAIL_PROVIDER=log` en `api/.env`;
el enlace/token aparecerá en el log:

```text
[mail:log] to=... subject=Verifica tu correo en ColivinApp text=.../verificar-correo?token=...
```

Los tests **no** cargan `.env` (`spring.dotenv.enabled=false` en Gradle), así que
nunca envían correos reales:

```bash
cd api
./gradlew test --tests "com.coliving.api.messaging.*" \
  --tests "com.coliving.api.identity.infrastructure.mail.*" \
  --tests "com.coliving.api.shared.mail.*"
```

## 5. Notas de seguridad

- El token de verificación/reset sigue siendo de un solo uso y solo se guarda su
  hash; el correo transporta el valor en claro, como es habitual.
- El push no incluye contenido del mensaje (RF-051): solo título genérico y el
  identificador del hilo.
- Los tokens FCM se eliminan automáticamente cuando FCM responde
  `UNREGISTERED` o `INVALID_ARGUMENT`.
- La cuenta de servicio de Firebase solo se usa en el backend y nunca se
  versiona. Usa variables de entorno o el gestor de secretos del despliegue.
