---
id: idp-first
title: Modo de seguridad IdP-first
sidebar_label: Modo IdP-first
---

# Modo de seguridad IdP-first {#idp-first-security-mode}

Esta página es el documento de decisión para ejecutar una aplicación AWE con un proveedor de identidad (IdP) externo como
única forma de iniciar sesión. Está escrita para los equipos de aplicación que configuran AWE y para los mantenedores que lo evolucionan.
Explica qué decide el IdP, qué conserva AWE, quién puede bloquear a un usuario, y qué partes existen hoy y cuáles están
planificadas.

:::info Guía de lectura
Todo lo que no esté marcado como **Planificado** describe el comportamiento del código actual. Los elementos marcados como **Planificado en AWE 5**
son decisiones ya tomadas pero que todavía no están disponibles; no tienen fecha de entrega y sus nombres de propiedad pueden
cambiar. Para configurar el propio SSO, empieza por [Single sign-on](authentication-sso.md).
:::

## Qué significa "IdP-first" {#what-idp-first-means}

En un despliegue IdP-first el proveedor de identidad (por ejemplo Keycloak, o Microsoft Entra ID) es la única
autoridad sobre **quién es el usuario** y **si la cuenta puede iniciar sesión**. AWE confía en el proveedor para esas dos
cuestiones y mantiene la responsabilidad sobre **qué puede hacer el usuario dentro de la aplicación**.

IdP-first es un perfil de despliegue, no una reescritura: la misma aplicación AWE puede ejecutarse con usuarios locales (base de datos,
LDAP, en memoria o proveedores personalizados) o con un IdP, y el perfil se selecciona por configuración.

## Límites de confianza {#trust-boundaries}

| Decisión | Responsable | Dónde ocurre en el código |
| --- | --- | --- |
| Autenticación (contraseña, MFA, federación, bloqueo por fuerza bruta) | IdP | Flujo de código de autorización OIDC gestionado por `oauth2Login` de Spring Security, configurado en `AweWebSecurityConfig` |
| Estado de la cuenta en el proveedor (deshabilitada, bloqueada, eliminada) | IdP | El proveedor se niega a autenticar al usuario |
| Claim de rol enviado a la aplicación | IdP | `AweWebSecurityConfig.realmRolesAuthoritiesConverter` lee `realm_access.roles` |
| A qué perfil de AWE corresponde un rol | AWE | `AweUserDetailService.mapGrantedAuthorityProfile` y la sincronización de roles de `AccessService` |
| Autorización de pantallas y opciones (perfiles, restricciones) | **AWE** | `MenuService` y `ScreenRestrictionGenerator`, dirigidos por el perfil y las restricciones del usuario cargadas al iniciar sesión |
| Tema, idioma y pantalla inicial | AWE | `AweUserDetailService.getAweUserDetails`, almacenados en la sesión por `AweSessionDetails.storeUserDetails` |
| Estado de la cuenta en AWE (habilitada, bloqueada) | AWE | Registro de usuario, leído por `AweUserDetailService` |

:::important La autorización permanece en AWE en todos los modos
La autorización de pantallas y opciones **nunca** se delega en el proveedor de identidad, sea cual sea el modo configurado. El
proveedor solo proporciona el nombre de un rol; AWE lo resuelve a un perfil y luego aplica las restricciones de ese perfil
(y cualquier restricción almacenada para el usuario individual) al menú, las pantallas y sus opciones. Un producto mantiene
su propio modelo de perfiles y restricciones; IdP-first solo cambia la forma en que el usuario entra.
:::

### Lo que no se le pide al proveedor {#what-the-provider-is-not-asked-to-do}

- No conoce las pantallas, opciones ni restricciones de AWE, y no debe configurarse para modelarlas.
- No almacena las preferencias de AWE del usuario (tema, idioma, pantalla inicial). Estas permanecen en el registro de usuario
  de AWE.
- La autenticación de dos factores mediante las pantallas TOTP propias de AWE forma parte del flujo de inicio de sesión **local**
  (`AccessService.login`). El flujo SSO (`AccessService.onAuthenticationSuccess`) va directamente a la pantalla de
  inicio, por lo que en IdP-first el segundo factor debe imponerlo el proveedor.

## Matriz de confianza {#trust-matrix}

### Quién puede bloquear a un usuario {#who-can-block-a-user}

Ambas partes pueden bloquear a un usuario, según se decida:

| Bloqueado en | Efecto | Notas |
| --- | --- | --- |
| IdP | El usuario no puede autenticarse. | Las sesiones de AWE existentes no se tocan hasta que terminan (ver más abajo). |
| AWE (indicadores `enabled` / bloqueado en el registro de usuario) | El usuario es rechazado tras un inicio de sesión correcto en el IdP. | Comportamiento actual desde AWE 5.0.0: el inicio de sesión SSO comprueba ambos indicadores y muestra una página de error; no se crea ninguna sesión. AWE 4.x y anteriores no los comprobaban en la vía SSO, por lo que allí un usuario deshabilitado o bloqueado en AWE todavía podía iniciar sesión a través del proveedor: bloquea también a esos usuarios en el proveedor. |

Bloquea a un usuario en el proveedor cuando deba perder el acceso en todas partes; bloquéalo en AWE cuando deba perder
el acceso solo a esta aplicación.

### Duración de la sesión y revocación en el IdP {#session-lifetime-and-idp-revocation}

Tras iniciar sesión, la sesión de AWE es una sesión HTTP normal (consulta la [configuración de sesión](../session.md)). El perfil del usuario,
las restricciones, el idioma, el tema y la pantalla inicial se cargan en ella al iniciar sesión (`AweSessionDetails`). AWE no
vuelve a llamar al proveedor mientras la sesión esté viva, por lo que:

- Deshabilitar o eliminar al usuario en el IdP **no** termina una sesión de AWE abierta; termina cuando caduca o cuando
  el usuario cierra sesión.
- Un cambio de rol o de perfil solo surte efecto en el siguiente inicio de sesión.
- Mantén el tiempo de espera de sesión de la aplicación lo bastante corto para cumplir tus requisitos de revocación.

**Planificado en AWE 5:** cierre de sesión por canal trasero (back-channel logout) de OIDC, para que el proveedor pueda terminar las sesiones de AWE cuando se revoque a un usuario o
cierre sesión en otro lugar.

### Cierre de sesión {#logout}

Hoy el cierre de sesión es **iniciado por la aplicación (RP-initiated)**: `/action/logout` borra la sesión de AWE y, cuando el SSO está habilitado, redirige al
navegador al endpoint de fin de sesión del proveedor mediante `OidcClientInitiatedLogoutSuccessHandler`, que termina también la
sesión del proveedor. Esto requiere que el navegador complete la redirección; no ocurre nada en AWE si el usuario
cierra sesión en el proveedor por otro canal. El cierre de sesión por canal trasero es el complemento planificado.

### Mapeo de roles {#role-mapping}

- Las autoridades del proveedor se filtran con `awe.security.sso.filter-authority-prefix`: solo se consideran las autoridades que
  empiezan por el prefijo y este se elimina para obtener el nombre del perfil de AWE. Con un prefijo vacío, se usa la
  autoridad completa.
- Gana la **primera** autoridad coincidente. Las autoridades convertidas se recopilan en un conjunto, por lo que cuando un usuario
  lleva varios roles coincidentes no está garantizado que la elección sea estable. Envía exactamente un rol con el
  prefijo por usuario hasta que esté disponible la selección determinista (planificada, ver más abajo).
- Un rol que no existe como perfil de AWE se ignora para los usuarios existentes (se mantiene su perfil) y se sustituye
  por el rol por defecto para los usuarios recién aprovisionados.
- El conversor por defecto solo entiende el claim `realm_access.roles` de Keycloak. Entra ID envía `roles` (roles de
  aplicación) o `groups`, que no se leen por defecto; hoy deben mapearse en el proveedor (intermediación de Keycloak,
  consulta [Single sign-on](authentication-sso.md#mappers)) o sobrescribiendo el
  bean `realmRolesAuthoritiesConverter` en la aplicación.

### Aprovisionamiento y rol por defecto {#provisioning-and-the-default-role}

Los nombres de propiedad siguientes son los de `SecurityConfigProperties.Sso` y `BaseConfigProperties`.

| Situación | Comportamiento hoy |
| --- | --- |
| Usuario desconocido, `awe.security.sso.auto-provision-user=true` (por defecto) | Se crea un registro de usuario con el rol mapeado, o con `awe.application.default-role` (por defecto `operator`) cuando ningún rol se mapea. |
| Usuario desconocido, `auto-provision-user=false` | No se crea ningún registro, pero el inicio de sesión continúa con una sesión construida a partir de los claims del token y el mismo rol de respaldo. Desactivar este indicador no deniega por sí solo a los usuarios desconocidos. |
| Usuario existente, el proveedor no envía ningún rol utilizable | Se mantiene el perfil almacenado, salvo que `awe.security.sso.overwrite-profile-with-default-role=true`. |
| Usuario nuevo, el proveedor no envía ningún rol utilizable | Recurre a `awe.application.default-role`. |

Los usuarios nuevos toman su nombre de inicio de sesión del claim `preferred_username`, mientras que los usuarios existentes se buscan con
`awe.security.sso.user-name-attribute` (por defecto `preferred_username`). Mantén ambos alineados.

**Planificado en AWE 5:** en el modo IdP-first un inicio de sesión sin ningún rol mapeable se **deniega** (fallo cerrado). El rol por defecto
de respaldo pasa a ser una opción explícita, de modo que nunca pueda conceder acceso por accidente.

### Registros multiinquilino {#multi-tenant-registrations}

Con `awe.security.sso.multitenant.enabled=true`, `MultiTenantFilter` deriva el inquilino de la primera etiqueta del
nombre de host y `MultiTenantOAuth2Config` lo mapea a un registro OAuth2.

- Sin `tenant-mappings`, se acepta **cualquier** subdominio como inquilino, y un subdominio sin registro
  coincidente recurre al registro por defecto (`MultiTenantOAuth2AuthenticationEntryPoint`,
  `OAuth2UrlService`).
- Con `awe.security.sso.multitenant.tenant-mappings` explícito, solo los subdominios listados son inquilinos válidos; cualquier
  otro nombre de host usa el inquilino por defecto.
- Los usuarios de AWE se identifican únicamente por el nombre de usuario. El mismo nombre de usuario emitido por los proveedores de dos inquilinos distintos
  se resuelve en el **mismo** usuario de AWE.

Recomendación: en los despliegues IdP-first declara siempre mapeos de inquilino explícitos, y asegúrate de que los nombres de usuario sean
únicos entre los proveedores de todos los inquilinos.

## Configuración: hoy y planificada {#configuration-today-and-planned}

### Disponible hoy {#available-today}

```properties title="Minimal IdP-first baseline (current behaviour)"
awe.security.sso.enabled=true
awe.security.sso.auto-launch=true
awe.security.sso.filter-authority-prefix=role_
# Do not store unknown users in AWE. This does NOT deny them: an unknown user still signs in with a session
# built from the token claims and the default role (see Provisioning). Deny unknown users in the provider.
awe.security.sso.auto-provision-user=false
# Keep profiles assigned in AWE when the provider sends no role
awe.security.sso.overwrite-profile-with-default-role=false
```

:::warning Auto-launch no es un interruptor del inicio de sesión local
`awe.security.sso.auto-launch=true` solo omite la pantalla de inicio de sesión nativa. El endpoint de inicio de sesión local
(`POST /action/login`, `JsonAuthenticationFilter`) y el proveedor de autenticación configurado
(`awe.security.auth-mode`: base de datos, LDAP, en memoria o personalizado) permanecen registrados y activos mientras el SSO está habilitado.
Hoy, una cuenta que tiene contraseña local todavía puede iniciar sesión sin el proveedor.
:::

Hasta que exista el interruptor planificado, reduce la vía local no aprovisionando contraseñas locales para los usuarios SSO y
restringiendo el acceso de red a `/action/login` donde tu infraestructura lo permita.

### Planificado en AWE 5 {#planned-in-awe-5}

| Elemento | Intención |
| --- | --- |
| Interruptor del inicio de sesión local | Una propiedad (nombre propuesto `awe.security.sso.local-login`, por defecto `true` para preservar el comportamiento actual) que desactiva la vía de inicio de sesión local cuando el SSO está activo. |
| Administrador de emergencia (break-glass) | Un administrador local opcional que todavía pueda iniciar sesión cuando la vía local esté desactivada; desactivado por defecto. |
| Cierre de sesión por canal trasero | Cierre de sesión por canal trasero de OIDC más un registro de sesiones, para que la revocación del lado del proveedor termine las sesiones de AWE. |
| Claim de roles configurable | Lectura de los roles desde un claim configurable para que Entra ID funcione directamente, además de la intermediación de Keycloak. Incluye selección determinista de perfil cuando coinciden varios roles. |
| Mapeo de roles con fallo cerrado | En IdP-first, denegar el inicio de sesión cuando ningún rol se mapee; el rol por defecto de respaldo pasa a ser opcional. |
| Prueba de integración con Keycloak | Una prueba automatizada de extremo a extremo contra un Keycloak real (inicio de sesión, mapeo de perfil, cierre de sesión por canal trasero, usuario deshabilitado). |

## Riesgos residuales y recomendaciones {#residual-risks-and-recommendations}

| Riesgo | Recomendación |
| --- | --- |
| Las sesiones de AWE abiertas sobreviven a la revocación en el IdP hasta que caducan. | Usa un tiempo de espera de sesión corto; adopta el cierre de sesión por canal trasero cuando esté disponible. |
| El inicio de sesión local permanece activo bajo SSO. | No mantengas contraseñas locales para los usuarios SSO; adopta el interruptor de inicio de sesión local cuando esté disponible y trata al administrador de emergencia como una excepción monitorizada. |
| El rol por defecto puede conceder acceso a usuarios sin rol. | Mantén `awe.application.default-role` **sin privilegios** (nunca un perfil administrativo); prefiere el mapeo con fallo cerrado cuando esté disponible. |
| Mapeo ambiguo cuando varios roles llevan el prefijo. | Envía un único rol con prefijo por usuario; no dependas del orden. |
| Secretos de cliente en archivos de configuración. | Inyéctalos desde variables de entorno o un almacén de secretos; si usas propiedades cifradas `ENC(...)`, sustituye el valor por defecto de `awe.security.master-key`. Nunca subas secretos al repositorio. |
| Proveedor incorrecto o suplantado. | Define `issuer-uri` explícitamente y sobre HTTPS; Spring Security valida el emisor, la audiencia, la caducidad y el nonce del token ID. Para Entra ID prefiere un registro de aplicación de un solo inquilino. Mantén los URI de redirección exactos (sin comodines en producción). |
| Resolución de inquilino abierta. | Declara `tenant-mappings` explícitos; mantén los nombres de usuario únicos entre inquilinos. |
| Segundo factor no impuesto. | El TOTP de AWE no se ejecuta en la vía SSO; impón la MFA en el proveedor. |

## Qué no cambia este modo {#what-this-mode-does-not-change}

- La autorización de pantallas y opciones, los perfiles y las restricciones: siempre AWE.
- El módulo REST también puede validar tokens emitidos externamente (modo `OAUTH2_RESOURCE_SERVER`, consulta el
  [módulo REST](../rest-module.md)); eso se configura por separado del inicio de sesión del navegador y queda fuera del alcance aquí.
