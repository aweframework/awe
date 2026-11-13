---
id: idp-first-keycloak-entra
title: IdP-first con Keycloak y Microsoft Entra ID
sidebar_label: Guía Keycloak y Entra ID
---

# IdP-first con Keycloak y Microsoft Entra ID {#idp-first-with-keycloak-and-microsoft-entra-id}

Esta guía muestra cómo ejecutar una aplicación AWE con Keycloak, o con Microsoft Entra ID, como única forma de iniciar sesión.
Es el complemento práctico del documento de decisión [Modo de seguridad IdP-first](idp-first.md), que explica qué decide el
proveedor de identidad y qué conserva AWE. La autorización de pantallas y opciones sigue en AWE: el proveedor solo envía el
nombre de un rol y AWE lo asigna a un perfil.

:::info Qué necesitas
- AWE 5 o posterior (el claim de roles, el interruptor de inicio de sesión local y el cierre de sesión por canal trasero no están en AWE 4.x).
- Un **perfil** de AWE para cada rol que vayas a enviar. Los nombres de perfil son los almacenados en AWE (por ejemplo
  `administrator`); los roles del proveedor llevan un prefijo, como `role_administrator`.
- Una dirección HTTPS pública para la aplicación. Los ejemplos usan `https://my-app.example.com`.
:::

## Configuración común {#common-configuration}

Estas propiedades hacen que el proveedor sea la única vía de entrada, sea cual sea el proveedor. Las propiedades propias de cada proveedor
están en las secciones siguientes.

```properties title="IdP-first baseline"
awe.security.sso.enabled=true
# Turn local login off. A role that maps to an AWE profile is then required (fail closed)
awe.security.sso.local-login=false
# Roles of the provider that start with this prefix are AWE profiles: role_administrator -> administrator
awe.security.sso.filter-authority-prefix=role_
# Let the provider end AWE sessions
awe.security.sso.back-channel-logout.enabled=true
# Sin vía de entrada local: nadie puede iniciar sesión con una contraseña local (activa break-glass en la sección correspondiente)
awe.security.sso.break-glass-users=
```

| Propiedad | Qué hace aquí |
| --- | --- |
| `awe.security.sso.local-login=false` | Oculta el formulario local y rechaza `POST /action/login`, `POST /api/authenticate` y el formulario de inicio de sesión para todos los que no sean usuarios de emergencia. |
| `awe.security.sso.filter-authority-prefix` | Selecciona los roles que significan un perfil de AWE y elimina el prefijo. |
| `awe.security.sso.roles-claims` | Claims del token ID que llevan los roles (consulta [el claim de roles](#the-roles-claim)). |
| `awe.security.sso.auto-provision-user` | `true` (por defecto) crea el usuario de AWE en el primer inicio de sesión, con el perfil del rol. |
| `awe.security.sso.break-glass-users` | Nombres de usuario que aún pueden usar el formulario local (consulta [inicio de sesión local y administrador de emergencia](#local-login-and-the-break-glass-administrator)). |

## El claim de roles {#the-roles-claim}

AWE lee los roles del **token ID**, de los claims listados en `awe.security.sso.roles-claims` (por defecto
`realm_access.roles`). Se pueden listar varios claims; sus roles se unen. Cuando más de un rol se asigna, gana el primer
nombre, por orden alfabético, que exista como perfil de AWE; envía un único rol con prefijo por usuario cuando la elección importe.

| Proveedor | Roles | `awe.security.sso.roles-claims` |
| --- | --- | --- |
| Keycloak | Roles de realm | `realm_access.roles` (por defecto) |
| Keycloak | Roles de cliente | `resource_access.<client-id>.roles` |
| Keycloak | Ambos | `realm_access.roles,resource_access.<client-id>.roles` |
| Microsoft Entra ID | Roles de aplicación | `roles` |

Si ningún rol se asigna a un perfil de AWE, el inicio de sesión se **rechaza** con una página de error, tanto para usuarios nuevos como
existentes. No se usa ni el perfil almacenado en AWE ni `awe.application.default-role`. Volver a optar por el rol por defecto
(`awe.security.sso.require-role=false`) es posible, pero va en contra del propósito de IdP-first.

## Keycloak {#keycloak}

### Crear el cliente {#keycloak-client}

En el realm de tus usuarios, crea un cliente:

1. **Client ID**: por ejemplo `awe`. **Client authentication**: activada (un cliente confidencial). Deja **Standard flow** activado y
   los demás flujos desactivados.
2. **Valid redirect URIs**: `https://my-app.example.com/login/oauth2/code/keycloak`. El último segmento es el id de registro de
   Spring usado más abajo (`keycloak`). No uses comodines en producción.
3. **Valid post logout redirect URIs**: `https://my-app.example.com/*`, para que al cerrar sesión en AWE se vuelva a la aplicación.
4. Copia el **Client secret** de la pestaña **Credentials**. Inyéctalo desde una variable de entorno o un almacén de secretos, nunca
   desde un archivo del repositorio.

### Enviar los roles en el token ID {#keycloak-roles}

Crea los roles de realm con el prefijo (`role_administrator`, `role_general`...) y asígnalos a los usuarios o a grupos. Después, en el
ámbito dedicado del cliente (**Client scopes** > `awe-dedicated` > **Add mapper** > **By configuration**):

- Mapper **User Realm Role**: **Token Claim Name** `realm_access.roles`, **Multivalued** activado, **Add to ID token** activado.
- Para roles de cliente, un mapper **User Client Role** con **Token Claim Name** `resource_access.awe.roles` y **Add to ID
  token** activado.

Los roles que no están en el **token ID** son invisibles para AWE, aunque estén en el token de acceso.

Cada usuario necesita un correo electrónico, un nombre y un apellido, o Keycloak interrumpe el inicio de sesión con un paso de
"actualiza tu cuenta" que un inicio de sesión de AWE no puede completar.

### Cierre de sesión por canal trasero {#keycloak-back-channel-logout}

En los ajustes del cliente, **Logout settings**:

- **Backchannel logout URL**: `https://my-app.example.com/logout/connect/back-channel/keycloak`. El último segmento es el
  id de registro.
- **Backchannel logout session required**: activado, para que el token de cierre de sesión lleve el id de sesión (`sid`).

Keycloak debe poder abrir esa dirección desde donde se ejecuta. AWE termina la sesión con una llamada interna a sí mismo:
cuando la dirección pública no es accesible desde el servidor de AWE (un proxy, un balanceador de carga), define
`awe.security.sso.back-channel-logout.logout-uri` como se describe en [Single sign-on](authentication-sso.md#back-channel-logout).

### Configuración de AWE {#keycloak-configuration}

```properties title="Keycloak"
# Common baseline: see above
awe.security.sso.roles-claims=realm_access.roles

spring.security.oauth2.client.provider.keycloak.issuer-uri=https://keycloak.example.com/realms/my-realm
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
spring.security.oauth2.client.registration.keycloak.provider=keycloak
spring.security.oauth2.client.registration.keycloak.client-id=awe
spring.security.oauth2.client.registration.keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET}
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
```

Usa `https` en `issuer-uri`: AWE valida contra él el emisor, la audiencia, la caducidad y el nonce del token ID.

## Microsoft Entra ID {#microsoft-entra-id}

Entra ID se configura como un proveedor OpenID Connect normal. Es distinto de la opción
`spring-cloud-azure-starter-active-directory` descrita en [Single sign-on](authentication-sso.md): en IdP-first usa el
registro de cliente OIDC de abajo, para que el claim de roles y el resto de propiedades funcionen igual que con Keycloak.

### Registrar la aplicación {#entra-registration}

En el centro de administración de Entra, en **App registrations**:

1. **New registration**. **Supported account types**: un solo inquilino, salvo que realmente atiendas a varias organizaciones.
2. **Redirect URI** (plataforma **Web**): `https://my-app.example.com/login/oauth2/code/entra`.
3. En **Certificates & secrets**, crea un secreto de cliente y anota su fecha de caducidad: un secreto caducado detiene todos los inicios de sesión.
   Inyéctalo desde una variable de entorno o un almacén de secretos.
4. Anota el **Application (client) ID** y el **Directory (tenant) ID**.

### Enviar los roles {#entra-roles}

Usa **roles de aplicación**, no grupos: el claim `groups` lleva ids de objeto de grupo, no nombres.

1. En **App roles**, crea un rol por cada perfil de AWE. El **Value** es el nombre del rol con el prefijo, por ejemplo
   `role_administrator`. Tipos de miembro permitidos: **Users/Groups**.
2. En **Enterprise applications**, abre la aplicación, luego **Users and groups** y asigna usuarios o grupos a los roles.
   Activa **Assignment required** en las **Properties** de la aplicación para que los usuarios sin asignar no puedan iniciar sesión.

Entra ID envía los roles asignados al usuario en el claim `roles` del token ID.

### Configuración de AWE {#entra-configuration}

```properties title="Microsoft Entra ID"
# Common baseline: see above
awe.security.sso.roles-claims=roles

spring.security.oauth2.client.provider.entra.issuer-uri=https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0
spring.security.oauth2.client.provider.entra.user-name-attribute=preferred_username
spring.security.oauth2.client.registration.entra.provider=entra
spring.security.oauth2.client.registration.entra.client-id=${ENTRA_CLIENT_ID}
spring.security.oauth2.client.registration.entra.client-secret=${ENTRA_CLIENT_SECRET}
spring.security.oauth2.client.registration.entra.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.entra.scope=openid,profile,email
spring.security.oauth2.client.registration.entra.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
```

El nombre de usuario de AWE es el claim `preferred_username`, que en Entra ID es el nombre principal de usuario (normalmente la
dirección de correo). Crea los usuarios de AWE con ese nombre, o deja que el autoaprovisionamiento los cree.

### Cierre de sesión {#entra-logout}

Entra ID no admite el cierre de sesión OpenID Connect por **canal trasero**, solo por canal frontal. Solo con Entra ID,
una sesión cerrada por un administrador en Entra ID sigue abierta en AWE hasta que caduca o el usuario cierra sesión en AWE.
Mantén un tiempo de espera de sesión corto (consulta [Configuración de sesión](../session.md)) o coloca Keycloak delante de Entra ID como
intermediario de identidad: AWE habla entonces con Keycloak y obtiene el cierre de sesión por canal trasero, con Entra ID como fuente de identidad.

## Inicio de sesión local y administrador de emergencia {#local-login-and-the-break-glass-administrator}

Con `awe.security.sso.local-login=false` la pantalla de acceso solo muestra los botones del proveedor. Si el proveedor está caído
sigues necesitando una vía de entrada: lista uno o dos usuarios de AWE en `awe.security.sso.break-glass-users`. Inician sesión con sus
credenciales locales normales en `/screen/public/signin-local`; todos los demás se rechazan con la misma respuesta que una contraseña incorrecta.

- Da al usuario de emergencia una contraseña larga y única guardada en un almacén de secretos, y un perfil con solo el acceso necesario para reparar
  la conexión con el proveedor.
- Cada intento se registra a nivel `WARN`; genera alertas con la etiqueta `[break-glass]`.
- Deja la lista vacía (la base de arriba) si no quieres ninguna vía de entrada local.

Para activarlo, añade esto a la base, con el nombre de un usuario de AWE que reserves solo para emergencias:

```properties title="Activación de break-glass (no forma parte de la base)"
awe.security.sso.break-glass-users=admin
```

Los detalles están en [Single sign-on](authentication-sso.md#switching-off-local-login).

## Comprueba que funciona {#check-that-it-works}

1. Abre la aplicación: se te envía al proveedor y, tras iniciar sesión, de vuelta a la pantalla inicial.
2. Inicia sesión con un usuario cuyo rol se asigna a un perfil: el menú es el de ese perfil.
3. Inicia sesión con un usuario sin rol asignado: aparece la página de error "no tiene ningún rol permitido en esta aplicación" y no hay sesión.
4. Desactiva al usuario en AWE (no en el proveedor) e inicia sesión de nuevo: aparece la página "desactivado en la aplicación".
5. Con Keycloak, cierra todas las sesiones del usuario en la consola de administración: la siguiente petición en AWE va al flujo de inicio de sesión.
6. Abre `/screen/public/signin-local` como usuario normal: el inicio de sesión se rechaza como una contraseña incorrecta.

El repositorio ejecuta las mismas comprobaciones contra un Keycloak real en un contenedor, consulta
[cómo se prueba](#how-this-is-tested).

## Riesgos residuales {#residual-risks}

| Riesgo | Qué hacer |
| --- | --- |
| Una sesión abierta de AWE sobrevive a la revocación del usuario en el proveedor hasta que caduca. | Cierre de sesión por canal trasero (Keycloak) y un tiempo de espera de sesión corto. Entra ID no tiene cierre por canal trasero: confía en el tiempo de espera. |
| Un cambio de rol en el proveedor surte efecto en el siguiente inicio de sesión. | Mantén corto el tiempo de espera de sesión si los cambios de rol deben aplicarse rápido. |
| El token ID es la única fuente de roles. | Comprueba en el proveedor que los roles están en el token ID y revisa quién puede asignarlos. |
| La autenticación de dos factores de AWE no se ejecuta en la vía SSO. | Impón la MFA en el proveedor (flujos de autenticación de Keycloak, acceso condicional de Entra ID). |
| El usuario de emergencia es una cuenta local con contraseña. | Mantén la lista corta, usa un almacén de secretos y genera alertas con `[break-glass]`. |
| El secreto de cliente es una credencial en la configuración. | Inyéctalo desde el entorno o un almacén de secretos, rótalo antes de que caduque y no lo subas nunca al repositorio. |
| El cierre de sesión por canal trasero solo llega al nodo que tiene la sesión. | En un clúster, declara un `OidcSessionRegistry` compartido y asegúrate de que la llamada interna llega al nodo correcto, consulta [Single sign-on](authentication-sso.md#back-channel-logout). |
| La autorización de pantallas y opciones no se delega. | Nada que hacer: sigue en AWE. Revisa los perfiles y restricciones de AWE como siempre. |

La matriz de confianza completa está en [Modo de seguridad IdP-first](idp-first.md).

## Cómo se prueba {#how-this-is-tested}

`KeycloakIdpFirstTest` en `awe-tests/awe-boot` arranca un contenedor de Keycloak con un realm de prueba y AWE en un puerto aleatorio,
inicia sesión de usuarios con el flujo real de código de autorización y comprueba el inicio de sesión, el perfil tomado de los claims de roles, el
rechazo cuando ningún rol se asigna, el rechazo de un usuario desactivado en AWE y el cierre de sesión por canal trasero. Necesita Docker y se
omite sin él:

```bash
mvn -pl awe-framework/awe-testing,awe-tests/awe-boot -Pkeycloak -Dskip.frontend=true \
  -Dtest=KeycloakIdpFirstTest -Dsurefire.failIfNoSpecifiedTests=false test
```
