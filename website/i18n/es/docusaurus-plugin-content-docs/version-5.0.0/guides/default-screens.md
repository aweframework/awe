---
id: default-screens
title: Ventanas por defecto
sidebar_label: Ventanas por defecto
---

## Introducción {#introduction}

AWE tiene algunas pantallas comunes prediseñadas que son muy útiles para una primera versión de una aplicación. De esta manera, ya está implementada la gestión de [usuarios](#users) y [perfiles](#profiles), algunos [temas](#themes) útiles para personalizar la aplicación, gestión de seguridad como el [control de acceso](#screen-access) y la [configuración de pantallas](#screen-configuration), y algunas herramientas de gestión a alto nivel como la [base de datos](#databases), la [configuración de módulos](#modules) y los [sitios](#sites), además de las [secuencias](#sequences) y los [parámetros de la aplicación](#application-parameters).

AWE también permite visualizar una pantalla de ayuda autogenerada basada en la definición de pantalla y algunos atributos adicionales como `help` y `help-image`. Esta pantalla de ayuda puede ser generada por pantalla o también hay una [pantalla de ayuda de la aplicación](#application-help) que contiene la documentación sobre la aplicación completa (basada en el menú).

## Usuarios {#users}

La opción *Usuarios* del menú abre la pantalla de administración de usuarios (`user-admin`): una sola pantalla para
buscar a los *usuarios* que pueden acceder a la aplicación, revisarlos, editarlos y hacer las tareas de seguridad.
Muestra los usuarios en un grid con su avatar, nombre, login y email, perfil, estado y estado del doble factor, con una
búsqueda por nombre, login, email o perfil y filtros rápidos con sus contadores (*Todos*, *Bloqueados*, *Sin 2FA*,
*Inactivos*). Cada fila tiene un botón de edición y un menú (editar, desbloquear, restablecer el doble factor, borrar), y
las filas seleccionadas muestran una barra con sus acciones (restablecer el doble factor, borrar). Un panel lateral crea
y edita un usuario, agrupado en secciones (identidad, acceso, seguridad del inicio de sesión, apariencia y salida), y
muestra el avatar, el nombre y el estado del usuario que se edita.

La columna *Perfiles adicionales* de la tabla muestra los perfiles adicionales de cada usuario como etiquetas: las dos
primeras, y una etiqueta *+N* cuyo tooltip lista las demás.

El panel lateral tiene un criterio *Perfiles adicionales*, una selección múltiple de los perfiles que el usuario tiene
además del principal. Al guardar el usuario se sustituyen sus perfiles adicionales, el perfil principal nunca se guarda
como adicional, y borrar un usuario o un perfil borra sus filas de perfiles adicionales. Consulte
[Perfiles adicionales](../security/additional-profiles.md).

*Ver permisos*, en el menú de una fila y en el panel de un usuario que se edita, abre la pestaña *Permisos* de la
[ventana de control de acceso](#screen-access) con ese usuario: los permisos de su perfil principal y de sus perfiles adicionales.
Muestra los perfiles guardados, así que el botón del panel se desactiva mientras se cambia el perfil principal; guarde
antes los cambios. Requiere `users.read` y `permissions.read`, y abre la opción `screen-access` del menú.

La pantalla reutiliza las queries y los maintains de los usuarios (`UsrLst`, `UsrNew`, `UsrUpd`, `UsrDel`, `reset2fa`),
así que una aplicación que los sobrescribe conserva su lógica. El desbloqueo usa el maintain `UsrUnl`, que además audita
el cambio.

:::warning Deprecadas
Las pantallas de usuarios anteriores (`Usr`, `UsrNew`, `UsrUpd` y `UsrViw`) están deprecadas desde AWE 5.0.0 y se
eliminarán en la siguiente versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla `user-admin`; vea la
[guía de migración a v5](v5-migration.md).
:::

<img alt="Pantalla de usuario" src={require('@docusaurus/useBaseUrl').default('img/UsersScreen.png')} />

## Perfiles {#profiles}

La opción *Perfiles* del menú abre la pantalla de administración de perfiles (`profile-admin`), construida como la de
[usuarios](#users): los *perfiles* agrupan a los usuarios en categorías de seguridad, como administradores, usuarios
regulares o usuarios especiales, y les dan un tema por defecto, una pantalla inicial y una restricción de ficheros.
Muestra los perfiles en un grid con su avatar, nombre, acrónimo y tema, pantalla inicial, restricción de ficheros y
estado, con una búsqueda por nombre, acrónimo, tema o pantalla inicial y filtros rápidos con sus contadores (*Todos*,
*Activos*, *Inactivos*). Cada fila tiene un botón de edición y un menú (editar, borrar), y las filas seleccionadas
muestran una barra para borrarlas. Un panel lateral crea y edita un perfil, agrupado en secciones (identidad, valores
por defecto y acceso), y muestra el avatar, el nombre y el estado del perfil que se edita.

La pantalla reutiliza las queries y los maintains de los perfiles (`ProLst`, `ProNew`, `ProUpd`, `ProDel` y la
comprobación `check-profile-acronym` de un acrónimo nuevo), así que una aplicación que los sobrescribe conserva su
lógica.

El campo *Permisos* del panel lateral da permisos al perfil, agrupados por recurso; los cambios se aplican en el
siguiente inicio de sesión de sus usuarios. Los perfiles predefinidos Lector y Colaborador se abren en solo lectura, y
*Duplicar perfil* crea una copia editable de cualquier perfil con sus permisos. Consulte
[La pantalla de perfiles](../security/permissions.md#profiles-screen) y
[Perfiles predefinidos](../security/permissions.md#predefined-profiles).

:::warning Deprecadas
Las pantallas de perfiles anteriores (`Prf`, `PrfNew`, `PrfUpd` y `PrfViw`) están deprecadas desde AWE 5.0.0 y se
eliminarán en la siguiente versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla `profile-admin`;
vea la [guía de migración a v5](v5-migration.md).
:::

<img alt="Pantalla de perfiles" src={require('@docusaurus/useBaseUrl').default('img/ProfilesScreen.png')} />

## Temas {#themes}

La opción *Temas* del menú abre la pantalla de administración de temas (`theme-admin`), construida como la de
[usuarios](#users): define los *temas* de la aplicación. Cada tema tiene un nombre, un primario y una superficie (una
paleta de una lista o, con la opción personalizada, un hex elegido con un selector de color) y un modo. Muestra los temas
en un grid con su avatar, nombre, primario · superficie (la paleta o el hex), modo y estado, con una búsqueda por nombre,
primario o superficie y filtros rápidos con sus contadores (*Todos*, *Activos*, *Inactivos*). Cada fila tiene un botón
de edición y un menú (editar, borrar), y las filas seleccionadas muestran una barra para borrarlas. Un panel lateral crea
y edita un tema, agrupado en secciones (identidad, colores y modo), y muestra el avatar, el nombre, los colores y el
estado del tema que se edita. Un tema nuevo empieza con el primario esmeralda, la superficie automática y el modo del
sistema, y está activo; el interruptor *Activo* se muestra al editar un tema. Consulte
[Configuración del tema](../design-system/for-developers.md).

La pantalla reutiliza las queries y los maintains de los temas (`ThmLst`, `ThmNew`, `ThmUpd` y `ThmDel`, que componen los
colores con el servicio `composeThemeColors`), así que una aplicación que los sobrescribe conserva su lógica.

:::warning Deprecadas
Las pantallas de temas anteriores (`Thm`, `ThmNew`, `ThmUpd` y `ThmViw`) están deprecadas desde AWE 5.0.0 y se
eliminarán en la siguiente versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla `theme-admin`; vea la
[guía de migración a v5](v5-migration.md).
:::

<img alt="Pantalla de temas" src={require('@docusaurus/useBaseUrl').default('img/ThemesScreen.png')} />

La pantalla `Theme customization` de AWE 4 se elimina en AWE 5 (consulte la [guía de migración a v5](v5-migration.md)).

Cada usuario alterna entre los modos `light` y `dark` con el botón de modo de la barra de navegación.

Los botones de la barra de navegación se definen en la pantalla `info-buttons.xml`. Aquí se encuentran el `themeModeSelector` y el `themeSelector`.

## Sitios {#sites}

La opción *Sitios* del menú abre la pantalla de administración de sitios (`site-admin`), construida como la de
[usuarios](#users): un sitio separa la lógica de la aplicación, cada uno con sus bases de datos, y fija qué base de datos
usa cada módulo en ese sitio. Muestra los sitios en un grid con su avatar, nombre, orden y estado, con una búsqueda por
nombre y filtros rápidos con sus contadores (*Todos*, *Activos*, *Inactivos*). Cada fila tiene un botón de edición y un
menú (editar, duplicar, eliminar), y las filas seleccionadas muestran una barra para eliminarlas.

Un panel lateral crea y edita un sitio, agrupado en secciones (identidad y los módulos y bases de datos del sitio), y
muestra el avatar, el nombre y el estado del sitio que se está editando. El grid de módulos y bases de datos está
**dentro del panel**: sus filas (módulo, base de datos y orden) se editan en el sitio y *Guardar cambios* (o *Crear
site*) guarda el sitio y sus filas con un solo maintain. El panel mide 640 píxeles, en píxeles porque el tamaño de fuente
raíz es de 10px en el cliente AngularJS y de 13px en React, así que un `max-width` en `rem` daría un panel distinto en
cada uno; las tres columnas del grid caben en esos 640 píxeles.

La pantalla reutiliza las queries y los maintains de los sitios (`SitLst`, `SitStaCnt`, `QryLstModDbsBySitAdm`, `SitNew`,
`SitUpd`, `SitDel` y `SitNamUni`), así que una aplicación que los sobrescriba conserva su lógica. `SitUpd` toma el sitio
de cada fila del grid de módulos de la propia fila (`IdeSitRow`), de modo que guardar un sitio sin cambiar sus filas
funciona, y `SitNew` lee el orden del sitio nuevo de `OrdCrt`.

:::tip Un grid hijo dentro del panel lateral
Cuando el grid hijo de un panel lateral cabe en él (unas pocas columnas estrechas), déjelo dentro del panel como hace
esta pantalla: un solo overlay y un solo guardado. Cuando es demasiado ancho, use el patrón de la pantalla de
[bases de datos](#databases): una lista de solo lectura en el panel y el grid editable en un diálogo ancho sobre él.
:::

:::warning Deprecado
Las pantallas anteriores de sitios (`Sit`, `SitNew`, `SitUpd` y `SitViw`) están deprecadas desde AWE 5.0.0 y se eliminarán
en la próxima versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla `site-admin`; consulte la
[guía de migración a la 5](v5-migration.md).
:::

## Módulos {#modules}

Un módulo es un conjunto especial de opciones definidas en el menú que se puede utilizar para separar funcionalidades dentro de una aplicación. Un usuario solo puede tener un módulo activo a la vez, por lo que cuando se cambia de módulo, las opciones del menú se actualizan con las opciones comunes y las del módulo.

La opción *Módulos* del menú abre la pantalla de administración de módulos (`module-admin`), construida como la de
[sitios](#sites). Muestra los módulos en un grid con su avatar, nombre y pantalla inicial, el número de perfiles,
usuarios y sitios, orden y estado, con una búsqueda por nombre y filtros rápidos con sus contadores (*Todos*, *Activos*,
*Inactivos*). Cada fila tiene un botón de edición y un menú (editar, duplicar, eliminar), y las filas seleccionadas
muestran una barra para eliminarlas.

Un panel lateral de 640 píxeles crea y edita un módulo en tres pestañas, y siempre se abre en la primera:

- *General*: el módulo, elegido entre los módulos declarados en el menú, su pantalla inicial, su orden en el selector de
  módulos y si está activo.
- *Acceso*: los perfiles cuyos usuarios pueden entrar en el módulo y, además de ellos, los usuarios que pueden entrar.
  Un usuario puede elegir un módulo cuando está asignado al usuario o a su perfil.
- *Sites y bases de datos*: la base de datos que usa el módulo en cada sitio. Son las mismas filas que edita la pantalla
  de [sitios](#sites) desde el sitio, así que un cambio en una pantalla se ve en la otra.

Los tres grids se editan dentro del panel, y *Guardar cambios* (o *Crear módulo*) guarda el módulo y sus filas con un solo
maintain (`ModUpd` o `ModNew`), aunque una pestaña no se haya abierto. La lista usa `ModLstAdm`, los filtros rápidos
`ModStaCnt`, y un nombre que ya existe se rechaza (`ModNamUni`).

La pantalla no tiene tema: el tema de un módulo y el tema de un usuario en un módulo se guardaban pero nunca se aplicaban,
porque el tema de un usuario sale del usuario y, si no tiene, del perfil. Los campos `IdeThm` de `ModNew` y `ModUpd` son
opcionales, así que guardar un módulo sin tema conserva el que tenía.

:::tip Pestañas dentro de un panel lateral
Cuando un panel lateral contiene varios grids hijos, repártalos en pestañas como hace esta pantalla: cada pestaña cabe en
el panel sin desplazamiento y los grids de las pestañas que no se abrieron se envían igualmente al guardar. Un campo
obligatorio vacío abre su pestaña y la marca; una fila de grid que se está editando bloquea el guardado con un aviso, pero
todavía no abre su pestaña (#918).
:::

:::warning Deprecado
Las pantallas anteriores de módulos (`Mod`, `ModNew`, `ModUpd` y `ModViw`) están deprecadas desde AWE 5.0.0 y se
eliminarán en la próxima versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla `module-admin`;
consulte la [guía de migración a la 5](v5-migration.md).
:::

## Bases de datos {#databases}

La opción *Bases de datos* del menú abre la pantalla de administración de bases de datos (`database-admin`), construida
como la de [usuarios](#users): gestiona las conexiones a orígenes de datos de cualquiera de los tipos de base de datos
soportados, como Oracle, SQL Server, H2, etc. Muestra las conexiones en un grid con su avatar, alias y descripción,
entorno, tipo de base de datos y estado, con una búsqueda por alias, descripción, conexión o usuario y filtros
rápidos con sus contadores (*Todas*, *Activas*, *Inactivas*). Cada fila tiene un botón de edición y un menú (editar,
borrar), y las filas seleccionadas muestran una barra para borrarlas. Un panel lateral crea y edita una conexión,
agrupado en secciones (conexión, credenciales, descripción y los sitios y módulos que la usan), y muestra el avatar, el
alias, la conexión y el estado de la conexión que se edita. La contraseña guardada nunca se
muestra: al editar, solo cambia si se escribe una nueva. Después de guardar o borrar una conexión se recarga el selector
de base de datos de la aplicación.

Los sitios y módulos de una conexión son un grid hijo, demasiado ancho para el panel lateral: el panel los muestra en
modo de solo lectura (sitio, módulo y orden) y su botón *Editar sitios y módulos* abre sobre el panel un diálogo ancho con
el grid editable. *Hecho* vuelve al panel, que muestra entonces los cambios, y *Guardar cambios* (o *Crear conexión*)
guarda la conexión y sus sitios y módulos a la vez. Use el mismo patrón para los grids hijos de sus propios paneles
laterales:

- Ponga el grid editable en un `<dialog>` con `style="modal-lg"` detrás del `<side-panel>` en la pantalla, para que se
  abra encima del panel. Los dos grids siguen en la pantalla, así que el maintain del panel sigue recibiendo las filas del
  grid editable.
- Muestre en el panel un grid de solo lectura (`show-toolbar="false"`, `pagination-disabled="true"`, columnas con
  `sendable="false"` y nombres propios, porque los nombres de las columnas de una pantalla son únicos), relleno con la
  acción [`copy-rows`](../api/actions.md): `<button-action type="copy-rows" target="GridSoloLectura" value="GridEditable"/>`.
  Sus columnas visibles toman, en orden, las celdas de las columnas visibles del grid editable.
- Lance `copy-rows` después de cargar o vaciar el grid editable al abrir el panel, y después de la acción `dialog` del
  botón que abre el diálogo: esa acción espera a que el diálogo se cierre (con *Hecho*, su botón de cerrar o Esc).

La pantalla reutiliza las queries y los maintains de las conexiones (`DbsLst`, `DbsNew`, `DbsUpd`, `DbsDel`, `DbsAlsUni`
y `QryLstSitModByDbs`), así que una aplicación que los sobrescribe conserva su lógica.

:::warning Deprecadas
Las pantallas de bases de datos anteriores (`Dbs`, `DbsNew`, `DbsUpd` y `DbsViw`) están deprecadas desde AWE 5.0.0 y se
eliminarán en la siguiente versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla `database-admin`;
vea la [guía de migración a v5](v5-migration.md).
:::

## Servidores de correo {#mail-servers}

La opción *Servidores de correo* del menú abre la pantalla de administración de servidores de correo
(`email-server-admin`), construida como la de [bases de datos](#databases): contiene los servidores de correo externos
que se usan para enviar correo en algunas partes de la aplicación, como la impresión de pantallas y los informes del
planificador. Muestra los servidores en una rejilla con su alias (y el host debajo), el puerto, la seguridad, el usuario,
la autenticación y el estado, con una búsqueda por alias y filtros rápidos con sus recuentos (*Todos*, *Activos*,
*Inactivos*). Cada fila tiene un botón de edición y un menú (editar, duplicar, borrar), y las filas seleccionadas
muestran una barra para borrarlas.

Un panel lateral crea y edita un servidor, agrupado en secciones (identidad: alias y el selector *Activo*; conexión:
host, puerto y seguridad; autenticación: su tipo, el usuario o buzón remitente, la contraseña y el registro de cliente
OAuth2) y muestra el alias y el estado del servidor que se está editando. La contraseña guardada nunca se muestra: al
editar, solo cambia si se escribe una nueva. *Duplicar* abre el panel como un servidor nuevo con los valores del
seleccionado, sin su contraseña, y el alias de un servidor es único.

La pantalla lee las consultas y los mantenimientos de servidores de correo del framework (`EmlSrvLst`, `EmlSrvNew`,
`EmlSrvUpd`, `EmlSrvDel`, `EmlSrvNamUni` y `EmlSrvOauRegLst`) y añade `EmlSrvStaCnt`, `EmlSrvStaCntAct` y
`EmlSrvStaCntIna` para los recuentos de los filtros rápidos, de modo que una aplicación que los sobrescriba conserva su
lógica. Los remitentes de correo leen los servidores, con su contraseña cifrada, de la consulta solo de servidor
`EmlSrvSndLst`: `EmlSrvLst` nunca devuelve la contraseña.

Cada servidor de correo define:

| Campo | Descripción |
|-------|-------------|
| Alias | Nombre del servidor (hasta 100 caracteres). Los usuarios y los [correos](../api/email-definition.md#special-variables) se refieren al servidor por este nombre. |
| Servidor | Host SMTP. |
| Puerto | Puerto SMTP. Déjelo vacío para usar el puerto por defecto del modo de seguridad. |
| Seguridad | Seguridad de la conexión: `Ninguna`, `STARTTLS` o `SSL/TLS`. |
| Autenticación | `Ninguna`, `Usuario y contraseña` u `OAuth2`. |
| Usuario / Buzón remitente | Usuario para la autenticación con usuario y contraseña, o buzón remitente para OAuth2: la etiqueta del campo sigue al tipo de autenticación. |
| Contraseña | Solo para la autenticación con usuario y contraseña. Se guarda cifrada. Al editar, déjela vacía para mantener el valor actual. |
| Registro de cliente OAuth2 | Solo para la autenticación OAuth2, y obligatorio: el registro de cliente de Spring Security que proporciona el token de acceso. |

Los cambios hechos en esta pantalla se aplican a los remitentes de correo al momento; no hace falta reiniciar la
aplicación.

:::warning Obsoleto
Las pantallas anteriores de servidores de correo (`EmlSrv`, `EmlSrvNew` y `EmlSrvUpd`) están obsoletas desde AWE 5.0.0 y
se eliminarán en la siguiente versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla
`email-server-admin`; consulte la [guía de migración a la v5](v5-migration.md).
:::

### Remitente por defecto y servidores de correo {#default-sender-and-mail-servers}

- `awe.mail.enabled=true` activa todas las funciones de correo. Sin esta propiedad la aplicación no envía ningún
  correo, ni siquiera a través de los servidores de esta pantalla.
- El **remitente por defecto** se configura con las [propiedades `awe.mail.*`](../properties.md#awe-email-properties)
  (no con `spring.mail.*`).
- Para enviar un mensaje, AWE toma el servidor de correo del **usuario que envía** (el usuario del parámetro
  `SESSION_USER` cuando el envío lo lleva y, si no, el usuario de la sesión): el servidor de correo asignado a ese
  usuario en la pantalla de [usuarios](#users). Si el usuario no tiene servidor, o el servidor no está definido aquí, el
  mensaje se envía a través del remitente por defecto.
- Un servidor de correo que no se puede leer (por ejemplo, con un valor guardado que no es válido) se descarta, con un
  error en el log que nombra el servidor: los demás servidores siguen funcionando y los usuarios de ese servidor envían
  a través del remitente por defecto.
- Un servidor de correo con OAuth2 que se lee pero está mal configurado (sin TLS, sin buzón remitente, con un registro
  desconocido) nunca recurre al remitente por defecto: sus mensajes fallan.

Tanto los servidores de correo de esta pantalla como el remitente por defecto pueden autenticarse con OAuth2: consulte
[Autenticación OAuth2](#oauth2-authentication).

### Modo de seguridad y puerto {#security-mode-and-port}

| Seguridad | Propiedades de JavaMail | Puerto por defecto |
|-----------|-------------------------|--------------------|
| `Ninguna` | Conexión sin cifrar | `25` |
| `STARTTLS` | `mail.smtp.starttls.enable=true` y `mail.smtp.starttls.required=true`: la conexión se eleva a TLS y falla si el servidor no lo admite | `587` |
| `SSL/TLS` | `mail.smtp.ssl.enable=true`: TLS desde el inicio (TLS implícito) | `465` |

Un puerto escrito en la pantalla siempre tiene prioridad sobre el puerto por defecto.

### Tipos de autenticación {#authentication-types}

- **Ninguna**: el servidor acepta mensajes sin autenticación (por ejemplo, un relay interno).
- **Usuario y contraseña**: SMTP AUTH con el usuario y la contraseña del servidor. La contraseña se descifra justo
  antes de enviarla al servidor SMTP.
- **OAuth2**: SASL `XOAUTH2` con el buzón remitente y un token de acceso OAuth2 en lugar de una contraseña. Consulte
  [Autenticación OAuth2](#oauth2-authentication).

#### Ejemplo con Amazon SES {#amazon-ses-example}

Amazon SES usa la autenticación con usuario y contraseña con las credenciales SMTP creadas en la consola de SES (no son
las claves de acceso de IAM):

| Campo | Valor |
|-------|-------|
| Servidor | `email-smtp.[REGION].amazonaws.com` |
| Puerto | `587` (o vacío) |
| Seguridad | `STARTTLS` |
| Autenticación | `Usuario y contraseña` |
| Usuario / Buzón remitente | Nombre de usuario SMTP de SES |
| Contraseña | Contraseña SMTP de SES |

### Autenticación OAuth2 {#oauth2-authentication}

Un servidor de correo de esta pantalla, o el remitente por defecto, puede autenticarse con OAuth2 en lugar de con un
usuario y una contraseña. Microsoft está desactivando SMTP AUTH con usuario y contraseña (autenticación básica) en
Exchange Online, y OAuth2 es su sustituto. Para configurarlo:

1. Prepare el proveedor: una aplicación que obtenga tokens con el grant `client_credentials` y pueda enviar desde el
   buzón remitente (consulte el [ejemplo con Microsoft 365](#microsoft-365-example)).
2. Declare esa aplicación como un [registro de cliente](#oauth2-client-registration) de Spring Security.
3. Use el registro en un [servidor de correo de esta pantalla](#mail-server-with-oauth2) o en el
   [remitente por defecto](#default-sender-with-oauth2).
4. Envíe un mensaje y lea el log si falla (consulte [Resolución de problemas](#oauth2-troubleshooting)).

#### Cómo funciona {#how-it-works}

1. Antes de cada conexión SMTP, AWE pide a Spring Security (`OAuth2AuthorizedClientManager` con el proveedor
   `client_credentials`) un token de acceso del registro de cliente. No hay interacción con el usuario: la aplicación
   se autentica a sí misma con su client id y su client secret.
2. AWE se conecta al servidor SMTP con TLS (`STARTTLS` o `SSL/TLS`) y se autentica con SASL `XOAUTH2`: el usuario es el
   buzón remitente y la contraseña es el token de acceso, enviado como token `Bearer`.
3. El mensaje se envía desde el buzón remitente.

| Tema | Comportamiento |
|------|----------------|
| Caché del token | AWE guarda en memoria un token por servidor de correo (el remitente por defecto cuenta como un servidor más, `awe.mail`). Spring Security lo reutiliza y pide uno nuevo cuando está a punto de caducar (le quedan menos de 60 segundos). |
| Token rechazado | Cuando el servidor SMTP rechaza la autenticación, se descarta el token en caché: ese mensaje falla y el siguiente envío pide un token nuevo. |
| Endpoint de tokens | Las peticiones al endpoint de tokens tienen un timeout de conexión de 10 segundos y un timeout de lectura de 30 segundos. |
| TLS | Obligatorio: el token de acceso es una credencial y nunca se envía por una conexión sin cifrar. Sin `STARTTLS` ni `SSL/TLS` no se envía nada y no se pide ningún token. |
| Sin alternativa | Un servidor de correo o un remitente por defecto con OAuth2 que no puede autenticarse nunca recurre a otro servidor, ni a usuario y contraseña: sus mensajes fallan. |
| Login | Los registros `client_credentials` nunca se ofrecen como opciones de login: el [inicio de sesión único](../security/authentication-sso.md) solo usa registros `authorization_code`, así que un registro de correo puede convivir con los de SSO. |
| Sin OAuth2 | Una aplicación sin ningún registro de cliente OAuth2 sigue enviando correo con los servidores `Ninguna` y `Usuario y contraseña`; la lista de registros del panel lateral está vacía. |
| Secretos | La base de datos solo guarda el id del registro: la URL de tokens, el client id y el client secret se quedan en la configuración de la aplicación. Los logs y los errores nombran el servidor y el registro, nunca el secreto ni el token. |
| Proveedores | Cualquier proveedor cuyo endpoint de tokens admita el grant `client_credentials` y cuyo servidor SMTP acepte esos tokens con `XOAUTH2`. No se admiten el grant `refresh_token`, las cuentas de servicio de Google Workspace (JWT bearer) ni `OAUTHBEARER`. |

#### Ejemplo con Microsoft 365 {#microsoft-365-example}

En el lado de Microsoft:

1. Registre una aplicación en Microsoft Entra ID y cree un client secret. Anote el id de directorio (tenant), el id de
   aplicación (client) y el valor del secreto.
2. Añada el permiso de aplicación `SMTP.SendAsApp` (*Office 365 Exchange Online*) y conceda el consentimiento de
   administrador.
3. Registre la entidad de servicio (service principal) de la aplicación en Exchange Online (Exchange Online PowerShell),
   con el object id de la aplicación empresarial:
   `New-ServicePrincipal -AppId [APPLICATION_ID] -ObjectId [ENTERPRISE_APPLICATION_OBJECT_ID]`.
4. Permita que la aplicación envíe desde el buzón remitente:
   `Add-MailboxPermission -Identity "noreply@contoso.com" -User [SERVICE_PRINCIPAL_ID] -AccessRights FullAccess`.
5. Compruebe que SMTP AUTH está activado en el buzón remitente:
   `Set-CASMailbox -Identity "noreply@contoso.com" -SmtpClientAuthenticationDisabled $false`.

:::info
Consulte [Authenticate an IMAP, POP or SMTP connection using OAuth](https://learn.microsoft.com/en-us/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth)
para los detalles de la configuración de Microsoft.
:::

El lado de AWE de este ejemplo es el registro `office365-mail` de la sección siguiente, usado por un
[servidor de correo](#mail-server-with-oauth2) o por el [remitente por defecto](#default-sender-with-oauth2).

#### Registro de cliente {#oauth2-client-registration}

Defina en las propiedades de la aplicación un registro de cliente de Spring Security con
`authorization-grant-type=client_credentials` y su proveedor (`token-uri`, o `issuer-uri` para descubrirlo). El id del
registro es el nombre al que se refiere el servidor de correo o el remitente por defecto:

```properties title="OAuth2 client registration for mail"
spring.security.oauth2.client.registration.[REGISTRATION_ID].authorization-grant-type=client_credentials
spring.security.oauth2.client.registration.[REGISTRATION_ID].client-id=[CLIENT_ID]
spring.security.oauth2.client.registration.[REGISTRATION_ID].client-secret=[CLIENT_SECRET]
spring.security.oauth2.client.registration.[REGISTRATION_ID].scope=[SCOPE]
# Provider: the token endpoint, or the issuer to discover it
spring.security.oauth2.client.provider.[REGISTRATION_ID].token-uri=[TOKEN_URI]
#spring.security.oauth2.client.provider.[REGISTRATION_ID].issuer-uri=[ISSUER_URI]
```

Con los valores del [ejemplo con Microsoft 365](#microsoft-365-example):

```properties title="Microsoft 365 registration"
spring.security.oauth2.client.registration.office365-mail.authorization-grant-type=client_credentials
spring.security.oauth2.client.registration.office365-mail.client-id=[APPLICATION_ID]
spring.security.oauth2.client.registration.office365-mail.client-secret=[CLIENT_SECRET]
spring.security.oauth2.client.registration.office365-mail.scope=https://outlook.office365.com/.default
spring.security.oauth2.client.provider.office365-mail.token-uri=https://login.microsoftonline.com/[TENANT_ID]/oauth2/v2.0/token
```

Los registros se leen al arrancar: reinicie la aplicación después de añadir o cambiar uno.

#### Servidor de correo con OAuth2 {#mail-server-with-oauth2}

Cree (o edite) el servidor de correo en el panel lateral y seleccione `OAuth2` como autenticación: el campo *Usuario*
pasa a ser *Buzón remitente* y aparece el selector *Registro de cliente OAuth2*, que solo lista los registros
`client_credentials`. Cuando la seguridad es `Ninguna`, el panel la cambia a `STARTTLS`. Con el registro de
Microsoft 365:

| Campo | Valor |
|-------|-------|
| Servidor | `smtp.office365.com` |
| Puerto | `587` (o vacío) |
| Seguridad | `STARTTLS` |
| Autenticación | `OAuth2` |
| Buzón remitente | El buzón que envía los mensajes, por ejemplo `noreply@contoso.com` |
| Registro de cliente OAuth2 | `office365-mail` |

Después seleccione el servidor en el campo *Servidor correo* de los usuarios que deben enviar a través de él, en la
pantalla de [usuarios](#users). Los demás usuarios siguen enviando a través del remitente por defecto (consulte
[Remitente por defecto y servidores de correo](#default-sender-and-mail-servers)).

#### Remitente por defecto con OAuth2 {#default-sender-with-oauth2}

El remitente por defecto se autentica con OAuth2 cuando
[`awe.mail.oauth2-registration`](../properties.md#awe.mail.oauth2-registration) nombra un registro de cliente
`client_credentials`. `awe.mail.user` es el buzón remitente, `awe.mail.pass` y `awe.mail.auth` no se usan, y es
obligatorio `awe.mail.tls=true` (STARTTLS requerido) o `awe.mail.ssl=true`. Con el registro de Microsoft 365:

```properties title="Microsoft 365 default sender"
awe.mail.enabled=true
awe.mail.host=smtp.office365.com
awe.mail.port=587
awe.mail.tls=true
awe.mail.user=noreply@contoso.com
awe.mail.oauth2-registration=office365-mail
```

Si faltan TLS/SSL o el usuario, o el registro no existe o no es `client_credentials`, al arrancar se escribe en el log un
error que nombra la propiedad y el registro, la aplicación arranca y todos los mensajes del remitente por defecto fallan
con un error de autenticación: no se pide ningún token y ningún mensaje sale sin OAuth2. Sin
`awe.mail.oauth2-registration` el remitente por defecto funciona como antes.

#### Resolución de problemas {#oauth2-troubleshooting}

AWE escribe estos errores en el log de la aplicación, y el envío que los encuentra falla con un error de autenticación.
Los servidores de correo de esta pantalla se comprueban cuando se cargan (en el primer envío y después de un cambio en
esta pantalla) y en cada envío; el remitente por defecto se comprueba al arrancar y en cada envío. `<server>` es el alias
del servidor de correo, o `awe.mail` para el remitente por defecto, y `<id>` es el id del registro.

| Mensaje | Significado y solución |
|---------|------------------------|
| `Email server '<server>' uses OAuth2 authentication without STARTTLS or SSL` | La seguridad es `Ninguna`. Seleccione `STARTTLS` o `SSL/TLS`; para el remitente por defecto, configure `awe.mail.tls=true` o `awe.mail.ssl=true`. |
| `awe.mail.oauth2-registration '<id>' needs awe.mail.tls=true or awe.mail.ssl=true: every email of the default sender fails` | Error de arranque del remitente por defecto: la misma solución. |
| `Email server '<server>' uses OAuth2 authentication without a sender mailbox` | El servidor de correo no tiene buzón remitente: escríbalo en el panel lateral. |
| `awe.mail.oauth2-registration '<id>' needs awe.mail.user (sender mailbox): every email of the default sender fails` | Error de arranque del remitente por defecto: configure `awe.mail.user`. |
| `Email server '<server>' uses an unknown OAuth2 client registration '<id>'` | No hay ningún `spring.security.oauth2.client.registration.<id>.*` con ese id: revise cómo está escrito y que las propiedades llegan a la aplicación. |
| `Email server '<server>' uses OAuth2 client registration '<id>', which is not client_credentials` | El registro tiene otro grant (por ejemplo el `authorization_code` del login): defina un registro aparte con `authorization-grant-type=client_credentials`. |
| `awe.mail.oauth2-registration '<id>' cannot be used: <message>` | Error de arranque del remitente por defecto; `<message>` es uno de los dos errores anteriores. |
| `Email server '<server>' could not obtain an OAuth2 access token from registration '<id>' (<code>)` | El endpoint de tokens no devolvió un token. `<code>` es el error OAuth2 del proveedor (vea más abajo). No se envía nada al servidor SMTP. |
| Respuesta SMTP `535` (Microsoft 365: `535 5.7.3 Authentication unsuccessful`) | AWE obtuvo un token pero el servidor SMTP lo rechazó para el buzón remitente. Revise el permiso `SMTP.SendAsApp` y su consentimiento de administrador, la entidad de servicio, el permiso sobre ese buzón exacto y que SMTP AUTH está activado en él. El token se descarta, así que el siguiente envío pide uno nuevo. |

| `<code>` | Causa habitual |
|----------|----------------|
| `invalid_client`, `unauthorized_client` | Client id o client secret incorrectos (un client secret de Entra ID caduca), o la aplicación no pertenece al tenant del `token-uri`. |
| `invalid_scope` | El proveedor no acepta el scope. Para Microsoft 365 es `https://outlook.office365.com/.default`. |
| `invalid_token_response` | No hay una respuesta de token válida: no se puede acceder al endpoint de tokens, no respondió dentro de los timeouts, el `token-uri` es incorrecto o la respuesta no es una respuesta de token OAuth2. |
| `no access token` | El endpoint de tokens respondió sin token de acceso. |

### Actualización {#upgrading}

La migración de Flyway `AWE_V1.2.3__Email_server_settings` (en todas las bases de datos soportadas) amplía las columnas
del nombre del servidor (100), del usuario (255) y de la contraseña (512) de `AweEmlSrv` y `HISAweEmlSrv`, y añade las
columnas `SecMod` (modo de seguridad) y `OauReg` (registro de cliente OAuth2). Los servidores existentes siguen
funcionando como antes: sin modo de seguridad equivale a `Ninguna`, sin puerto equivale a `25`, y los valores de
autenticación `0` (ninguna) y `1` (usuario y contraseña) no cambian; `2` es OAuth2.

:::note
En versiones anteriores la contraseña guardada a través de esta pantalla llegaba cifrada al servidor SMTP, así que los
servidores con autenticación de usuario y contraseña no podían autenticarse. Ahora la contraseña se descifra antes de
enviarla, y esos servidores se autentican correctamente. Una contraseña que no se puede descifrar (por ejemplo, una
insertada en texto plano directamente en la base de datos) se usa tal y como está guardada.
:::

## Colas de mensajes {#message-queues}

La opción *Colas de mensajes* del menú abre la pantalla de administración de colas de mensajes (`queue-admin`),
construida como la de [bases de datos](#databases): contiene las colas JMS que la aplicación usa para comunicarse con un
broker, y cada entrada es la definición de cola que puede usar el archivo
[Queues.xml](api/jms-queues-definition.md). Muestra las colas en una rejilla con su alias (y la descripción debajo), el
destino, el tipo de cola, el broker y el estado, con una búsqueda por alias y filtros rápidos con sus recuentos
(*Todos*, *Activos*, *Inactivos*). Cada fila tiene un botón de edición y un menú (editar, duplicar, borrar), y las filas
seleccionadas muestran una barra para borrarlas.

Un panel lateral crea y edita una cola, agrupado en secciones (identidad: alias, descripción y el selector *Activo*;
cola: tipo de cola, destino y tipo de conexión; conexión: broker, usuario y contraseña) y muestra el estado de la cola
que se está editando. El usuario y la contraseña del broker solo se aplican a una conexión remota: con una conexión JNDI
se desactivan y se vacían. La contraseña guardada nunca se muestra: al editar, solo cambia si se escribe una nueva.

La pantalla lee las consultas y los mantenimientos de colas del framework (`QueLst`, `QueNew`, `QueUpd`, `QueDel` y
`QueAlsUni`) y añade `QueStaCnt`, `QueStaCntAct` y `QueStaCntIna` para los recuentos de los filtros rápidos, de modo que
una aplicación que los sobrescriba conserva su lógica.

:::warning Obsoleto
Las pantallas anteriores de colas (`Que`, `QueNew` y `QueUpd`) están obsoletas desde AWE 5.0.0 y se eliminarán en la
siguiente versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla `queue-admin`; consulte la
[guía de migración a la v5](v5-migration.md).
:::

## Secuencias {#sequences}

La opción *Secuencias* del menú abre la pantalla de administración de secuencias (`sequence-admin`), construida como la
de [usuarios](#users). Una secuencia es un nombre con un valor que AWE incrementa y entrega como siguiente identificador
de un registro nuevo, como haría un campo autonumérico: resulta muy útil para modelos de base de datos antiguos sin
campos autonuméricos. La pantalla muestra las secuencias en un grid con su nombre, valor y estado, con una búsqueda por
nombre y filtros rápidos con sus contadores (*Todas*, *Activas*, *Inactivas*). Cada fila tiene un botón de edición y un
menú (editar, eliminar), y las filas seleccionadas muestran una barra para eliminarlas.

Un panel lateral crea y edita una secuencia: su nombre, su estado y su valor, con una ayuda de qué significa el valor. El
nombre de una secuencia es su clave primaria, así que el panel lo muestra de solo lectura al editar: se escribe al crear
la secuencia.

Reutiliza las queries de las secuencias (`AweKeyLst`, `AweKeySug`) y añade los maintains `AweKeyNew`, `AweKeyUpd` y
`AweKeyDel`, que trabajan sobre una secuencia cada uno (`AweKeyUpd` toma la secuencia de la fila seleccionada de
`KeyNam.selected`), así que el grid editable de la pantalla anterior y su maintain múltiple ya no se usan. El maintain
múltiple sigue funcionando para la pantalla deprecada como `AweKeyMul`.

:::warning Deprecado
La pantalla anterior de secuencias (`AweKey`) está deprecada desde AWE 5.0.0 y se eliminará en la próxima versión menor
(5.1.0). Apunte sus propias opciones de menú a la pantalla `sequence-admin`; consulte la
[guía de migración a la 5](v5-migration.md).
:::

## Parámetros de la aplicación {#application-parameters}

La opción *Parámetros de la aplicación* del menú abre la pantalla de administración de parámetros de la aplicación
(`application-parameter-admin`), construida como la de [secuencias](#sequences). Los parámetros de la aplicación son una
especie de propiedades adicionales que pueden sobrescribir las definidas en el archivo `application.properties`, una capa
extra para mejorar la personalización de la aplicación. La pantalla muestra los parámetros en un grid con su nombre y
descripción, valor, categoría (*General*, *Seguridad* o *Informes*) y estado, con una búsqueda por nombre o descripción y
filtros rápidos con sus contadores (*Todos*, *Activos*, *Inactivos*). Cada fila tiene un botón de edición y un menú
(editar, eliminar), y las filas seleccionadas muestran una barra para eliminarlas.

Un panel lateral crea y edita un parámetro: su nombre, su categoría y su estado, y su valor y descripción. El nombre de
un parámetro es la clave que lee la aplicación, así que el panel lo muestra de solo lectura al editar. Al guardar un
parámetro se recargan las propiedades de la aplicación, de modo que el valor nuevo está en vigor sin reiniciarla.

Reutiliza las queries de los parámetros (`AweAppParLst`, `AweAppParSug`) y añade los maintains `AweAppParNew`,
`AweAppParUpd` y `AweAppParDel`, que trabajan sobre un parámetro cada uno (`AweAppParUpd` toma el parámetro de la fila
seleccionada de `IdeAweAppPar.selected`), así que el grid editable de la pantalla anterior y su maintain múltiple ya no se
usan. El maintain múltiple sigue funcionando para la pantalla deprecada como `AweAppParMul`.

:::warning Deprecado
La pantalla anterior de parámetros de la aplicación (`AweAppPar`) está deprecada desde AWE 5.0.0 y se eliminará en la
próxima versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla `application-parameter-admin`; consulte
la [guía de migración a la 5](v5-migration.md).
:::

## Difusión {#broadcasting}

Esta pantalla es simplemente una forma de enviar mensajes a todos los usuarios conectados a la aplicación, o un mensaje específico a un único usuario.

Aparecerá como un mensaje en la parte superior de la pantalla.

## Registro {#log}

La pantalla de registro da acceso a todos los registros de la aplicación. Se dejará de utilizar próximamente para dar paso a una nueva pila de observabilidad en una arquitectura distribuida.

## Control de acceso {#screen-access}

La opción *Control de acceso* del menú *Seguridad* abre la ventana de control de acceso (`access-admin`): una sola pantalla que responde,
para un perfil o un usuario, **qué puede abrir del menú privado y por qué**, y permite a un administrador funcional
cambiarlo. Un auditor consulta la misma pantalla sin cambiar nada.

<img alt="Pantalla de acceso" src={require('@docusaurus/useBaseUrl').default('img/AccessScreen.png')} />

### Camino rápido {#quick-path}

1. En *Ver el acceso de*, elija *Perfil* o *Usuario* y después el perfil o el usuario en el buscador de al lado. Si
   quiere, elija un módulo. Estos campos comparten una fila en la parte superior de la pantalla, encima de sus dos
   pestañas: *Opciones de menú* (el árbol) y *Permisos*.
2. Lea el árbol: cada opción del menú privado con su acceso y el motivo.
3. Pulse *¿Por qué?* en una opción para ver la capa que lo decide, y *Permitir* o *Denegar* para cambiarlo.

### Leer el árbol {#read-the-tree}

El árbol es el menú privado completo (`menu/private.xml`) de la aplicación. Cada opción muestra:

| Columna | Qué dice |
|---------|----------|
| Opción del menú | La etiqueta y, debajo, el nombre de la opción en `private.xml`. |
| Acceso | *Permitido* o *Denegado*: lo que el perfil o el usuario tiene en el menú real. |
| Motivo | La capa que decide el acceso: sin reglas (permitido por defecto), el fichero del perfil base, el módulo, una regla global, una regla del perfil, una regla del usuario o una regla heredada de una opción padre. |
| Regla propia | La regla del perfil o del usuario elegido sobre esa opción (*Permitir* o *Denegar*), si la tiene. |
| Avisos | Debajo del motivo: *permitida, pero su padre está denegado* (no se puede llegar a ella desde el menú) o *regla redundante* (quitarla no cambia el acceso ni sin módulo ni en ningún módulo del menú). |

La búsqueda encuentra opciones por etiqueta o por nombre, y los filtros rápidos muestran sus contadores: *Todas*,
*Permitidas*, *Denegadas*, *Con regla* y *Avisos*. Una opción encontrada conserva sus opciones padre en el árbol, así
que nunca aparece sola.

Con un módulo elegido, las opciones de otros módulos aparecen denegadas con el motivo *Pertenece al módulo ...*, como en
el menú real de un usuario que trabaja en ese módulo. Sin módulo, solo deciden las reglas.

### Permitir o denegar una opción {#allow-or-deny-an-option}

Los botones *Permitir* y *Denegar* de una fila guardan una regla del perfil o del usuario elegido sobre esa opción. Una
regla sobre una opción se aplica también a las opciones que cuelgan de ella. Tras el cambio se refresca el menú de los
usuarios conectados. Un botón que no cambiaría nada aparece deshabilitado: *Permitir* en una opción permitida,
*Denegar* en una denegada.

Para cambiar varias opciones a la vez, marque sus filas: aparece una barra con *Permitir*, *Denegar* y *Quitar regla*.
Con una fila marcada, la barra deshabilita los mismos botones que la fila, y *Quitar regla* solo se habilita si la
opción tiene regla propia. Con varias filas marcadas, los tres botones están habilitados y la vista previa indica qué
opciones cambian. Antes de guardar nada, un diálogo de vista previa lista las opciones cuyo acceso cambia (hoy, después del cambio y qué
lo decide hoy) y los usuarios del perfil que pierden o ganan acceso, incluidos los que lo conservan por su propia regla.
Sus botones se quedan abajo mientras las listas se desplazan. *Cancelar* no guarda nada.

### Panel ¿Por qué? {#why-panel}

*¿Por qué?* abre un panel lateral con el resultado (*OPE no puede abrir Información*) y la cascada que lo decide, de la
capa más general a la más concreta:

1. **Módulo**: una opción de un módulo distinto del elegido queda denegada.
2. **Perfil base**: el fichero de restricciones del usuario o de su perfil (`profile/<nombre>.xml`; si ninguno lo
   indica, `awe.security.default-restriction`, `general` por defecto).
3. **Regla global**: una regla para todos los usuarios.
4. **Regla del perfil**: una regla del perfil (del perfil del usuario, si se ha elegido un usuario).
5. **Regla del usuario**: una regla del usuario.
6. **Herencia**: una regla sobre una opción padre se aplica a sus hijas.

Gana la última capa con regla, y el panel la marca con *Decide*. Así, una regla del usuario gana a la de su perfil, y
una regla del perfil a una global. El acceso se calcula con la misma cascada que el menú de un usuario conectado, así
que la pantalla nunca contradice el menú real.

El panel lista también *todas las reglas sobre la opción*, de cualquier perfil o usuario, donde cada regla se puede
cambiar (*Permitir* ↔ *Denegar*) o quitar. Quitar una regla pide confirmación, porque puede ser de otro perfil o
usuario.

<img alt="Panel ¿Por qué?" src={require('@docusaurus/useBaseUrl').default('img/AccessWhyPanel.png')} />

### Comparar {#compare}

*Comparar* añade *Comparar con* a la fila del sujeto, con un recordatorio a su lado: un solo buscador que busca perfiles
y usuarios a la vez y los lista agrupados bajo *Perfil* y *Usuario* (en el cliente React, su flecha los lista todos). El
árbol gana dos columnas, el acceso en la comparación y por qué cambia, y el filtro rápido *Solo diferencias*.

- **Un perfil** simula el sujeto con ese perfil: un perfil frente a otro, o un usuario con otro perfil, conservando las
  reglas propias del usuario.
- **Un usuario** compara con el acceso real de ese usuario: su propio perfil y sus propias reglas.

No se guarda nada: *Dejar de comparar* vuelve a la vista normal. Para dar ese perfil al usuario, use la pantalla de
[usuarios](#users).

### Revisar {#review}

*Revisar* muestra el número de avisos y abre un panel lateral que comprueba las reglas de **todos** los perfiles y
usuarios, no solo del elegido. Cada hallazgo tiene su corrección:

| Hallazgo | Corrección |
|----------|------------|
| Reglas sobre opciones que ya no existen en `private.xml`, o de un perfil o usuario borrado | Quitar la regla. |
| Opciones permitidas con el padre denegado (no aparecen en el menú) | Permitir también la opción padre, o quitar la regla. |
| Reglas que no cambian el resultado (ni sin módulo ni en ningún módulo) | Quitar la regla, una a una o todas a la vez con *Quitar las reglas sin efecto*. Una regla que solo importa dentro de un módulo no aparece. |

<img alt="Revisión de accesos" src={require('@docusaurus/useBaseUrl').default('img/AccessReviewPanel.png')} />

### Previsualizar {#preview}

*Previsualizar* abre una vista previa del menú que ve el perfil o el usuario elegido en el módulo elegido: las opciones a
las que llega, cuántas quedan ocultas y el motivo de cada una. Es solo una vista previa: no inicia sesión como el
usuario ni abre sus pantallas.

### Permisos {#permissions}

La pestaña *Permisos* muestra, para el mismo perfil o usuario, **qué puede hacer**: cada permiso del catálogo (el
`Permissions.xml` de AWE, de sus módulos y de la aplicación), agrupado por recurso, con su estado y de dónde viene. Para
un usuario, los permisos son los de su perfil principal más los de sus perfiles adicionales.

| Columna | Qué indica |
|---------|------------|
| Permiso | El verbo o la acción y, debajo, su identificador (`users.update`). El grupo de cada recurso indica cuántos de sus permisos están concedidos. |
| Estado | *Concedido* o *No concedido*. |
| Origen | Una etiqueta por origen: el perfil y la entrada de su fichero de perfil (`MGR · fichero manager (*)`), el perfil y su asignación (`MGR · asignación users.*`), o *Siempre concedido* para un recurso declarado `granted="always"` en el catálogo (también los de la aplicación). |
| Aviso | Un fichero de perfil excluye el permiso pero otro origen lo concede, o la exclusión que lo deja fuera. |

El buscador encuentra permisos por identificador o etiqueta, también de su recurso, y los filtros rápidos cuentan
*Todos*, *Concedidos*, *No concedidos* y *Avisos*. La tarjeta sobre la tabla lo resume: cuántos permisos están
concedidos, los perfiles del usuario y los avisos.

Los permisos de un perfil son su fichero de perfil menos sus exclusiones, más sus asignaciones (pantalla *Perfiles*);
consulte [Permisos efectivos](../security/permissions.md#effective-permissions). Una exclusión solo resta dentro de
su propio fichero de perfil: no es una denegación, así que una asignación u otro perfil pueden conceder el permiso.
*¿Por qué?* abre un panel lateral con lo que aporta cada perfil al permiso: su fichero de perfil, sus asignaciones y su
veredicto, y el catálogo para un permiso concedido siempre.

*Comparar* también funciona en esta pestaña: la tabla muestra el estado del otro usuario o perfil y por qué cambia, con
el filtro rápido *Solo diferencias*. Los demás recuentos siguen siendo los del sujeto elegido. Un perfil se compara con
sus propios permisos, y un usuario con los permisos de todos sus perfiles.

*Ver permisos* en la pantalla de [usuarios](#users) abre esta pestaña con el usuario ya elegido. Hasta que se elige un
usuario o un perfil, la tarjeta de resumen está oculta.

La pestaña muestra el estado **configurado**: los permisos se cargan al iniciar sesión, así que un cambio se aplica en
el siguiente inicio de sesión de cada usuario (lo recuerda la ayuda de la tabla). No cambia nada y requiere `users.read` y `permissions.read`. El módulo,
*Revisar* y *Previsualizar* son de las opciones de menú y se ocultan en esta pestaña.

### El acceso que viaja con la aplicación {#access-that-travels-with-the-application}

Las reglas de esta pantalla son **datos de cada entorno**: se guardan en la base de datos y apuntan a los nombres de
las opciones del `private.xml` de la aplicación. Por eso no hay importación ni exportación de reglas: las opciones de una
aplicación no existen en otra.

- El acceso que debe ser igual en todos los entornos, y que se distribuye con la aplicación, va en los ficheros de
  restricciones base de los perfiles (`profile/<nombre>.xml`, con entradas `<restriction option="..."/>`). La pantalla
  los muestra como la capa *Perfil base*.
- Para una auditoría, *Imprimir* exporta el árbol con el acceso y el motivo de cada opción (vea el
  [motor de impresión](print-guide.md)).

:::warning Deprecadas
Las pantallas de acceso anteriores `ScrAccRes` (*Acceso a pantallas*), `menu-access` y `screen-access` están deprecadas
desde AWE 5.0.0 y se eliminarán en la siguiente versión menor (5.1.0). La opción `screen-access` del menú por defecto y
de los arquetipos abre ahora `access-admin`. Apunte sus propias opciones de menú a la pantalla `access-admin`; vea la
[guía de migración a v5](v5-migration.md). Las reglas que guardaban son las mismas, así que no hay nada que migrar.
:::

## Utilidad de cifrado {#encrypt-util}

Esta opción es muy útil para generar valores o propiedades cifrados. Basta con introducir el texto que se desea cifrar en el criterio `Text` y pulsar el botón `Encrypt`. Se rellenará el texto `Encrypted` con el texto que hay que añadir en las columnas cifradas de la base de datos y `Encrypted property`, que es el texto cifrado que se utiliza en los valores del archivo `application.properties`.

El campo `Hash de contraseña` muestra el hash del texto calculado con el algoritmo seleccionado en `Algoritmo de contraseña`, en el formato que se guarda en `ope.l1_pas`. Úselo para escribir scripts de inserción de usuarios: el valor se inserta tal cual en `ope.l1_pas`.

| Algoritmo de contraseña | Resultado |
|---|---|
| BCrypt | `{bcrypt}$2a$...` (68 caracteres). Los hashes BCrypt llevan sal, por lo que cada pulsación genera un hash distinto, y todos validan la misma contraseña |
| RIPEMD-160 (AWE 4) | El hash de AWE 4, 40 caracteres hexadecimales sin prefijo, para bases de datos o sistemas que aún lo necesiten |

Por defecto se selecciona el algoritmo configurado (`awe.security.password-encoder`). La `Phrase key` no cambia el hash de la contraseña.

<img alt="Utilidad de cifrado" src={require('@docusaurus/useBaseUrl').default('img/EncryptManagerScreen.png')} />

## Configuración de pantalla {#screen-configuration}

Esta pantalla permite cambiar la funcionalidad de la pantalla definida en el archivo XML. La nueva funcionalidad se almacena en la base de datos y puede restringirse por usuario o perfil. Para cambiar un elemento de una pantalla solo hay que definir la pantalla, el elemento que se va a restringir o modificar y el atributo con los nuevos valores.

### Ejemplo: {#example}

Si tenemos un criterio que será obligatorio `validation="required"`.

``` xml
 <criteria id="CrtSit" label="PARAMETER_NAME" component="suggest" server-action="data" target-action="SitSug" style="col-xs-7 col-sm-6 col-lg-3" validation="required"/>
```

Y queremos que deje de ser obligatorio, establezca la nueva configuración como se muestra en la imagen siguiente:

<img alt="Configuración de pantalla" src={require('@docusaurus/useBaseUrl').default('img/screen_conf.png')} />

### Cómo se usa: {#how-to-use-it}

Si desea acceder a esta pantalla y utilizarla, añada la siguiente opción en su `private.xml` o `public.xml`.

``` xml
 <option name="screen_configuration" label="MENU_TOOLS_SCR_CNF" screen="ScrCnf" icon="laptop" />
```

## Manual de usuario {#user-manual}

Esta opción permite al usuario acceder a un archivo PDF definido en la aplicación como manual de usuario. Este archivo PDF puede definirse en varios idiomas.

## Ayuda de la aplicación {#application-help}

<img alt="Ayuda de la aplicación" src={require('@docusaurus/useBaseUrl').default('img/ApplicationHelpScreen.png')} />