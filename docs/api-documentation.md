# Documentación de la API (Swagger / OpenAPI 3)

Swagger UI del monolito modular completo: cada ruta, parámetro y cuerpo de
petición/respuesta sale de los propios controladores, sin listas escritas a mano
que puedan desincronizarse.

## URLs

| Recurso | Ruta |
|---------|------|
| Swagger UI | `/swagger-ui.html` (redirige a `/swagger-ui/index.html`) |
| Documento OpenAPI (JSON) | `/v3/api-docs` |
| Documento OpenAPI (YAML) | `/v3/api-docs.yaml` |

Las tres son públicas (`SecurityConfig`), para que la UI pueda cargar el
documento; el resto de la API sigue exigiendo el token de sesión. En `dev` basta
`./gradlew bootRun` y abrir <http://localhost:8080/swagger-ui.html>.

## Cómo se genera

- `springdoc-openapi-starter-webmvc-ui:3.1.1` (misma línea de Spring Boot 4.1.x
  que el proyecto) y las propiedades `springdoc.api-docs.path` /
  `springdoc.swagger-ui.path` en `application.yml`.
- `OpenApiConfig` (`shared/presentation`): título, versión, descripción, esquema
  `bearerAuth` y el requisito global de seguridad. Registra además
  `SpringDocUtils.addAnnotationsToIgnore(AuthenticationPrincipal::class.java)`:
  el principal lo inyecta Spring Security, nunca viaja en la petición, así que no
  debe documentarse como parámetro.
- Cada controlador lleva un `@Tag` que agrupa sus operaciones en la UI. Los
  nombres no se repiten salvo `Conversations` (misma descripción → springdoc lo
  deduplica en una sola entrada).

## Autenticación en la UI

- El token es el de sesión devuelto por `POST /api/v1/auth/login` o
  `POST /api/v1/auth/register` (opaco, no JWT): se pega en el botón **Authorize**
  y Swagger lo envía como `Authorization: Bearer <token>`.
- El requisito es global (espeja el `anyRequest().authenticated()` de
  `SecurityConfig`); los cinco flujos públicos lo anulan con
  `@SecurityRequirements`: registro, verificación de correo, login y las dos
  operaciones de restablecimiento de contraseña. `POST /api/v1/auth/logout` sí
  requiere sesión.
- El namespace `/api/v1/admin/**` requiere además rol MODERADOR o
  ADMINISTRADOR (la UI no lo distingue: lo aplica el filtro de seguridad).

## Entornos

`prod` apaga la documentación salvo que el despliegue lo pida explícitamente:

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: ${SWAGGER_ENABLED:false}
  swagger-ui:
    enabled: ${SWAGGER_ENABLED:false}
```

## Grupos (tags)

| Grupo | Rutas |
|-------|-------|
| `Auth`, `Me Identity` | `/api/v1/auth/**`, `/api/v1/users/me/**` |
| `Profile` | `/api/v1/profile/**` |
| `Host Accommodation` | `/api/v1/host/properties/**`, `/api/v1/host/units/**`, `/api/v1/host/publications/**` |
| `Publications` | `/api/v1/publications/**` |
| `Reservations`, `Host Reservations` | `/api/v1/reservations/**`, `/api/v1/host/reservations/**` |
| `Search`, `Favorites` | `/api/v1/search/**`, `/api/v1/favorites/**` |
| `Conversations`, `Messaging Safety`, `Notifications` | `/api/v1/conversations/**`, bloques, `/api/v1/notifications/**` |
| `Community`, `Host Community` | `/api/v1/community/**`, `/api/v1/host/community/**` |
| `Cases` | `/api/v1/cases` (seguimiento del usuario que reportó) |
| `Reviews`, `Reputation` | `/api/v1/reviews/**`, `/api/v1/users/{userId}/reputation` |
| `Admin Identity`, `Admin Publications`, `Admin Cases`, `Admin Catalogs`, `Admin Benefit rules` | `/api/v1/admin/**` |

## Verificación

`OpenApiDocumentationIT` (contexto completo + PostgreSQL efímero) contrasta el
documento generado con el inventario real: **98 operaciones**, los **22 grupos**,
el esquema `bearerAuth`, los cinco flujos públicos como anónimos, la ausencia del
principal como parámetro y que `/v3/api-docs`, `/v3/api-docs.yaml` y
`/swagger-ui/index.html` respondan sin token. Al añadir un endpoint nuevo el test
falla hasta declararlo en su lista esperada.
