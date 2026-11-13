---
id: permissions
title: Permisos de consultas, targets de maintain y servicios
sidebar_label: Permisos
---

# Permisos {#permissions}

Un **permiso** nombra una cosa que puede hacer un usuario, como leer los usuarios o borrar un perfil. El código declara
los permisos en un catálogo, una consulta o un target de maintain declara los permisos que requiere, y el administrador
asigna permisos a los **perfiles**. Un cliente solo lanza una consulta, un target o un servicio que declara un permiso
cuando el usuario de la sesión lo tiene.

| Quiere | Lea |
| --- | --- |
| Declarar los permisos de un módulo | [Catálogo de permisos](#permission-catalog) |
| Exigir un permiso para lanzar una consulta o un target | [El atributo `permission`](#the-permission-attribute) |
| Exigir un permiso para llamar a un servicio | [Servicios](#services) |
| Ocultar los botones que un usuario no puede usar, o abrir un panel en solo lectura | [Elementos de pantalla](#screen-elements) |
| Dar permisos a un perfil | [La pantalla de perfiles](#profiles-screen), [Asignación a perfiles](#assignment-to-profiles) |
| Distribuir permisos con un perfil, o usar los perfiles Lector y Colaborador | [Ficheros de perfil](#profile-files), [Perfiles predefinidos](#predefined-profiles) |
| Exigir un permiso a los targets que no declaran ninguno | [Targets sin permiso](#require-undeclared) |
| Ver qué puede hacer un usuario y por qué | [Permisos efectivos](#effective-permissions) |
| Comprobar un permiso en Java | [Autoridades de Spring Security](#spring-security-authorities) |
| Desplegarlo o encontrar los rechazos | [Modo](#mode), [Auditoría](#audit) |
| Saber qué protegen AWE y sus módulos | [Recursos del catálogo](#catalog-resources), [Permisos de las pantallas de AWE](#permissions-of-the-awe-screens) |

## Identificadores de permiso {#permission-identifiers}

Un identificador de permiso es `<recurso>.<verbo>` o `<recurso>.<acción>`:

- Un **recurso** es lo que protege el permiso: `users`, `profiles`, `invoices`...
- Un **verbo** es una operación genérica que comparten los recursos: `read`, `create`, `update`, `delete`, `export`.
- Una **acción** es una operación de un solo recurso: `users.unlock`, `users.reset-2fa`.

Los identificadores de verbos, recursos y acciones usan letras, dígitos, `_` y `-`.

## Catálogo de permisos {#permission-catalog}

Cada módulo de `awe.application.module-list` puede aportar un catálogo en `global/Permissions.xml`. AWE combina los
catálogos de todos los módulos, igual que combina `Queries.xml` y `Maintain.xml`. Este es el catálogo de AWE:

```xml title="global/Permissions.xml"
<permissions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/permissions.xsd">
  <verb id="read" label="PERMISSION_VERB_READ" />
  <verb id="create" label="PERMISSION_VERB_CREATE" />
  <verb id="update" label="PERMISSION_VERB_UPDATE" />
  <verb id="delete" label="PERMISSION_VERB_DELETE" />
  <verb id="export" label="PERMISSION_VERB_EXPORT" />

  <!-- verbs="crud" means read, create, update and delete -->
  <resource id="users" label="PERMISSION_RESOURCE_USERS" verbs="crud">
    <action id="unlock" label="PERMISSION_USERS_UNLOCK" />
    <action id="reset-2fa" label="PERMISSION_USERS_RESET_2FA" />
  </resource>
  <resource id="profiles" label="PERMISSION_RESOURCE_PROFILES" verbs="crud" />
  <resource id="permissions" label="PERMISSION_RESOURCE_PERMISSIONS" verbs="read,update" />
  <resource id="system" label="PERMISSION_RESOURCE_SYSTEM" verbs="read,update" />
  <resource id="access" label="PERMISSION_RESOURCE_ACCESS" verbs="read,update" />
  <resource id="audit" label="PERMISSION_RESOURCE_AUDIT" verbs="read" />
  <resource id="themes" label="PERMISSION_RESOURCE_THEMES" verbs="crud" />

  <!-- Granted to every authenticated user -->
  <resource id="self" label="PERMISSION_RESOURCE_SELF" verbs="read,update" granted="always" />

  <resource id="default" label="PERMISSION_RESOURCE_DEFAULT" verbs="read,write" />
</permissions>
```

| Elemento | Atributos | Descripción |
| --- | --- | --- |
| `verb` | `id`, `label` | Un verbo. Los verbos de cada módulo están disponibles para los recursos de todos los módulos. |
| `resource` | `id`, `label`, `verbs`, `granted` | Un recurso. Tiene un permiso por cada verbo de `verbs` (separados por comas, `crud` equivale a `read,create,update,delete`) y uno por cada `action`. |
| `action` | `id`, `label` | Una acción del recurso que la contiene. |

- Las etiquetas son claves de locale.
- `granted="always"` da los permisos del recurso a todos los usuarios autenticados. El recurso `self` de AWE lo usa,
  así que ningún perfil necesita `self.read` ni `self.update`. Un permiso concedido siempre no necesita ninguna
  autoridad: lo tiene todo usuario autenticado, también con una autenticación que no construyó el inicio de sesión de
  AWE.
- El verbo `write` lo declara AWE y solo existe para el recurso `default` de AWE, el permiso de los targets que no
  declaran ninguno (consulte [Targets sin permiso](#require-undeclared)).
- El esquema es `permissions.xsd` (consulte [Herramientas XSD](../guides/xsd-tooling.md#published-schemas)).

Una aplicación declara sus propios permisos en el `global/Permissions.xml` de su módulo:

```xml title="global/Permissions.xml"
<permissions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/permissions.xsd">
  <verb id="approve" label="PERMISSION_VERB_APPROVE" />
  <resource id="invoices" label="PERMISSION_RESOURCE_INVOICES" verbs="crud,export,approve" />
</permissions>
```

### Ampliar o sobrescribir los permisos de AWE {#catalog-override}

Los módulos se combinan en el orden de `awe.application.module-list`, y el módulo de la aplicación va el primero.
Una aplicación vuelve a declarar un recurso de AWE para ampliarlo o para cambiar sus etiquetas:

```xml title="global/Permissions.xml"
<permissions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/permissions.xsd">
  <!-- Themes conserva read, create, update y delete de AWE y añade export -->
  <resource id="themes" label="PERMISSION_RESOURCE_APP_THEMES" verbs="export" />
  <!-- users.unlock se muestra con la etiqueta de la aplicación -->
  <resource id="users" label="PERMISSION_RESOURCE_USERS">
    <action id="unlock" label="PERMISSION_USERS_UNLOCK_ACCOUNT" />
  </resource>
</permissions>
```

- **Etiquetas**: el primer módulo que declara un verbo, un recurso o un permiso da su etiqueta. La etiqueta de la
  aplicación gana a la de AWE.
- **Verbos y acciones**: los permisos de un recurso son los de todos los módulos que lo declaran. Una aplicación añade
  verbos y acciones a un recurso de AWE, pero no puede quitarlos: liste solo los verbos y las acciones que añade.
- **El mismo permiso** declarado por varios módulos (`themes.read` listado por la aplicación y por AWE, o la acción
  `users.unlock` de arriba) conserva la primera declaración, y se escribe en el log un aviso que nombra el permiso al
  arrancar.

### Errores de carga {#catalog-load-errors}

Estas declaraciones son errores de carga: un identificador con otros caracteres, un recurso que lista un verbo que no
declara ningún módulo y un recurso distinto de `default` que lista el verbo `write`. Un error de carga hace fallar el
arranque. En una [recarga en caliente](../dev-tools.md#xml-hot-reload-backend-definitions) el catálogo nuevo se rechaza
y AWE conserva el anterior; también conserva el catálogo anterior cuando al nuevo le falta un permiso que requiere una
consulta o un target.

## El atributo `permission` {#the-permission-attribute}

Una `<query>` o un `<target>` de maintain declara los permisos que requiere en su atributo `permission`:

```xml
<target name="UsrDel" permission="users.delete">
  ...
</target>

<query id="InvoicesToApprove" permission="invoices.read invoices.approve">
  ...
</query>
```

- Varios permisos se separan con espacios, y se requieren **todos**.
- Los permisos son explícitos: el atributo no admite comodines.
- Un permiso que no está en el catálogo, o un `permission` en una consulta o un target `public`, es un **error de
  carga**: el arranque falla e indica la consulta o el target y el permiso, de modo que una errata nunca deja un target
  sin proteger. En una recarga en caliente, el fichero modificado se rechaza y AWE conserva las definiciones anteriores.
- Una consulta o un target sin `permission` no se comprueba por permiso.

El permiso se comprueba en los lanzamientos que pide un cliente: las peticiones `/action/...`, los endpoints de
maintain de ficheros y los endpoints de la [API REST](#rest-api). Los lanzamientos que hace el servidor nunca se
comprueban: los servicios Java que llaman a `launchPrivateMaintain` o `launchPrivateQuery`, y los targets que un
maintain incluye con `<include target="..."/>`. Las cargas iniciales de los componentes de una pantalla también son
lanzamientos de cliente: un componente cuya consulta se rechaza se abre sin datos, y la pantalla muestra un único aviso
aunque se rechacen varios componentes.

El permiso funciona junto con el [acceso por pantalla](screen-access.md): un lanzamiento de la aplicación web de un
target que declara un permiso solo se ejecuta cuando el acceso por pantalla lo permite **y** el usuario tiene todos los
permisos. El permiso hace la comprobación más estricta; nunca abre lo que el acceso por pantalla cierra.

### API REST {#rest-api}

La API REST (`/api/maintain`, `/api/data` y `/api/public/...`) se autoriza solo por los permisos: el
[acceso por pantalla](screen-access.md#rest-api) no se aplica, así que un usuario que no puede abrir ninguna pantalla
que use un target lo lanza por la API REST cuando tiene sus permisos.

- Una petición rechazada responde HTTP `403` con un `AweRestResponse` de tipo `ERROR` y el mensaje genérico de rechazo,
  que nunca nombra el target. Los demás errores conservan su estado: `400` para una consulta o un target desconocidos,
  `401` sin un token válido.
- El rechazo se audita como `ACCESS_DENIED` con la causa `PERMISSION` en el canal `REST`.
- Un target o una consulta privados sin `permission` están abiertos a cualquier usuario autenticado de la API REST.
  Cuando la API REST esté expuesta, active [`require-undeclared`](#require-undeclared) para que esos targets requieran
  `default.read` o `default.write`.

## Servicios {#services}

Un `<service>` declara los permisos que requiere en el mismo atributo `permission`, con las mismas reglas: varios
permisos separados por espacios se requieren todos, y un permiso que no está en el catálogo es un error de carga.

```xml
<service id="ChgLogLvl" permission="system.update">
  ...
</service>
```

El permiso de un servicio se comprueba cuando lo llama una acción de cliente (las peticiones `/action/...`), con el
mismo [modo](#mode), rechazo y [auditoría](#audit) que una consulta o un target. Estas llamadas nunca se comprueban: las
llamadas del servidor desde código Java, los servicios de una consulta o un maintain de tipo `service` (se comprueba la
consulta o el target) y los servicios de una `launch-phase`.

Algunos servicios solo lanzan la consulta o el target de maintain que pide el cliente: `launchQuery`, `launchMaintain`,
`checkUnique`, `checkEmpty`, `launchColumnValues`, `subscribe` y `updateModel`. Están marcados con
`authorization="target"`: se comprueba la consulta o el target lanzado, no el servicio.

```xml
<service id="launchQuery" authorization="target">
  ...
</service>
```

Un servicio con `authorization="target"` no puede declarar `permission`: es un error de carga. Consulte
[Definición de servicios](../api/service-definition.md#service-element).

## Elementos de pantalla {#screen-elements}

Con `enforce`, AWE quita de una pantalla los botones, los botones de contexto y las columnas de botón
(`component="button"`) que lanzan una consulta, un target de maintain o un servicio que el usuario no puede lanzar. Lo
decide el servidor al enviar la pantalla, para cada usuario, así que los dos motores reciben la pantalla sin esos
elementos. Con `warn` y `off` la pantalla no cambia.

### Cómo oculta AWE los elementos de pantalla {#how-awe-hides-screen-elements}

| Pregunta | Respuesta |
| --- | --- |
| ¿Cuándo se ocultan elementos? | Solo con `enforce`. Con `warn` el usuario ve todos los elementos y un lanzamiento sin permiso se ejecuta: solo se escribe en el log y se [audita](#audit). Con `off` no se comprueba nada. |
| ¿Qué elementos? | Los botones, los botones de contexto y las columnas de botón de las tablas. |
| ¿Cuándo se quita un botón? | Cuando una de sus acciones lanza una consulta, un target de maintain o un servicio cuyo permiso el usuario no tiene. |
| ¿Y un target que no declara permiso? | No requiere nada, así que nunca quita un botón, salvo que esté activo [`require-undeclared`](#require-undeclared): entonces una consulta requiere `default.read` y un target de maintain o un servicio `default.write`. Los targets públicos y de solo servidor nunca requieren nada. |
| ¿Y si el botón no lanza nada? | Un botón que solo abre un panel, como *Nuevo* o *Duplicar*, no tiene target del que deducir: declara su propio [`permission`](#the-permission-attribute-of-an-element), que sustituye a la decisión deducida. |
| ¿Y los paneles? | Un panel se abre en solo lectura (**modo consulta**) cuando la acción que lo abre declara un [`edit-permission`](#view-mode-of-a-panel-edit-permission) que el usuario no tiene, o cuando se le quitaron todos sus botones de guardar. |
| ¿Esto es la seguridad? | No. Ocultar es solo presentación: el servidor comprueba cada lanzamiento, así que un botón oculto que alguien llama a mano se sigue rechazando. |

Un ejemplo: una pantalla de facturas y un usuario que solo tiene `invoices.read`.

```xml
<!-- Abre el panel para crear: no hay nada que deducir, así que declara su permiso -->
<button id="ButNew" label="BUTTON_NEW" permission="invoices.create">
  <button-action type="dialog" target="PnlInv" />
</button>
<!-- InvDel declara permission="invoices.delete" -->
<button id="ButDel" label="BUTTON_DELETE">
  <button-action type="server" server-action="maintain" target-action="InvDel" />
</button>
<!-- InvSnd no declara permiso -->
<button id="ButSnd" label="BUTTON_SEND">
  <button-action type="server" server-action="maintain" target-action="InvSnd" />
</button>
<!-- Abre el mismo panel para editar -->
<column name="EdtBtn" component="button" icon="lucide:pencil">
  <button-action type="dialog" target="PnlInv" edit-permission="invoices.update" />
</column>
```

| Elemento | Qué ve el usuario | Por qué |
| --- | --- | --- |
| *Nuevo* | Se quita | Su atributo `permission` pide `invoices.create`. |
| *Borrar* | Se quita | Lanza `InvDel`, que requiere `invoices.delete`. |
| *Enviar* | Se muestra | `InvSnd` no declara permiso. Con `require-undeclared` requiere `default.write` y se quita. |
| Lápiz | Se muestra y abre la factura en solo lectura | La apertura pide `invoices.update` en `edit-permission`. |

Las reglas en detalle:

- Un elemento se quita cuando una de sus `button-action` lanza un target cuyos permisos el usuario no tiene: el servicio
  de su `server-action`, o su `target-action` cuando ese servicio está marcado con `authorization="target"`.
- Un target sin `permission`, o con un permiso concedido siempre (`self`), nunca quita nada. Con
  [`require-undeclared`](#require-undeclared), un target privado sin `permission` requiere el permiso del recurso
  `default`, y el elemento lo sigue.
- Cuando los botones de contexto quitados dejan un `context-separator` al principio o al final del menú, o junto a otro
  separador, el separador también se quita.
- Solo deciden los permisos: el [acceso por pantalla](screen-access.md) de un target no quita su botón.

### El atributo `permission` de un elemento {#the-permission-attribute-of-an-element}

Un `button`, un `context-button` o una `column` de botón puede declarar `permission`, con las mismas reglas que en una
consulta (varios permisos separados por espacios se requieren todos, y un permiso que no está en el catálogo es un error
de carga). Sustituye a lo que se deduce de las acciones: el elemento se mantiene cuando el usuario tiene todos los
permisos, y se quita en caso contrario. Úselo para un elemento que no lanza ningún target, como un botón que solo abre
un panel, o para proteger un elemento con otros permisos que los de sus targets.

Sin él, un botón *Nuevo* que solo abre un panel sigue visible para un usuario que no puede crear, y el panel se abre
vacío en modo consulta. Los botones *Nuevo* y *Duplicar* de las pantallas de AWE declaran el permiso del target de
maintain que crea el registro:

| Pantallas | `permission` |
| --- | --- |
| Usuarios | `users.create` |
| Perfiles (*Nuevo* y *Duplicar*) | `profiles.create` |
| Temas | `themes.create` |
| Sitios, módulos, bases de datos, servidores de correo, colas, secuencias y parámetros de la aplicación (*Nuevo*, y *Duplicar* donde existe) | `system.update` |
| Tareas, panel, calendarios y servidores del planificador (`awe-scheduler`) | `scheduler.update` |
| Suscripciones (`awe-notifier`) | `subscriptions.create` |
| Gestor de literales (`awe-developer`) | `developer.update` |

Un test de descriptores de AWE (`CreateButtonPermissionTest`) comprueba que todo botón que abre un panel para crear lo
declara, así que una pantalla nueva no puede olvidarlo. Un test del mismo tipo mantiene al día las pantallas de su
aplicación.

### Modo consulta de un panel: `edit-permission` {#view-mode-of-a-panel-edit-permission}

Un diálogo o un panel lateral al que se le quitan todos los botones de guardar se abre en **modo consulta** desde todas
las acciones que lo abren, sin declarar nada. Un botón de guardar es un `button` o un `context-button` del panel con una
`button-action` que lanza un target de maintain, o un servicio que no está marcado con `authorization="target"`. Un
panel que no tiene ningún botón de guardar no se ve afectado, y tampoco un panel en el que el usuario conserva al menos
un botón de guardar: es el caso de un panel compartido para crear y para editar, para un usuario que puede crear pero
no editar, así que sigue necesitando `edit-permission`.

Un panel lateral o un diálogo que sirve tanto para crear como para editar solo conoce su modo a través de la acción que
lo abre. Una `button-action` de tipo `dialog` puede declarar `edit-permission`: cuando el usuario no tiene todos los
permisos que lista, esa apertura muestra el panel en **modo consulta**. Sus criterios son de solo lectura y ninguna
dependencia de la pantalla (incluida `set-readonly`) puede hacerlos editables mientras está abierto; la siguiente
apertura sin modo consulta vuelve a ser editable. Los botones del panel siguen la regla general, así que se quita un
botón de guardar cuyo target el usuario no puede lanzar.

```xml
<button-action type="dialog" target="PnlUsr" edit-permission="users.update" />
```

### Ejemplo: administración de usuarios {#example-user-administration}

La pantalla `user-admin` de AWE abre el mismo panel lateral `PnlUsr` para crear y para editar un usuario:

```xml
<!-- Solo abre el panel, así que declara el permiso de la creación -->
<button id="ButNew" label="BUTTON_NEW_USER" permission="users.create">
  ...
  <button-action type="dialog" target="PnlUsr" />
</button>

<!-- El lápiz de cada fila y el botón de contexto Editar abren el panel para editar -->
<column name="EdtBtn" component="button" icon="lucide:pencil" title="BUTTON_EDIT">
  ...
  <button-action type="dialog" target="PnlUsr" edit-permission="users.update" />
</column>
```

Un usuario que solo tiene `users.read` recibe la lista sin el botón **Nuevo usuario** y sin los botones de borrar,
desbloquear y restablecer el 2FA, y el lápiz abre el usuario en solo lectura, sin el botón de guardar. Un usuario con
`users.*` recibe la pantalla como antes.

:::caution Limitación
Un target lanzado con un nombre construido en tiempo de ejecución (código JavaScript o React propio) no se puede
deducir: su botón sigue visible y el servidor sigue rechazando el lanzamiento.
:::

## Asignación a perfiles {#assignment-to-profiles}

Un administrador asigna permisos en [la pantalla de perfiles](#profiles-screen). La tabla `AweProPer` los guarda, una
fila por perfil y permiso:

| Columna | Contenido |
| --- | --- |
| `IdeAweProPer` | Identificador de la fila (clave `ProPerKey` de `AweKey`) |
| `IdePro` | Perfil (`AwePro.IdePro`) |
| `Per` | Identificador de permiso o comodín. Único por perfil. |

`Per` contiene un permiso del catálogo o un comodín:

| Valor | Concede |
| --- | --- |
| `users.delete` | Ese permiso |
| `*` | Todos los permisos del catálogo |
| `users.*` | Todos los permisos de un recurso |
| `*.read` | Un verbo o una acción en todos los recursos |

Los comodines se expanden contra el catálogo, así que los permisos efectivos de un usuario son siempre una lista de
permisos reales. **No hay permisos negativos**: un perfil no puede quitar un permiso que da otra asignación.

La tabla guarda el identificador como texto, así que el catálogo sigue en el código. Una asignación que no coincide con
ningún permiso del catálogo se escribe en el log al arrancar y se ignora; nunca se borra, así que vuelve a aplicarse si
el permiso vuelve.

La migración de AWE `AWE_V1.6.0__authorization.sql` crea `AweProPer` (con su tabla de auditoría `HISAweProPer`) y da al
perfil de administrador (`ADM`) la asignación `*`. Para dar a un perfil la administración de usuarios y de perfiles:

```sql
-- Perfil con el acrónimo MGR: todos los permisos de los usuarios y de los perfiles
INSERT INTO AweProPer (IdeAweProPer, IdePro, Per)
SELECT k.KeyVal + 1, p.IdePro, 'users.*' FROM AwePro p, AweKey k WHERE p.Acr = 'MGR' AND k.KeyNam = 'ProPerKey';
INSERT INTO AweProPer (IdeAweProPer, IdePro, Per)
SELECT k.KeyVal + 2, p.IdePro, 'profiles.*' FROM AwePro p, AweKey k WHERE p.Acr = 'MGR' AND k.KeyNam = 'ProPerKey';
UPDATE AweKey SET KeyVal = KeyVal + 2 WHERE KeyNam = 'ProPerKey';
```

### La pantalla de perfiles {#profiles-screen}

El campo *Permisos* del panel lateral de la pantalla de [perfiles](../guides/default-screens.md#profiles) edita las
asignaciones de un perfil. Es una selección múltiple del catálogo de la aplicación (AWE, sus módulos y la propia
aplicación), en el idioma del usuario y agrupada por recurso:

| Grupo | Ofrece |
| --- | --- |
| *Todos los recursos* | *Todo* (`*`, todos los permisos) y cada verbo que usan varios recursos, en todos los recursos: *Consultar* es `*.read`. |
| Un grupo por recurso | *Todo* (`users.*`, todos los permisos del recurso), y después cada verbo y cada acción propia del recurso. Una acción propia, como *Desbloquear* de los usuarios, o un verbo de un solo recurso, como *Escribir* de *Por defecto*, solo se ofrece aquí. |

- Los permisos concedidos siempre (`self`) no se ofrecen: los tiene todo usuario que ha iniciado sesión.
- Guardar el perfil guarda los permisos del campo. Las asignaciones que el campo no ofrece, como un permiso que ya no
  está en el catálogo, se conservan.
- Los cambios se aplican en el **siguiente inicio de sesión** de cada usuario del perfil.
- Un usuario sin `permissions.read` no ve el campo, y sin `permissions.update` lo ve en solo lectura; el resto del
  perfil se puede guardar igualmente.
- Borrar un perfil borra sus asignaciones. Cada cambio se audita en `HISAweProPer`.
- El campo solo muestra las asignaciones. Los permisos del [fichero de perfil](#profile-files) no están en él: la
  pestaña *Permisos* de [Control de acceso](../guides/default-screens.md#permissions) muestra todos los permisos de un
  perfil con su origen.

## Ficheros de perfil {#profile-files}

Un perfil también puede recibir permisos de su **fichero de perfil**: el fichero `profile/<nombre>.xml` que indica la
*restricción* del perfil (`AwePro.Res`), el mismo fichero que guarda sus restricciones de pantallas. Un perfil sin
restricción usa el fichero por defecto (`awe.security.default-restriction`, `general` por defecto). El fichero de perfil
es código: se versiona y viaja con la aplicación, mientras que las asignaciones de `AweProPer` son datos de cada
entorno.

```xml title="profile/contributor.xml"
<profile xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/profile.xsd">
  <restriction option="users" />
  <restriction option="profiles" />
  <permission id="*" />
  <exclude id="users.*" />
  <exclude id="profiles.*" />
  <exclude id="permissions.*" />
</profile>
```

| Elemento | Atributo | Descripción |
| --- | --- | --- |
| `permission` | `id` | Un permiso del catálogo o un comodín (`*`, `users.*`, `*.read`), como en `AweProPer`. |
| `exclude` | `id` | Un permiso o un comodín que se resta de los permisos de este fichero. |

- Los permisos de un perfil son los permisos de su fichero **menos** sus exclusiones, **más** sus asignaciones.
- Una exclusión solo resta dentro de su propio fichero: no es una denegación. Una asignación del perfil, u otro perfil
  del usuario, pueden conceder el permiso. Las exclusiones solo existen en los ficheros de perfil.
- Un identificador que no coincide con ningún permiso del catálogo es un error de carga: el arranque falla y nombra el
  perfil y la entrada. En una recarga en caliente AWE conserva los perfiles anteriores.
- Solo los ficheros de perfil dan permisos: el fichero de restricción de un usuario no.
- `administrator.xml` no concede nada. El perfil de administrador (`ADM`) recibe `*` como asignación de la migración,
  así que un perfil de la aplicación que reutiliza ese fichero no recibe todos los permisos.

## Perfiles predefinidos {#predefined-profiles}

AWE incluye dos perfiles cuyos permisos están en sus ficheros de perfil:

| Perfil | Acrónimo | Fichero de perfil | Permisos |
| --- | --- | --- | --- |
| Lector (*Reader*) | `RDR` | `reader.xml` | `*.read`: consulta todos los recursos y no cambia nada. |
| Colaborador (*Contributor*) | `CTB` | `contributor.xml` | `*` salvo `users.*`, `profiles.*` y `permissions.*`. Su fichero también restringe las opciones *Usuarios* y *Perfiles*. |

- AWE los crea al arrancar cuando no existe ningún perfil con su acrónimo. Si existe, se conserva y se escribe un aviso
  en el log. `awe.security.permissions.predefined-profiles=false` desactiva la creación.
- Sus comodines también cubren los permisos de la aplicación: Lector consulta los recursos de su catálogo, y
  Colaborador los recibe todos.
- Son de solo lectura: la pantalla *Perfiles* los muestra sin *Guardar cambios*, y se rechazan la modificación y el
  borrado de un perfil predefinido (*El perfil predefinido ... no se puede modificar ni borrar. Duplícalo y modifica la
  copia.*).
- Para adaptar uno, **duplíquelo**: *Duplicar perfil*, en el panel o en el menú de una fila, convierte el panel en una
  copia con el acrónimo y el nombre vacíos, y *Crear copia* crea un perfil normal cuyas asignaciones son los permisos
  efectivos del original (fichero menos exclusiones, más asignaciones). La copia se cambia como cualquier otro perfil.
  No usa el fichero del perfil predefinido; elija su restricción si necesita una. *Duplicar perfil* sirve para
  cualquier perfil y requiere `profiles.create`.
- Un perfil es predefinido cuando tiene a la vez el acrónimo y el fichero de perfil de uno de ellos: un perfil de su
  aplicación con el acrónimo `RDR` y otro fichero es un perfil normal.

## Targets sin permiso: `require-undeclared` {#require-undeclared}

Por defecto, una consulta, un target de maintain o un servicio sin `permission` no se comprueba por permiso. Con
`awe.security.permissions.require-undeclared=true`, requiere el permiso del recurso `default` cuando lo lanza un
cliente:

| Target sin `permission` | Requiere |
| --- | --- |
| Consulta privada | `default.read` |
| Target de maintain privado | `default.write` |
| Servicio llamado por una acción | `default.write` |

- No afecta a: los targets `public` y [`server-only`](../api/maintain-definition.md#server-only-targets), los
  servicios marcados con `authorization="target"` (se comprueba el target que lanzan) y los lanzamientos del servidor.
- El recurso `default` está en el catálogo de AWE (*Por defecto · Consultar* y *Por defecto · Escribir* en la pantalla
  de perfiles). `*` incluye `default.write` y `*.read` incluye `default.read`, así que el administrador, Colaborador y
  Lector siguen funcionando.
- Los [elementos de pantalla](#screen-elements) lo siguen: un botón que lanza uno de esos targets se quita a los
  usuarios sin su permiso `default`.
- En modo `warn` el inventario del arranque sigue listando esos targets e indica que requieren `default.read` o
  `default.write`.
- Actívelo cuando la [API REST](#rest-api) esté expuesta: la API REST no se comprueba por acceso a pantalla, así que sin
  él cualquier usuario REST autenticado puede lanzar un target privado sin `permission`.

:::caution Antes de activarlo
- Todo perfil existente necesita `*` o `default.*` (o al menos `default.read`), o sus usuarios pierden todas las
  consultas y los targets que no declaran permiso.
- Mantenga el recurso `default` de AWE en el catálogo de la aplicación: sin él nadie puede tener `default.read` ni
  `default.write`, y no se puede lanzar ningún target sin permiso.
- Ejecute antes con `awe.security.authorization.mode=warn` y lea los rechazos.
:::

## Permisos efectivos {#effective-permissions}

Los permisos efectivos de un usuario son la unión de los permisos del perfil principal y de los
[perfiles adicionales](additional-profiles.md) activos, más los permisos concedidos siempre. Cada perfil aporta su
[fichero de perfil](#profile-files) menos sus exclusiones, más sus asignaciones. Se cargan al iniciar sesión, así que un
cambio en las asignaciones, en los ficheros de perfil, en los perfiles de un usuario o en el catálogo se aplica en el
siguiente inicio de sesión.

La pestaña *Permisos* de la pantalla de [control de acceso](../guides/default-screens.md#permissions) los muestra para
un usuario o un perfil, con el origen de cada uno.

## Autoridades de Spring Security {#spring-security-authorities}

Al iniciar sesión cada permiso efectivo se convierte en una autoridad de Spring Security `PERM_<id>`, junto a las
autoridades `ROLE_` de los perfiles. Los servicios de la aplicación pueden comprobarlas:

```java
@PreAuthorize("hasAuthority('PERM_users.delete')")
public void deleteUser(Integer userId) {
  // Only for the users with the users.delete permission
}
```

El usuario recibe las mismas autoridades `PERM_` en la sesión web, con el inicio de sesión local y con el inicio de
sesión web del [inicio de sesión único](authentication-sso.md), y con un token `local-jwt` de la
[API REST](../rest-module.md).

En el modo `oauth2-resource-server` de la API REST el usuario se construye a partir del token, sin un usuario en la
base de datos: los roles de su claim de perfil se asignan a perfiles de AWE como lo hace el
[inicio de sesión único](authentication-sso.md) (nombres de perfil, sin distinguir mayúsculas y minúsculas), y esos
perfiles reciben las autoridades `PERM_` que les da el inicio de sesión local, leídas en cada petición. Un rol que no es
un nombre de perfil de AWE no concede ningún permiso.

## Modo {#mode}

La comprobación sigue `awe.security.authorization.mode`, la misma propiedad que el
[acceso por pantalla](screen-access.md#configuration):

| Valor | Comportamiento |
| --- | --- |
| `enforce` (por defecto) | Un lanzamiento sin los permisos se rechaza con el error de *no autorizado*, se escribe en el log y se audita. |
| `warn` | El lanzamiento se escribe en el log y se audita (el detalle de la auditoría termina en `(allowed: warn)`), y se ejecuta. |
| `off` | Sin comprobación. |

Un lanzamiento rechazado siempre responde con el título *Acceso denegado* y el mensaje *No tiene permiso para realizar
esta acción. Si lo necesita, contacte con su administrador.*, nunca con la respuesta de error de la acción que lo lanzó,
así que un `control-unique-cancel` rechazado no se lee como un mensaje de *ya existe*. El mensaje nunca nombra la
consulta, el target o el servicio rechazado: ese identificador es un detalle técnico que solo queda en el log y en la
[auditoría](#audit).

Ejecute una aplicación existente primero con `warn` y lea los rechazos antes de usar `enforce`, como en el
[despliegue gradual del acceso por pantalla](screen-access.md#rollout).

En modo `warn` AWE también escribe en el log al arrancar el inventario de lo que falta por declarar: las consultas,
los targets de mantenimiento y los servicios que lanzan las pantallas del menú privado y que no declaran `permission`.
No se listan los targets públicos ni los de solo servidor, los de las pantallas del menú público ni los servicios con
`authorization="target"`. El log tiene un aviso con el total y una línea por tipo, y no se escribe nada cuando todos
los targets están declarados:

```text
3 targets used by private screens declare no permission (authorization mode is warn):
  query: CusLst, OrdLst
  service: exportOrders
```

Con [`require-undeclared`](#require-undeclared) la primera línea termina en `(authorization mode is warn, they require
default.read or default.write)`.

## Auditoría {#audit}

Un lanzamiento sin los permisos registra un evento `ACCESS_DENIED` de la [auditoría de seguridad](security-audit.md)
con la causa `PERMISSION` y el detalle `query: <id>`, `maintain: <id>` o `service: <id>`, y un aviso en el log. En modo
`warn` se escribe el mismo registro, su detalle termina en `(allowed: warn)` (por ejemplo
`maintain: InvDel (allowed: warn)`) y la petición continúa, así que la auditoría distingue un lanzamiento que `warn`
dejó pasar de uno rechazado. Los permisos del usuario nunca se escriben en el log ni se auditan.

## Propiedades {#properties}

| Propiedad | Por defecto | Descripción |
| --- | --- | --- |
| `awe.security.authorization.mode` | `enforce` | `off`, `warn` o `enforce`. Consulte [Modo](#mode). |
| `awe.security.permissions.predefined-profiles` | `true` | Crea al arrancar los [perfiles predefinidos](#predefined-profiles) Lector y Colaborador. |
| `awe.security.permissions.require-undeclared` | `false` | Exige `default.read` o `default.write` a los targets privados que no declaran permiso. Consulte [Targets sin permiso](#require-undeclared). |

También aparecen en la [referencia de propiedades](../properties#awe-security-properties).

## Recursos del catálogo {#catalog-resources}

AWE y sus módulos declaran un recurso por área de administración. Solo el perfil de administrador (`ADM`) los recibe de
la migración, y los [perfiles predefinidos](#predefined-profiles) de sus ficheros: dé a los demás perfiles los permisos
de las áreas que usan.

| Recurso | Permisos | Módulo | Cubre |
| --- | --- | --- | --- |
| `users` | `crud`, `unlock`, `reset-2fa` | AWE | Administración de usuarios |
| `profiles` | `crud` | AWE | Administración de perfiles |
| `permissions` | `read`, `update` | AWE | Asignaciones de permisos de los perfiles |
| `system` | `read`, `update` | AWE | Sitios, módulos, bases de datos, servidores de correo, colas, secuencias, parámetros de la aplicación, configuración de informes, log, herramientas de cifrado y mensajes a los usuarios |
| `access` | `read`, `update` | AWE | Acceso por pantalla y configuración de pantallas de perfiles y usuarios |
| `audit` | `read` | AWE | Auditoría de seguridad |
| `themes` | `crud` | AWE | Administración de temas |
| `self` | `read`, `update` (concedidos siempre) | AWE | Ajustes propios del usuario: datos de usuario, contraseña, 2FA, idioma, tema, disposición de las tablas, búsquedas guardadas, favoritos, notificaciones |
| `scheduler` | `read`, `update`, `execute` | `awe-scheduler` | Tareas, servidores y calendarios del planificador (`read`, `update`); lanzamiento de tareas y arranque y parada del planificador (`execute`) |
| `developer` | `read`, `update` | `awe-developer` | Gestor de literales y ruta de las herramientas de desarrollo |
| `sql-extractor` | `read`, `update` | `awe-tools` | Extractor SQL y sus ficheros guardados |
| `subscriptions` | `crud` | `awe-notifier` | Administración de suscripciones (las notificaciones de un usuario son `self`) |

Algunos selectores y sugerencias de las pantallas de administración declaran el permiso de su área: `DbsSel`, `DbsSug`,
`SitSel`, `SitSug`, `ModSelBD`, `ModSug`, `ThmSel`, `UsrActSel`, `UsrActSug`, `CatParTyp` y `FntLst` requieren
`system.read`, y `GetPrnLst` y `SelEmlSrvNom` requieren `users.read`. Una pantalla de la aplicación que los reutiliza
también necesita ese permiso, o una consulta propia.

## Permisos de las pantallas de AWE {#permissions-of-the-awe-screens}

Las pantallas de administración de usuarios y de perfiles de AWE declaran estos permisos:

| Permiso | Consultas | Targets de maintain |
| --- | --- | --- |
| `users.read` | `UsrLst`, `UsrChkUni`, `UsrStaCnt`, `UsrAddProSel`, `UsrAddProIni` | |
| `users.create` | | `UsrNew` |
| `users.update` | | `UsrUpd` |
| `users.delete` | | `UsrDel` |
| `users.unlock` | | `UsrUnl` |
| `users.reset-2fa` | | `reset2fa` |
| `profiles.read` | `ProLst`, `ProStaCnt`, `check-profile-acronym`, `ProPerAcc`, `ProPdfSel` | |
| `profiles.create` | | `ProNew`, `ProDup` |
| `profiles.update` | | `ProUpd` |
| `profiles.delete` | | `ProDel` |
| `permissions.read` | `profilePermissions`, `allProfilePermissions`, `ProPerCat`, `ProPerSel` | |
| `permissions.update` | | `ProPerUpd` (solo servidor) |
| `users.read` y `permissions.read` | `access-permissions`, `access-permission-counts`, `access-permission-summary`, `access-permission-why`, `access-permission-why-result` | |

`profilePermissions` y `allProfilePermissions` leen las asignaciones de `AweProPer`. AWE las lanza desde el servidor,
al iniciar sesión y al arrancar, así que nunca se comprueban; el permiso solo las protege de un cliente.

`ProNew` y `ProUpd` guardan los permisos del perfil mediante `ProPerUpd` solo cuando cambian, y entonces el usuario
también necesita `permissions.update`. `ProDel` borra las asignaciones del perfil, y cada cambio de `AweProPer` se
audita en `HISAweProPer`.
