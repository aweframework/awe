---
id: default-screens
title: Ventanas por defecto
sidebar_label: Ventanas por defecto
---

## Introducción {#introduction}

AWE tiene algunas pantallas comunes prediseñadas que son muy útiles para una primera versión de una aplicación. De esta manera, ya está implementada la gestión de [usuarios](#users) y [perfiles](#profiles), algunos [temas](#themes) útiles para personalizar la aplicación, gestión de seguridad como el [acceso a pantallas](#screen-access) y la [configuración de pantallas](#screen-configuration), y algunas herramientas de gestión a alto nivel como la [base de datos](#databases), la [configuración de módulos](#modules) y los [sitios](#sites), además de las [secuencias](#sequences) y los [parámetros de la aplicación](#application-parameters).

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
planificador. Muestra los servidores en una rejilla con su alias (y el host debajo), el usuario, la autenticación y el
estado, con una búsqueda por alias y filtros rápidos con sus recuentos (*Todos*, *Activos*, *Inactivos*). Cada
fila tiene un botón de edición y un menú (editar, duplicar, borrar), y las filas seleccionadas muestran una barra para
borrarlas.

Un panel lateral crea y edita un servidor, agrupado en secciones (identidad: alias, host y el selector *Activo*;
autenticación: su interruptor, el usuario y la contraseña) y muestra el alias y el estado del servidor que se está
editando. La contraseña guardada nunca se muestra: al editar, solo cambia si se escribe una nueva. *Duplicar* abre el
panel como un servidor nuevo con los valores del seleccionado, sin su contraseña, y el alias de un servidor es único.

La pantalla lee las consultas y los mantenimientos de servidores de correo del framework (`EmlSrvLst`, `EmlSrvNew`,
`EmlSrvUpd`, `EmlSrvDel` y `EmlSrvNamUni`) y añade `EmlSrvStaCnt`, `EmlSrvStaCntAct` y `EmlSrvStaCntIna` para los
recuentos de los filtros rápidos, de modo que una aplicación que los sobrescriba conserva su lógica.

:::warning Obsoleto
Las pantallas anteriores de servidores de correo (`EmlSrv`, `EmlSrvNew` y `EmlSrvUpd`) están obsoletas desde AWE 5.0.0 y
se eliminarán en la siguiente versión menor (5.1.0). Apunte sus propias opciones de menú a la pantalla
`email-server-admin`; consulte la [guía de migración a la v5](v5-migration.md).
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

## Acceso a las pantallas {#screen-access}

El acceso a pantallas contiene una lista de restricciones de opciones que se pueden definir para todos los usuarios de la aplicación, para un perfil concreto o incluso para un usuario concreto.

Puede buscar una restricción concreta con los criterios superiores, o actualizar

## Acceso al menú {#menu-access}

Esta es una nueva pantalla diseñada para mejorar la usabilidad de la restricción de opciones de menú. En esta pantalla hay un árbol de menú y tres criterios: `User`, `Profile` y `Module`. El árbol inferior se basa en las selecciones realizadas en los criterios: por ejemplo, el árbol inicial muestra el menú de restricciones para todos los usuarios y todos los perfiles, para las opciones que no pertenecen a ningún módulo.

Cuando se selecciona un módulo en los criterios, el árbol inferior muestra todas las opciones de menú de ese módulo, incluidas las opciones que no tienen módulo definido.

Si se elige un perfil en el criterio `Profile`, el árbol muestra las restricciones de ese perfil (y en la columna `access` se ven las restricciones generales para todos los usuarios y perfiles de cada opción).

Al elegir un usuario en el criterio `User`, el árbol muestra las restricciones de ese usuario. La columna `access` muestra las restricciones específicas del perfil del usuario en cada opción.

<img alt="Menu restrictions" src={require('@docusaurus/useBaseUrl').default('img/MenuRestrictionsScreen.png')} />

Para cambiar las restricciones de cualquier opción, se puede seleccionar la opción y hacer *clic derecho* sobre ella. Se mostrará un menú contextual con las opciones de restricción (permitir, restringir o eliminar la restricción en caso de estar definida). También se puede seleccionar la opción y pulsar los botones situados debajo del árbol para cambiar el valor de una restricción.

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