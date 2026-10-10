---
id: rest 
title: Módulo API Rest
sidebar_label: Módulo API Rest
---

¿Qué es una API REST? REST significa **Re**presentational **S**tate **T**ransfer (transferencia de estado representacional). (A veces se escribe "REST".) Se
basa en un protocolo de comunicaciones sin estado, cliente-servidor y almacenable en caché, y en prácticamente todos los casos se
utiliza el protocolo HTTP.

REST es un estilo de arquitectura para diseñar aplicaciones en red. La idea es que, en lugar de usar mecanismos
complejos como CORBA, RPC o SOAP para conectar máquinas, se utiliza HTTP simple para realizar llamadas entre máquinas.

Al igual que los servicios web, un servicio REST es:

- Independiente de la plataforma (no importa si el servidor es Unix, el cliente es un Mac, o cualquier otra cosa)
- Independiente del lenguaje (C# puede comunicarse con Java, etc.)
- Basado en estándares (se ejecuta sobre HTTP)
- Fácil de usar en presencia de cortafuegos

Para activar este módulo, sigue estos pasos:

- Añade las **dependencias awe-rest** al descriptor pom.xml.

```xml
<dependencies>
...
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-rest-spring-boot-starter</artifactId>
  </dependency>
...
</dependencies>
```

<img alt="AWE Rest" src={require('@docusaurus/useBaseUrl').default('img/AWE_Rest.png')} />

## **Propiedades de configuración de AWE Rest** {#awe-rest-configuration-properties}

Este módulo admite dos modos de autenticación REST:

- `local-jwt` (**por defecto**): AWE emite y valida JWT locales a través de `/api/authenticate`.
- `oauth2-resource-server` (opcional): AWE valida tokens de acceso OAuth2/OIDC emitidos externamente (por ejemplo Entra ID, Keycloak), y `/api/authenticate` **no** se utiliza.

### Propiedades de JWT local {#local-jwt-properties}

| Clave | Valor por defecto | Descripción |
|---|---|---|
| `awe.rest.api.auth.mode` | `local-jwt` | Modo de autenticación REST (`local-jwt` u `oauth2-resource-server`) |
| `awe.rest.api.jwt.authorization-header` | `Authorization` | Nombre de la cabecera de autenticación |
| `awe.rest.api.jwt.prefix` | `Bearer` | Prefijo del token JWT |
| `awe.rest.api.jwt.secret` | `${awe.security.master.key}` | Secreto JWT para la firma de tokens locales |
| `awe.rest.api.jwt.issuer` | `AWE ISSUER` | Emisor del JWT local |
| `awe.rest.api.jwt.expiration-time` | `60m` | Expiración del JWT local |

### Propiedades de OAuth2 Resource Server y Swagger UI {#oauth2-resource-server-and-swagger-ui-properties}

Para ver la lista completa de propiedades, sus valores por defecto y descripciones, consulta la documentación de [propiedades de AWE Rest](properties.md#awe-rest-properties).

### Ejemplo con Microsoft Entra ID {#microsoft-entra-id-example}

Usa dos registros de aplicación para las pruebas locales de Swagger UI:

1. **AWE REST API**: expone el ámbito (scope) protegido de la API y es la audiencia del servidor de recursos.
2. **AWE Swagger UI**: cliente SPA público utilizado por Swagger UI para ejecutar Authorization Code + PKCE.

#### Registrar la aplicación AWE REST API {#register-the-awe-rest-api-application}

En Azure Portal, abre **Microsoft Entra ID** → **App registrations** → **New registration**.
Crea el registro de la API y anota estos valores:

- **Application (client) ID**: se usa como `<api-application-client-id>`.
- **Directory (tenant) ID**: se usa como `<tenant-id>`.

Después configura el registro de la API:

1. Abre **Expose an API**.
2. Establece el **Application ID URI**. El valor por defecto `api://<api-application-client-id>` es válido.
3. Añade un ámbito (scope) habilitado, por ejemplo `access`. El ámbito completo pasa a ser `api://<api-application-client-id>/access`.
4. En el manifiesto de la aplicación, establece `requestedAccessTokenVersion` a `2` para que los tokens de acceso usen el emisor v2 de Entra ID.

#### Registrar la aplicación cliente de Swagger UI {#register-the-swagger-ui-client-application}

Crea un segundo registro de aplicación para Swagger UI, por ejemplo **AWE Swagger UI Dev**.
En **Authentication**, añade una plataforma **Single-page application** con esta URI de redirección:

```text
http://localhost:8080/swagger-ui/oauth2-redirect.html
```

Si tu servidor local usa otro puerto o ruta de contexto, ajusta la URI en consecuencia.
No configures un secreto de cliente para Swagger UI, y no mantengas la misma URI de redirección bajo la plataforma **Web**, porque el canje de tokens desde el navegador debe usar el tipo de cliente SPA.
Las casillas de concesión implícita (implicit grant) como **Access tokens** o **ID tokens** no son necesarias para Authorization Code + PKCE.

#### Conceder a Swagger UI permiso sobre la API {#grant-swagger-ui-permission-to-the-api}

En el registro **AWE Swagger UI**:

1. Abre **API permissions** → **Add a permission** → **My APIs**.
2. Selecciona el registro **AWE REST API**.
3. Selecciona el ámbito `access`.
4. Concede el consentimiento de administrador si la política de tu tenant lo requiere.

Ejemplo de configuración:

```properties
awe.rest.api.auth.mode=oauth2-resource-server

# Match the token issuer exactly. Entra ID v2 tokens end with /v2.0.
awe.rest.api.oauth2-resource-server.jwt.issuer-uri=https://login.microsoftonline.com/<tenant-id>/v2.0

# Match the access token aud claim exactly. Entra ID may emit the bare API client id,
# even when the requested scope uses api://<api-application-client-id>/access.
awe.rest.api.oauth2-resource-server.jwt.audiences[0]=<api-application-client-id>

awe.rest.api.openapi.oauth2.authorization-url=https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/authorize
awe.rest.api.openapi.oauth2.token-url=https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token
awe.rest.api.openapi.oauth2.client-id=<swagger-ui-application-client-id>
awe.rest.api.openapi.oauth2.use-pkce=true

# Escape ':' in .properties keys and keep the full scope key inside brackets.
awe.rest.api.openapi.oauth2.scopes.[api\://<api-application-client-id>/access]=Access AWE REST API
```

Si AWE devuelve `The iss claim is not valid`, decodifica el token de acceso y verifica que `issuer-uri` coincida con el claim `iss` del token. Si AWE devuelve `Token audience is not accepted`, configura `awe.rest.api.oauth2-resource-server.jwt.audiences` con el claim `aud` exacto del token.

## **Servicios** {#services}

La API REST de AWE proporciona `AUTHENTICATE`, `QUERY` y `MAINTAIN`, agrupados en `Protected API` (requiere autenticación) y
`Public API` (no requiere autenticación).

El comportamiento de la autenticación depende del modo configurado:

- **local-jwt**: autentícate contra `/api/authenticate` y después usa el JWT devuelto en `Authorization: Bearer <token>`.
- **oauth2-resource-server**: obtén un token de acceso de tu proveedor de identidad y llama directamente a AWE con `Authorization: Bearer <access_token>`.

> `/api/authenticate` es exclusivo de local-jwt y no es un endpoint de tokens OAuth2.
> El modo OAuth2 Resource Server no aprovisiona ni sincroniza usuarios `ope` locales, y no añade autorización granular de query/maintain ni un modelo de permisos REST de lectura/escritura.

:::info La documentación *swagger* completa de los servicios rest de awe está
disponible [aquí](http://demo.aweframework.com/swagger-ui.html).
:::

| Servicio | Método |  Ruta | Requiere autenticación  | Descripción                                        |
| ----------- | -----| -------|------------------------|----------------------------------------------------|
| [authenticate](#authenticate-service) | POST | `/api/authenticate` | false | Se usa para autenticarse. Proporciona un token JWT para establecer como cabecera http (valor por defecto `Authorization`) en los servicios protegidos |
| [data](#query-service) | POST | `/api/data/{queryId}` | true | Se usa para lanzar consultas de la aplicación. En modo `local-jwt` autentícate primero con `/api/authenticate`; en modo `oauth2-resource-server` envía un token de acceso de un proveedor externo. |
| [maintain](#maintain-service) | POST | `/api/maintain/{maintainId}` | true | Se usa para lanzar maintains de la aplicación. En modo `local-jwt` autentícate primero con `/api/authenticate`; en modo `oauth2-resource-server` envía un token de acceso de un proveedor externo. |

### **Servicio authenticate** {#authenticate-service}

El servicio **authenticate** tiene las siguientes **entradas**:

| Entrada | Uso | Tipo | Descripción     |  Valor |
| ----------- | ---------|------------------------|-------------------------------------|---------------|
| username | **Obligatorio** | Parámetro de consulta | Es el nombre del usuario a autenticar |  **Ej.:** `test` |
| password | **Obligatorio** | Parámetro de consulta | Es la contraseña del usuario a autenticar |  **Ej.:** `test` |

El servicio **authenticate** tiene las siguientes **salidas**:

| Salida | Tipo | Descripción |
| -----------| ------ | ---------|
| username | String | Es el nombre del usuario para el que se ha generado el token. |
| token | String | Es el token jwt. Se usa para el proceso de autenticación. **Nota:** Si quieres llamar a la API rest `/api/data` o `/api/maintain`, tienes que enviar este parámetro como cabecera http en la petición |
| issuer | String | Es el emisor del jwt |
| expiresAt | DateTime | Hora de expiración del token jwt |

> **Nota:** La salida está en formato `JSON`

Este es un ejemplo de salida json

```json
{
  "expiresAt": "2021-04-26T16:16:18.000+00:00",
  "issuer": "AWE issuer",
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0IiwiaXNzIjoiQVdFIElTU1VFUiIsImV4cCI6MT",
  "username": "foo"
}
```

### **Servicio query** {#query-service}

El servicio **data** tiene las siguientes **entradas**:

| Entrada | Uso | Tipo | Descripción     |  Valor |
| ----------- | ---------|------------------------|-------------------------------------|---------------|
| queryId | **Obligatorio** | Parámetro de consulta de URI | Parámetro de URI para establecer el nombre de la consulta en la petición |  Ej.: `UsrLst` |
| RequestParameter | **Opcional** | Objeto Json (cuerpo) | Lista de parámetros de la consulta en formato JSON | Ej.: `{"parameters": {"parName1": "value1","parName2": "value2","parName3": ["valueList1","valueList2","valueList3"]}}` |

El servicio **data** tiene las siguientes **salidas**:

| Salida | Tipo | Descripción |
| ------- | ---- | ---------|
| type | String | Resultado de la operación `(ok, info, warning, error)`  |
| title | String | Título de la respuesta  |
| message | String | Mensaje de la respuesta  |
| dataList | Objeto Json | Datos resultantes del servicio de consulta |

> **Nota:** La salida está en formato `JSON`

### **Servicio maintain** {#maintain-service}

El servicio **maintain** como POST tiene las siguientes **entradas**:

| Entrada | Uso | Tipo | Descripción     |  Valor |
| ----------- | ---------|------------------------|-------------------------------------|---------------|
| maintainId | **Obligatorio** | Parámetro de consulta de URI | Parámetro de URI para establecer el nombre del maintain en la petición |  Ej.: `UsrDel` |
| RequestParameter | **Opcional** | Objeto Json (cuerpo) | Lista de parámetros de la consulta en formato JSON | Ej.: `{"parameters": {"IdeOpe": 2} }`|

El servicio **maintain** tiene las siguientes **salidas**:

| Salida | Tipo | Descripción |
| ------- | ---- | ---------|
| type | String | Resultado de la operación `(ok, info, warning, error)`  |
| title | String | Título de la respuesta  |
| message | String | Mensaje de la respuesta  |
| resultDetails | Objeto Json | Detalles del resultado del maintain |

> **Nota:** La salida está en formato `JSON`

## **Ejemplos de cliente de la API Rest** {#client-api-rest-examples}

### Ejemplos rápidos de OAuth2 Resource Server {#oauth2-resource-server-quick-examples}

#### Flujo de usuario delegado (Swagger UI) {#delegated-user-flow-swagger-ui}

1. Configura los metadatos OAuth2 de Swagger UI (`authorization-url`, `token-url`, `scopes` y el `client-id` público).
2. Abre `/swagger-ui/`.
3. Usa **Authorize** y completa el inicio de sesión Authorization Code + PKCE en tu proveedor.
4. Invoca los endpoints protegidos `/api/data/**` o `/api/maintain/**`.

#### Aplicación web Spring Boot con inicio de sesión de usuario (Authorization Code) {#spring-boot-web-app-with-user-login-authorization-code}

Usa este flujo cuando una aplicación web Spring Boot llama a AWE REST en nombre del usuario autenticado.
La aplicación Spring Boot es un cliente **Web** confidencial y usa un secreto de cliente.

Configuración del proveedor de identidad:

| Aplicación | Configuración requerida |
|---|---|
| AWE REST API | Expón un ámbito delegado, por ejemplo `api://<api-application-client-id>/access`. |
| Aplicación web Spring Boot | Añade una URI de redirección **Web** como `http://localhost:9090/login/oauth2/code/entra`, crea un secreto de cliente y concede el ámbito delegado de la API. |

Ejemplo de configuración del cliente Spring Boot:

```properties
spring.security.oauth2.client.registration.entra.client-id=<spring-boot-client-id>
spring.security.oauth2.client.registration.entra.client-secret=<spring-boot-client-secret>
spring.security.oauth2.client.registration.entra.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.entra.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.entra.scope=openid,profile,email,api://<api-application-client-id>/access

spring.security.oauth2.client.provider.entra.issuer-uri=https://login.microsoftonline.com/<tenant-id>/v2.0
```

Ejemplo de controlador usando el token de acceso del usuario autorizado:

```java
@GetMapping("/call/simple-get-all")
@ResponseBody
String callSimpleGetAll(@RegisteredOAuth2AuthorizedClient("entra") OAuth2AuthorizedClient authorizedClient) {
  return webClient.post()
    .uri("http://localhost:8080/api/data/SimpleGetAll")
    .contentType(MediaType.APPLICATION_JSON)
    .headers(headers -> headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue()))
    .bodyValue(Map.of("parameters", Map.of()))
    .retrieve()
    .bodyToMono(String.class)
    .block();
}
```

#### Servicio backend sin inicio de sesión de usuario (Client Credentials) {#backend-service-without-user-login-client-credentials}

Usa este flujo cuando un servicio backend llama a AWE REST como la propia aplicación, sin inicio de sesión en navegador ni contexto de usuario.
El proveedor de identidad emite un token de aplicación una vez concedido el consentimiento de administrador.

Configuración del proveedor de identidad:

| Aplicación | Configuración requerida |
|---|---|
| AWE REST API | Define un rol de aplicación, por ejemplo `access_as_application`, con **Applications** como tipo de miembro permitido. |
| Servicio backend | Crea un secreto de cliente o un certificado, asigna el rol de aplicación de la API en **Application permissions** y concede el consentimiento de administrador. |

Ejemplo de configuración del cliente Spring Boot:

```properties
spring.security.oauth2.client.registration.entra-client.client-id=<backend-client-id>
spring.security.oauth2.client.registration.entra-client.client-secret=<backend-client-secret>
spring.security.oauth2.client.registration.entra-client.authorization-grant-type=client_credentials
spring.security.oauth2.client.registration.entra-client.scope=api://<api-application-client-id>/.default

spring.security.oauth2.client.provider.entra-client.token-uri=https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token
```

Ejemplo de obtención programática del token:

```java
@Bean
OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                      OAuth2AuthorizedClientService clients) {
  OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
    .clientCredentials()
    .build();
  AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
    new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
  manager.setAuthorizedClientProvider(provider);
  return manager;
}

@GetMapping("/call/simple-get-all-client")
@ResponseBody
String callSimpleGetAllClient() {
  OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId("entra-client")
    .principal("awe-rest-client")
    .build();
  OAuth2AuthorizedClient client = authorizedClientManager.authorize(request);

  return webClient.post()
    .uri("http://localhost:8080/api/data/SimpleGetAll")
    .contentType(MediaType.APPLICATION_JSON)
    .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
    .bodyValue(Map.of("parameters", Map.of()))
    .retrieve()
    .bodyToMono(String.class)
    .block();
}
```

Los tokens de aplicación normalmente contienen `roles` en lugar de `scp` y no contienen claims de usuario como `preferred_username`. AWE REST construye un principal de cliente sintético a partir de claims como `azp`, `appid`, `client_id` o `sub`.

* **Ejemplo de cliente de login (modo local-jwt)**

```java
// Authenticate
@Test
public void authenticateUser() {
    // Init rest template
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    // Build authenticate request
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost:8080/api/authenticate"))
    .queryParam("username","test")
    .queryParam("password","test");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<LoginResponse> response = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.POST, 
            entity, 
            LoginResponse.class);
    // LoginResponse has token info
    ...
}
```

* **Ejemplo de cliente de datos**

```java
// Data without parameters
@Test
public void protectedQueryAuthorized() {
    // Init rest template
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    String queryId = "query";
    
    //Authenticate user (call /api/authenticate to get jwt token)
    headers.add("Authorization", "Bearer " + jwtToken);

    HttpEntity<String> entity = new HttpEntity<>(headers);
    
    ResponseEntity<AweRestResponse> response = restTemplate.exchange("http://localhost:8080/api/data/" + queryId,
        HttpMethod.POST,
        entity,
        AweRestResponse.class);
        // AweRestResponse has response info
        ...
}
```

```java
// Data with parameters
@Test
public void protectedQueryParametersAuthorized() {
    // Init rest template
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    String queryId = "query";
    
    //Authenticate user (call /api/authenticate to get jwt token)
    headers.add("Authorization", "Bearer " + jwtToken);

    // Build parameters request
    headers.setContentType(MediaType.APPLICATION_JSON);
    RequestParameter parameters = new RequestParameter();
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("param1", 1);
    paramMap.put("param2", "value2");
    paramMap.put("param3", Arrays.asList("value1", "value2"));
    parameters.setParameters(paramMap);

    HttpEntity<RequestParameter> entity = new HttpEntity<>(parameters, headers);     
    ResponseEntity<AweRestResponse> response = restTemplate.exchange("http://localhost:8080/api/data/" + queryId,
        HttpMethod.POST,
        entity,
        AweRestResponse.class);
        // AweRestResponse has response info
        ...
}
```

* **Ejemplo de cliente de maintain**

```java
// Maintain without parameters
@Test
public void protectedMaintainAuthorized() {
    // Init rest template
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    String maintainId = "MAINTAIN";

    //Authenticate user (call /api/authenticate to get jwt token)
    headers.add("Authorization", "Bearer " + jwtToken);

    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<AweRestResponse> response = restTemplate.exchange("http://localhost:8080/api/maintain/" + maintainId,
    HttpMethod.POST,
    entity,
    AweRestResponse.class);
    // AweRestResponse has response of maintain result
    ...
}
```

```java
// Maintain with parameters
@Test
public void protectedMaintainParametersAuthorized() {
    // Init rest template
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    String maintainId = "MAINTAIN";

    // Build parameters request
    headers.setContentType(MediaType.APPLICATION_JSON);
    RequestParameter parameters = new RequestParameter();
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("userId", 1);
    parameters.setParameters(paramMap);

    //Authenticate user (call /api/authenticate to get jwt token)
    headers.add("Authorization", "Bearer " + jwtToken);

    HttpEntity<RequestParameter> entity = new HttpEntity<>(parameters, headers);
    ResponseEntity<AweRestResponse> response = restTemplate.exchange("http://localhost:8080/api/maintain/" + maintainId,
    HttpMethod.POST,
    entity,
    AweRestResponse.class);
    // AweRestResponse has response of maintain result
    ...
}
```
