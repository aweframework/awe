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
| Claim de rol enviado a la aplicación | IdP | `AweWebSecurityConfig.realmRolesAuthoritiesConverter` lee los claims de `awe.security.sso.roles-claims` (por defecto `realm_access.roles`) |
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
| IdP | El usuario no puede autenticarse. | Las sesiones de AWE existentes terminan de inmediato con el cierre de sesión por canal trasero cuando el proveedor termina las sesiones del usuario; en caso contrario viven hasta que terminan (ver más abajo). |
| AWE (indicadores `enabled` / bloqueado en el registro de usuario) | El usuario es rechazado tras un inicio de sesión correcto en el IdP. | Comportamiento actual desde AWE 5.0.0: el inicio de sesión SSO comprueba ambos indicadores y muestra una página de error; no se crea ninguna sesión. AWE 4.x y anteriores no los comprobaban en la vía SSO, por lo que allí un usuario deshabilitado o bloqueado en AWE todavía podía iniciar sesión a través del proveedor: bloquea también a esos usuarios en el proveedor. |

Bloquea a un usuario en el proveedor cuando deba perder el acceso en todas partes; bloquéalo en AWE cuando deba perder
el acceso solo a esta aplicación.

### Duración de la sesión y revocación en el IdP {#session-lifetime-and-idp-revocation}

Tras iniciar sesión, la sesión de AWE es una sesión HTTP normal (consulta la [configuración de sesión](../session.md)). El perfil del usuario,
las restricciones, el idioma, el tema y la pantalla inicial se cargan en ella al iniciar sesión (`AweSessionDetails`). AWE no
vuelve a llamar al proveedor mientras la sesión esté viva, por lo que:

- Sin el cierre de sesión por canal trasero, deshabilitar o eliminar al usuario en el IdP **no** termina una sesión de AWE abierta; termina cuando caduca o cuando
  el usuario cierra sesión.
- Con `awe.security.sso.back-channel-logout.enabled=true` (desde AWE 5, desactivado por defecto), el proveedor termina directamente las sesiones de
  AWE: cuando termina la sesión del usuario en el proveedor (cierre de sesión en otro lugar, un administrador cierra la sesión del
  usuario), envía un token de cierre de sesión y AWE termina las sesiones correspondientes, incluido el estado de AWE que se guarda para ellas (datos de
  sesión y conexiones websocket). Deshabilitar a un usuario en el proveedor termina las sesiones de AWE solo cuando el proveedor también
  termina las sesiones del usuario en el proveedor y envía el cierre de sesión; comprueba el comportamiento del proveedor. Los detalles y advertencias
  (llamada interna, nombre de la cookie, clústeres) están en [Single sign-on](authentication-sso.md#back-channel-logout).
- Un cambio de rol o de perfil solo surte efecto en el siguiente inicio de sesión.
- Mantén el tiempo de espera de sesión de la aplicación lo bastante corto para cumplir tus requisitos de revocación.

### Cierre de sesión {#logout}

Hoy el cierre de sesión es **iniciado por la aplicación (RP-initiated)**: `/action/logout` borra la sesión de AWE y, cuando el SSO está habilitado, redirige al
navegador al endpoint de fin de sesión del proveedor mediante `OidcClientInitiatedLogoutSuccessHandler`, que termina también la
sesión del proveedor. Esto requiere que el navegador complete la redirección. Un cierre de sesión en el proveedor por otro canal solo llega a AWE
con el cierre de sesión por canal trasero habilitado (ver arriba).

### Mapeo de roles {#role-mapping}

- Las autoridades del proveedor se filtran con `awe.security.sso.filter-authority-prefix`: solo se consideran las autoridades que
  empiezan por el prefijo y este se elimina para obtener el nombre del perfil de AWE. Con un prefijo vacío, se usa la
  autoridad completa.
- Desde AWE 5, cuando un usuario lleva varios roles coincidentes, gana el **primer nombre, por orden alfabético, que exista como
  perfil de AWE**, sea cual sea el orden de los roles en el token (el primer nombre por orden alfabético cuando no
  existe ninguno). AWE 4.x tomaba el primer rol de un conjunto sin orden, por lo que la elección no era estable.
- Un rol que no existe como perfil de AWE se ignora para los usuarios existentes (se mantiene su perfil) y se sustituye
  por el rol por defecto para los usuarios recién aprovisionados, salvo que se exija un rol (ver más abajo).
- Desde AWE 5, los claims se pueden configurar con `awe.security.sso.roles-claims` (por defecto `realm_access.roles`):
  `resource_access.<client-id>.roles` para los roles de cliente de Keycloak, `roles` para los roles de aplicación de Entra ID, o varios de ellos.
  Los detalles están en [Single sign-on](authentication-sso.md#roles-claim).

### Aprovisionamiento y rol por defecto {#provisioning-and-the-default-role}

Los nombres de propiedad siguientes son los de `SecurityConfigProperties.Sso` y `BaseConfigProperties`.

Cuando se exige un rol (`awe.security.sso.require-role`, exigido por defecto en IdP-first, ver más abajo), un inicio de sesión
cuyos roles del proveedor no se mapean a ningún perfil de AWE existente se rechaza antes de que se aplique ninguna de estas filas.

| Situación | Comportamiento cuando no se exige un rol |
| --- | --- |
| Usuario desconocido, `awe.security.sso.auto-provision-user=true` (por defecto) | Se crea un registro de usuario con el rol mapeado, o con `awe.application.default-role` (por defecto `operator`) cuando ningún rol se mapea. |
| Usuario desconocido, `auto-provision-user=false` | No se crea ningún registro, pero el inicio de sesión continúa con una sesión construida a partir de los claims del token y el mismo rol de respaldo. Desactivar este indicador no deniega por sí solo a los usuarios desconocidos. |
| Usuario existente, el proveedor no envía ningún rol utilizable | Se mantiene el perfil almacenado, salvo que `awe.security.sso.overwrite-profile-with-default-role=true`. |
| Usuario nuevo, el proveedor no envía ningún rol utilizable | Recurre a `awe.application.default-role`. |

Los usuarios nuevos toman su nombre de inicio de sesión del claim `preferred_username`, mientras que los usuarios existentes se buscan con
`awe.security.sso.user-name-attribute` (por defecto `preferred_username`). Mantén ambos alineados.

### Mapeo de roles con fallo cerrado {#fail-closed-role-mapping}

Desde AWE 5, en el modo IdP-first (`awe.security.sso.local-login=false`) un inicio de sesión sin ningún rol mapeable se **deniega**
(fallo cerrado): no se usa ni el perfil almacenado ni el rol por defecto, y el usuario ve una página de error localizada.
Esto se aplica también a los **usuarios existentes**: el perfil almacenado en AWE no es un respaldo. Con el inicio de sesión local activado, se mantiene el
comportamiento anterior salvo que se establezca `require-role=true`.
El rol por defecto de respaldo es una opción explícita con `awe.security.sso.require-role=false`, de modo que nunca pueda conceder
acceso por accidente. `awe.security.sso.require-role=true` exige un rol en cualquier configuración SSO. Los detalles están en
[Single sign-on](authentication-sso.md#requiring-a-role-fail-closed).

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
`awe.security.sso.auto-launch=true` solo omite la pantalla de inicio de sesión nativa. Por defecto el endpoint de inicio de sesión local
(`POST /action/login`, `JsonAuthenticationFilter`) y el proveedor de autenticación configurado
(`awe.security.auth-mode`: base de datos, LDAP, en memoria o personalizado) permanecen registrados y activos mientras el SSO está habilitado, así que una
cuenta que tiene contraseña local todavía puede iniciar sesión sin el proveedor. Usa el interruptor de inicio de sesión local de abajo para cerrarlo.
:::

### Interruptor del inicio de sesión local y usuarios de emergencia {#local-login-switch-and-break-glass-users}

Desde AWE 5, `awe.security.sso.local-login=false` desactiva la vía de inicio de sesión local cuando el SSO está habilitado (por defecto `true`,
que mantiene el comportamiento anterior). Los detalles y ejemplos están en
[Inicio de sesión único](authentication-sso.md#switching-off-local-login).

```properties title="IdP-first con una vía de entrada de emergencia"
awe.security.sso.enabled=true
awe.security.sso.local-login=false
# Optional. Empty by default: nobody can sign in locally.
awe.security.sso.break-glass-users=admin
# Not needed: with local-login=false a role that maps to an AWE profile is required by default.
# awe.security.sso.require-role=true
```

| Aspecto | Comportamiento |
| --- | --- |
| Ámbito | Solo se aplica cuando `awe.security.sso.enabled=true`; se ignora en caso contrario. |
| Vías locales | Los tres puntos de entrada de inicio de sesión (`POST /action/login`, `POST /api/authenticate` y el inicio de sesión estándar por formulario) comparten un único gestor de autenticación que aplica la política. Solo pasan los usuarios de emergencia. El bean `AuthenticationManager` compartido que usan los flujos internos no se decora. |
| Qué ve un usuario rechazado | La misma respuesta que con una contraseña incorrecta, con aproximadamente el mismo coste (se ejecuta una autenticación real con una contraseña desechable), más el mismo evento de fallo de credenciales incorrectas. No se puede averiguar desde fuera qué usuarios son de emergencia. |
| Pantalla de acceso | Ambos clientes ocultan el usuario, la contraseña y el botón de iniciar sesión; los botones de SSO permanecen. Un usuario de emergencia abre `/screen/public/signin-local`. |
| Usuarios de emergencia | Los nombres de usuario de `awe.security.sso.break-glass-users`, recortados y comparados exactamente (distingue mayúsculas y minúsculas). Desactivado por defecto (lista vacía). Se autentican con sus credenciales locales y su perfil normales; las cuentas bloqueadas o caducadas se siguen rechazando. |
| Auditoría | Cada intento de inicio de sesión de emergencia se registra a nivel `WARN` con la etiqueta `[break-glass]` y el nombre de usuario saneado; los rechazos se registran a nivel `WARN` sin la etiqueta. Las contraseñas y los tokens nunca se registran. |
| Qué no cubre | El inicio de sesión OIDC, el modo REST `oauth2-resource-server` y cualquier autenticación que la aplicación añada fuera del gestor de autenticación de AWE. |

### Cierre de sesión por canal trasero {#back-channel-logout}

Desde AWE 5, el proveedor puede terminar las sesiones de AWE (desactivado por defecto). Los detalles están en
[Single sign-on](authentication-sso.md#back-channel-logout).

```properties title="Dejar que el proveedor termine las sesiones de AWE"
awe.security.sso.back-channel-logout.enabled=true
# Only when the public address is not reachable from the server itself
awe.security.sso.back-channel-logout.logout-uri=http://localhost:8080{basePath}/logout/connect/back-channel/{registrationId}
```

### Planificado en AWE 5 {#planned-in-awe-5}

| Elemento | Intención |
| --- | --- |
| Prueba de integración con Keycloak | Una prueba automatizada de extremo a extremo contra un Keycloak real (inicio de sesión, mapeo de perfil, cierre de sesión por canal trasero, usuario deshabilitado). |

## Riesgos residuales y recomendaciones {#residual-risks-and-recommendations}

| Riesgo | Recomendación |
| --- | --- |
| Las sesiones de AWE abiertas sobreviven a la revocación en el IdP hasta que caducan. | Habilita el cierre de sesión por canal trasero (`awe.security.sso.back-channel-logout.enabled=true`) y mantén un tiempo de espera de sesión corto como segunda línea, para los proveedores o eventos que no envían cierre de sesión. |
| El inicio de sesión local permanece activo bajo SSO (por defecto). | Define `awe.security.sso.local-login=false` y trata cada inicio de sesión de emergencia del log como una excepción monitorizada. |
| El rol por defecto puede conceder acceso a usuarios sin rol. | Mantén `awe.application.default-role` **sin privilegios** (nunca un perfil administrativo); mantén el fallo cerrado por defecto de IdP-first, o establece `awe.security.sso.require-role=true`. |
| Varios roles llevan el prefijo. | Gana el primer nombre, por orden alfabético, que exista como perfil de AWE; envía un único rol con prefijo por usuario cuando la elección importe. |
| Secretos de cliente en archivos de configuración. | Inyéctalos desde variables de entorno o un almacén de secretos; si usas propiedades cifradas `ENC(...)`, sustituye el valor por defecto de `awe.security.master-key`. Nunca subas secretos al repositorio. |
| Proveedor incorrecto o suplantado. | Define `issuer-uri` explícitamente y sobre HTTPS; Spring Security valida el emisor, la audiencia, la caducidad y el nonce del token ID. Para Entra ID prefiere un registro de aplicación de un solo inquilino. Mantén los URI de redirección exactos (sin comodines en producción). |
| Resolución de inquilino abierta. | Declara `tenant-mappings` explícitos; mantén los nombres de usuario únicos entre inquilinos. |
| Segundo factor no impuesto. | El TOTP de AWE no se ejecuta en la vía SSO; impón la MFA en el proveedor. |

## Qué no cambia este modo {#what-this-mode-does-not-change}

- La autorización de pantallas y opciones, los perfiles y las restricciones: siempre AWE.
- El módulo REST también puede validar tokens emitidos externamente (modo `OAUTH2_RESOURCE_SERVER`, consulta el
  [módulo REST](../rest-module.md)); eso se configura por separado del inicio de sesión del navegador y queda fuera del alcance aquí.
