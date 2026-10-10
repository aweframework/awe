---
id: selenium-testing
title: Pruebas Selenium
sidebar_label: Pruebas Selenium
---

Este documento ofrece una visión básica sobre cómo empezar a desarrollar pruebas *Selenium* para aplicaciones desarrolladas con AWE. Antes de empezar a desarrollar pruebas, asegúrate de leer la *Guía de desarrollo de pruebas Selenium*, especialmente la sección *Consejos de optimización/ayuda*. Los aspectos básicos que debes conocer antes de empezar a desarrollar pruebas *Selenium*, como las configuraciones generales y la integración con *Jenkins*, no se tratan en este documento.

Todo el contenido de este documento está explicado asumiendo que el lector ya sabe cómo usar las herramientas y los comandos relativos a *Selenium*.

:::note Dónde encontrar cada cosa
- Escribir una prueba: [Instrucciones básicas](#basic-instructions), y después el [catálogo de pasos](#step-catalogue) para encontrar el paso que necesitas.
- Algo difiere entre los clientes AngularJS y React: [Clientes AngularJS y React](#angularjs-and-react-clients).
- Falta un paso o un hook: [Añadir un paso que falta](#adding-a-missing-step) y [Hooks de prueba estables](#stable-test-hooks-data-testid).
- Una prueba falla: [Ejecutar las suites y resolver problemas](#running-the-suites-and-troubleshooting).
- Detalles y ejemplos por componente: las secciones *Criterios* y [Celdas de tabla](#grid-cells) más adelante.
:::

:::tip
Es muy importante comentar todas las pruebas. **Cada bloque de comandos** relacionado con la interacción de un componente **DEBE empezar con un comentario** que indique qué intenta hacer la prueba.
:::

## Instrucciones básicas {#basic-instructions}

Actualmente nuestras definiciones de pruebas selenium se basan en **Java WebDrivers**.
Estos drivers permiten al desarrollador lanzar un navegador definido y acciones
sobre él para probar la interfaz de usuario.

Para desarrollar pruebas de integración con WebDrivers en AWE, sigue los siguientes pasos:

- Importa el paquete `awe-testing` en las dependencias de tu pom:

  ```xml
  <!-- Test dependencies -->
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-testing</artifactId>
    <scope>test</scope>
  </dependency>
  ```

- Define las pruebas de integración en el pom:

  ```xml
  <!-- Spring boot -->
  <plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
      ...
      <execution>
        <id>pre-integration-test</id>
        <configuration>
          <wait>1000</wait>
          <maxAttempts>180</maxAttempts>
        </configuration>
        <goals>
          <goal>start</goal>
        </goals>
      </execution>
      <execution>
        <id>post-integration-test</id>
        <goals>
          <goal>stop</goal>
        </goals>
      </execution>
      ...
    </executions>
  </plugin>
  ```

- Define el plugin de maven `failsafe` para lanzar las pruebas IT (de integración):

  ```xml
  <!-- Failsafe -->
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
      <execution>
        <id>integration-test</id>
        <goals>
          <goal>integration-test</goal>
        </goals>
      </execution>
    </executions>
  </plugin>
  ```

- ¡Y listo! Simplemente lánzalo con maven:

  ```
  mvn verify
  ```

### Definición de una prueba {#test-definition}

Para generar una prueba selenium basta con extender la clase `SeleniumUtilities` en tus clases de pruebas de integración (XxxxxXxxIT.class):

```java
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebsocketTestsIT extends SeleniumUtilities {

  /**
   * Log into the application
   * @throws Exception
   */
  @Test
  public void t000_loginTest() throws Exception {
    checkLogin("test", "test", "span.avatar-text", "Manager (test)");
  }

  /**
   * Log out from the application
   * @throws Exception
   */
  @Test
  public void t999_logoutTest() throws Exception {
    checkLogout(".slogan", "Almis Web Engine");
  }

  /**
   * Tests something
   * @throws Exception Error on test
   */
  @Test
  public void t001_myFirstTest() throws Exception {
    // Title
    setTestTitle("My first test");

    // Test things
    // ...
  }
}
```

### Establecer el título de la prueba {#set-test-title}

Toda prueba debería empezar con un título, para encontrar dónde empieza la prueba
en el archivo de log:

```java
// Test title
setTestTitle("My first test");
```

### Iniciar y cerrar sesión {#log-in-and-log-out}

Hemos creado dos métodos para simplificar las acciones de inicio y cierre de sesión:

Para iniciar sesión en la aplicación simplemente llama al método `checkLogin` con los siguientes
parámetros:
- **user** - Nombre de usuario
- **password** - Contraseña
- **userName** - Nombre que la aplicación muestra para el usuario autenticado

 ```java
 // Log in and check the name of the logged user
 checkLogin("test", "test", "Manager (test)");
 ```

 El paso no depende del cliente (AngularJS o React): sabe dónde muestra cada uno al usuario. También existe una
 forma con un selector CSS y un texto, `checkLogin("test", "test", "span.avatar-text", "Manager (test)")`, que ata la
 prueba a un único renderizado: prefiere la anterior.

 Para cerrar sesión en la aplicación simplemente llama al método `checkLogout`, que comprueba que se muestra la *pantalla de inicio de sesión*:

 ```java
 // Log out
 checkLogout();
 ```

 Si la aplicación pide una confirmación antes de cerrar sesión, usa `checkLogoutWithConfirmation()`. Para comprobar que
 la aplicación rechaza unas credenciales, usa `checkLoginRejected(user, password, messageType, title, message)`.

### Clases de prueba independientes {#independent-test-classes}

Las clases de prueba de las aplicaciones de test de AWE no dependen unas de otras, y cada prueba abre su propia pantalla y
sesión, de modo que una prueba que falla no deja a las siguientes clases sin un inicio de sesión o un módulo. Dentro de una clase, las pruebas de una
secuencia de crear, actualizar y eliminar sobre el mismo registro siguen siendo una cadena ordenada por los nombres `tNNN_`: la actualización necesita el
registro que creó la creación. Tres reglas mantienen las clases independientes:

- **Prepara la sesión antes de cada prueba, de forma idempotente.** `ensureLoggedIn("test", "test", "Manager (test)")` no hace nada
  cuando la aplicación ya muestra a ese usuario (el texto debe ser exactamente el que muestra el frontend, por ejemplo
  `Manager (test)`, y espera brevemente a un nombre que aún no está rellenado), cierra la sesión del otro usuario cuando hay otro autenticado,
  e inicia sesión cuando no hay nadie. `ensureModule("Test", "test")` selecciona el módulo solo cuando su opción de menú no se muestra, y
  `ensureLoggedOut()` cierra sesión solo cuando se muestra un usuario. Es seguro ejecutarlos antes de cada prueba. En las aplicaciones de test
  (`awe-tests/awe-boot` y `awe-tests/awe-boot-react`) una pequeña clase base, `AbstractSessionTests`, los ejecuta en un
  `@BeforeEach`; la extensión que abre el navegador se ejecuta antes, de modo que la primera prueba de una clase encuentra un navegador nuevo e
  inicia sesión. La clase elige el estado desde el que empiezan sus pruebas con un valor `Session` en su constructor (`LOGGED_IN`, o
  `TEST_MODULE` para las pantallas de prueba), y una prueba que comprueba ella misma el inicio de sesión o la selección de módulo declara un estado
  anterior con `@StartsFrom`:

  ```java
  class MatrixTestsIT extends AbstractSessionTests {

    MatrixTestsIT() {
      super(Session.TEST_MODULE);
    }

    // Starts from the login screen: ensureLoggedOut() leaves it there whatever the browser showed
    @Test
    @StartsFrom(Session.BLANK)
    void t000_loginTest() {
      checkLogin("test", "test", "Manager (test)");
    }
  }
  ```

- **Mantén una prueba real de inicio de sesión y una prueba real de cierre de sesión por clase** (`t000_...` y `t999_...`), porque comprueban esas
  funcionalidades. Las demás pruebas nunca dependen de ellas: la preparación inicia sesión de nuevo cuando la prueba de inicio de sesión no se ejecutó o falló.
- **Abre la pantalla en cada prueba** (`gotoScreen`) en lugar de continuar en la pantalla que dejó la prueba anterior, y
  usa datos propios: una clase crea los registros que actualiza y elimina, con nombres que ninguna otra clase usa
  (las pruebas del servidor de correo usan `auth server` y `plain server`, no el mismo nombre). Las tablas se buscan por el texto
  que contiene una fila, de modo que ningún nombre puede estar contenido en el nombre de un registro de otra clase (`Manual task` y
  `Prerequisite task`, no `Task` y `Task 2`). Un registro que una clase solo necesita como requisito previo (las pruebas de tareas programadas
  usan un calendario y una tarea manual para su lanzamiento personalizado y sus dependencias) lo crea esa clase al
  principio y lo elimina al final, después de los registros que lo usan. Las clases CRUD (`AbstractCrudTests`) hacen lo mismo: las clases de módulo y de base de datos crean su propio sitio, y la clase de usuario su propio perfil.
- **`gotoScreen` no recarga la pantalla que ya está abierta.** El cliente conserva sus criterios, sus recargas pendientes y
  la fila que seleccionó la prueba anterior, y una tabla que se recarga después pierde esa selección, de modo que una prueba que selecciona
  una fila justo después de otra en la misma pantalla puede fallar en un botón que permanece deshabilitado. Donde importe, pasa por
  otra pantalla primero (las pruebas del planificador lo hacen en `AbstractSchedulerTests.openScreen`). De la misma manera, una prueba no debe
  dejar trabajo en segundo plano ejecutándose hacia la siguiente: una tarea programada con dependencias las lanza cuando termina, por lo que las
  pruebas de tareas programadas la ejecutan antes de añadir las dependencias.
- **Una prueba que cambia algo global lo restaura incluso cuando falla.** Hazlo en un `@AfterEach` que se ejecute solo cuando
  la prueba no terminó su propia limpieza, con una bandera que la prueba activa antes del paso que puede cambiar el estado y
  desactiva tras el paso que lo restaura. `SchedulerManagementTestsIT` detiene y reinicia el único planificador de la
  aplicación, que las demás clases del planificador necesitan en ejecución: es una clase propia, cada prueba termina arrancando el
  planificador de nuevo, y su limpieza pulsa el botón de reinicio (funciona sea cual sea el estado del planificador) cuando la prueba
  falla en medio. `IntegrationTestsIT` elimina de la misma manera la configuración de pantalla que almacena.

Las clases que resultan de dividir una larga conservan su `@Tag`, de modo que se ejecutan en el mismo job de CI y no hace falta un nuevo job.
Un job de React lista sus clases en `TEST_CLASSES`: reemplaza el nombre antiguo por los nuevos (una prueba de la aplicación React
comprueba que cada clase listada existe).

### Ir a una nueva página {#go-to-a-new-page}

Para ir a una nueva página, tienes que llamar al método `gotoScreen` con una lista de
nombres de opciones correspondientes a las opciones de menú en las que necesitas hacer clic para ir a
la pantalla seleccionada. Por ejemplo, para ir a la pantalla `sites` necesitas hacer clic primero
en la opción de menú `tools`:

```java
// Go to sites screen
gotoScreen("tools", "sites");
```  

### Hacer clic en un botón {#click-on-a-button}

Para hacer clic en un botón simplemente llama al método `clickButton` con el nombre del botón como parámetro:

```java
// Click "ButSnd" button
clickButton("ButSnd");
```

Si el botón hace que se cargue una nueva pantalla, puedes indicar al método que espere después de la llamada al botón:

```java
// Click "ButNew" button and wait the screen to load
clickButton("ButNew", true);
```

Si el botón es un botón de búsqueda, puedes llamar al método `searchAndWait` para esperar a que la tabla se cargue
(si el botón se llama `ButSch`):

```java
// Search and wait
searchAndWait();
```

Si el nombre del botón es otro, simplemente indícalo como primer argumento:

```java
// Search and wait
searchAndWait("mySearchButton");
``` 

### Esperar a entradas y botones accionables {#wait-for-actionable-inputs-and-buttons}

Cuando un flujo personalizado necesita esperar hasta que un criterio pueda recibir entrada de forma segura o hasta que un botón se pueda pulsar, `SeleniumUtilities` expone dos ayudantes protegidos para las subclases:

- `waitForInputActionability(String criterionName)`
- `waitForButtonClickability(String buttonName)`

Estos ayudantes son genéricos y pueden reutilizarse en cualquier flujo de UI que necesite entradas accionables o botones pulsables.

```java
// Wait until a criterion can receive input
waitForInputActionability("cod_usr");

// Wait until a button is clickable
waitForButtonClickability("ButLogIn");
```

Úsalos cuando la UI renderiza el elemento pronto pero una actualización posterior del frontend aún está habilitando el control.

### Esperar a que una tabla se cargue {#wait-for-a-grid-to-load}

Para esperar a que una tabla se cargue, llama al método `waitForLoadingGrid`:

```java
// Wait a grid to load
waitForLoadingGrid();
``` 

### Aceptar un diálogo de confirmación {#accept-a-confirm-dialog}

Para confirmar un diálogo simplemente llama al método `acceptConfirm`:

```java
// Confirm a dialog
acceptConfirm();
``` 

### Comprobar y cerrar un mensaje {#check-and-close-a-message}

Cuando la aplicación devuelve un mensaje, puedes verificarlo y cerrarlo con el método `checkAndCloseMessage`:

```java
// Accept message
checkAndCloseMessage(messageType);
```

Donde **messageType** depende del mensaje que quieras verificar. Tienes los siguientes tipos de mensaje:
* **success** - mensaje verde, cuando todo va bien.
* **warning** - mensaje amarillo, hay una advertencia.
* **info** - mensaje azul, una notificación al usuario.
* **danger** - mensaje rojo, la aplicación ha fallado de algún modo.

### Combo: hacer clic en un botón, aceptar el diálogo de confirmación y cerrar el mensaje {#combo-click-button-accept-confirm-dialog-and-close-message}

Hay operaciones que normalmente se lanzan juntas.
La combinación de hacer clic en un botón (botón de confirmación), aceptar un diálogo
y cerrar un mensaje de éxito es una de ellas. Hemos creado una funcionalidad para hacer todas estas
acciones en un único método sencillo:

```java
// Store and confirm
clickButtonAndConfirm("ButCnf");
```

- Hace clic en el botón "ButCnf" (o el definido)
- Hace clic en aceptar el diálogo de confirmación
- Espera un mensaje **success** y lo cierra

## Catálogo de pasos {#step-catalogue}

Cada paso que puede llamar una prueba es un método `protected` de `SeleniumUtilities`, de modo que una clase de prueba que lo extienda los llama
directamente. Este catálogo los lista todos, agrupados por área, para que puedas revisarlo antes de escribir una prueba. Los nombres de
los parámetros siguen la firma Java: `criterionName` es el identificador del criterio, `gridId`, `rowId` y `columnId` son los identificadores de la tabla,
la fila y la columna, y `messageType` es `success`, `info`, `warning` o `danger`. La columna *Motor* está vacía cuando
el paso se comporta igual en el cliente AngularJS y en el React; en caso contrario indica qué difiere (consulta
[Clientes AngularJS y React](#angularjs-and-react-clients)).

Un paso que no está listado aquí no existe: añádelo (consulta [Añadir un paso que falta](#adding-a-missing-step)) en lugar de
escribir un selector en tu prueba. Esta lista la comprueba una prueba unitaria de `awe-testing` (`SeleniumTestGuideSyncTest`): la
compilación falla si un método público o protegido, no obsoleto, de `SeleniumUtilities` no se menciona en esta guía.

:::note Pasos obsoletos
Un paso que se reemplaza se marca como `@Deprecated` en el código fuente y se mantiene durante toda la línea 5.x, sin eliminación antes de
la 6.0 (consulta [Compatibilidad de la API](#api-compatibility-of-awe-testing)). Los pasos que reciben o devuelven un `By` de Selenium, y
`getDriver()`, están obsoletos en favor del mismo paso con un `Locator` (consulta
[Pasos personalizados sin tipos de Selenium](#custom-steps-without-selenium-types)):

| Obsoleto | Usar en su lugar |
|---|---|
| `waitForText(By selector, String contains)` | `waitForText(Locator selector, String contains)` |
| `waitForValue(By selector, String contains)` | `waitForValue(Locator selector, String contains)` |
| `waitForEmptyText(By selector, String text)` | `waitForEmptyText(Locator selector, String text)` |
| `waitForCssSelector(String cssSelector)` (returns a `By`) | `waitForCssLocator(String cssSelector)` (returns a `Locator`) |
| `writeText(By selector, CharSequence text)` | `writeText(Locator selector, CharSequence text)` |
| `writeTextOnDriver(By selector, CharSequence... text)` | `writeTextOnDriver(Locator selector, CharSequence... text)` |
| `checkVisible(By selector)` | `checkVisible(Locator selector)` |
| `checkNotVisible(By selector)` | `checkNotVisible(Locator selector)` |
| `getDriver()` | Los pasos de este catálogo, o `getBrowser()` con un `Locator` |

Los pasos CSS en bruto que reciben un `String` (`click(String cssSelector)`, `checkText(String, String)`...) no están obsoletos.
:::

### Sesión y navegación {#session-and-navigation}

| Paso | Qué hace |
|---|---|
| `goToUrl(String url)` | Abre una URL: el punto de partida de una suite |
| `setTestTitle(String title)` | Escribe el título de la prueba en el log. Empieza toda prueba con él |
| `checkLogin(String username, String password, String userName)` | Inicia sesión y comprueba el nombre que la aplicación muestra para el usuario autenticado. Forma preferida |
| `checkLogin(String username, String password, String cssSelector, String checkText)` | Inicia sesión y comprueba un texto dentro de un selector CSS. Forma con selector en bruto: prefiere la de tres argumentos |
| `ensureLoggedIn(String username, String password, String userName)` | No hace nada cuando la aplicación ya muestra a ese usuario (el texto exacto que muestra el frontend, como `Manager (test)`); cierra la sesión del otro usuario cuando hay otro autenticado, e inicia sesión (`checkLogin`) cuando no hay nadie. Úsalo en la preparación de una clase de prueba, no en la prueba que comprueba el inicio de sesión |
| `ensureLoggedOut()` | No hace nada cuando no se muestra ningún usuario; cierra sesión cuando hay uno, aceptando la confirmación cuando la aplicación la pide. Úsalo en la preparación de la prueba que comprueba el inicio de sesión |
| `checkLoginRejected(String username, String password, String messageType, String title, String message)` | Intenta iniciar sesión con credenciales que la aplicación rechaza y comprueba el mensaje que muestra |
| `checkLogout()` | Cierra sesión y comprueba que se muestra la pantalla de inicio de sesión. Forma preferida |
| `checkLogout(String cssSelector, String checkText)` | Cierra sesión y comprueba un texto dentro de un selector CSS. Forma con selector en bruto: prefiere `checkLogout()` |
| `checkLogoutWithConfirmation()` | Cierra sesión de una aplicación que pide primero una confirmación, la acepta y comprueba la pantalla de inicio de sesión |
| `gotoScreen(String... menuOptions)` | Hace clic en las opciones de menú en orden para abrir una pantalla, y espera a que el menú surta efecto |
| `waitForMenuOption(String option)` | Espera hasta que el clic en una opción de menú haya surtido efecto. `gotoScreen` ya lo llama |
| `checkMenuOption(String option, String text)` | Comprueba que una opción de menú es visible y contiene un texto |
| `selectModule(String moduleName)` | Abre el menú de ajustes (`ButSetTog`) y selecciona un módulo en el selector `module` |
| `ensureModule(String moduleName, String menuOption)` | No hace nada cuando la opción de menú que solo muestra ese módulo ya es visible; selecciona el módulo (`selectModule`) cuando no lo es |
| `broadcastMessageToUser(String user, String text)` | Va a la pantalla `tools > broadcast-messages` de las aplicaciones de test de AWE, envía un texto a un usuario y cierra los mensajes `success` e `info` |
| `invalidateSession()` | Invalida la sesión del usuario desde otra ventana, como si se hubiera cerrado en el servidor |

### Esperas y pausas {#waiting-and-pausing}

| Paso | Qué hace |
|---|---|
| `waitForInputActionability(String criterionName)` | Espera hasta que un criterio pueda recibir entrada |
| `waitForButtonClickability(String buttonName)` | Espera hasta que un botón se pueda pulsar |
| `waitForButton(String buttonName)` | Espera hasta que un botón es pulsable |
| `waitForTab(String tabCriterionName)` | Espera hasta que un criterio de pestañas es pulsable |
| `waitForContextButton(String buttonName)` | Espera hasta que un botón del menú contextual es pulsable |
| `waitForLoadingBar()` | Espera a que desaparezca la barra de carga de la página |
| `waitForLoadingGrid()` | Espera a que termine la carga de una tabla |
| `waitForText(String clazz, String contains)` | Espera un texto dentro de la etiqueta con una clase CSS. Forma con selector en bruto |
| `waitForText(Locator selector, String contains)` | Espera un texto dentro de un locator. Para ayudantes de tu producto |
| `waitForValue(Locator selector, String contains)` | Espera hasta que el valor de un input contiene un texto. Para ayudantes de tu producto |
| `waitForEmptyText(Locator selector, String text)` | Espera hasta que el valor de un input ya no contiene un texto. Para ayudantes de tu producto |
| `waitForCssLocator(String cssSelector)` | Espera un selector CSS y lo devuelve como un `Locator`. Forma con selector en bruto |
| `waitForText(By selector, String contains)` | Obsoleto: usa `waitForText(Locator, String)` |
| `waitForValue(By selector, String contains)` | Obsoleto: usa `waitForValue(Locator, String)` |
| `waitForEmptyText(By selector, String text)` | Obsoleto: usa `waitForEmptyText(Locator, String)` |
| `waitForCssSelector(String cssSelector)` | Obsoleto: devuelve un `By`. Usa `waitForCssLocator(String)` |
| `pause(Integer time)` | Duerme un número de milisegundos. Úsalo solo cuando no hay un estado que esperar |
| `showMouse()` | Muestra un puntero que sigue al ratón real, una ayuda visual para vídeos y capturas de pantalla |

### Botones, menús e info {#buttons-menus-and-info}

| Paso | Qué hace |
|---|---|
| `clickButton(String buttonName)` | Hace clic en un botón |
| `clickButton(String buttonName, boolean waitForLoadingBar)` | Hace clic en un botón y, cuando es `true`, espera a que la pantalla se cargue |
| `clickButtonAndConfirm(String button)` | Hace clic en un botón, acepta el diálogo de confirmación y cierra el mensaje `success` |
| `clickButtonAndConfirm(String button, String messageType)` | Igual, cerrando un mensaje de otro tipo |
| `checkButtonVisible(String buttonId)` | Comprueba que un botón se muestra, sea cual sea su estado |
| `checkButtonNotVisible(String buttonId)` | Comprueba que un botón no se muestra |
| `checkButtonDisabled(String buttonId)` | Comprueba que un botón se muestra y está deshabilitado |
| `clickInfoButton(String infoButtonName)` | Hace clic en un botón de info (el menú de usuario, la lista de módulos...) |
| `clickContextButton(String... contextButtonOptionList)` | Hace clic en las opciones de un menú contextual en orden |
| `closeContextMenu()` | Cierra el menú contextual abierto sin elegir una opción |
| `checkContextMenuNotVisible()` | Comprueba que no se muestra ningún menú contextual |
| `searchAndWait()` | Hace clic en `ButSch` y espera a que la tabla se cargue |
| `searchAndWait(String buttonName)` | Igual con otro botón de búsqueda |

### Mensajes y diálogos {#messages-and-dialogs}

| Paso | Qué hace |
|---|---|
| `acceptConfirm()` | Acepta el diálogo de confirmación y espera a que desaparezca |
| `checkAndCloseMessage(String messageType)` | Comprueba que se muestra un mensaje del tipo y lo cierra |
| `closeMessages(String messageType, int count)` | Cierra los mensajes de una pila uno a uno. Una pantalla con varios mensajes (uno por consulta fallida) mantiene sus controles bloqueados hasta que se cierran todos |
| `checkMessageTitle(String messageType, String text)` | Comprueba el título de un mensaje |
| `checkMessageText(String messageType, String text)` | Comprueba el texto de un mensaje |
| `checkMessageMissing(String messageType)` | Comprueba que no se muestra ningún mensaje del tipo |
| `checkDialogClosed(String dialogId)` | Comprueba que se ha cerrado un diálogo modal |
| `checkValidationErrorVisible()` | Comprueba que la pantalla muestra errores de validación |

### Criterios: escribir y leer {#criteria-writing-and-reading}

| Paso | Qué hace |
|---|---|
| `writeText(String criterionName, CharSequence text)` | Escribe un texto en un criterio, borrándolo antes |
| `writeText(String criterionName, CharSequence text, boolean clearText)` | Igual, eligiendo si se borra el criterio antes |
| `writeText(Locator selector, CharSequence text)` | Escribe un texto en un locator. Para ayudantes de tu producto |
| `writeTextOnDriver(Locator selector, CharSequence... text)` | Envía teclas a un locator como lo haría el navegador, sin desplazarse ni hacer pausas. Para ayudantes de tu producto |
| `writeText(By selector, CharSequence text)` | Obsoleto: usa `writeText(Locator, CharSequence)` |
| `writeTextOnDriver(By selector, CharSequence... text)` | Obsoleto: usa `writeTextOnDriver(Locator, CharSequence...)` |
| `clearText(String cssSelector)` | Borra el texto del input encontrado por un selector CSS. Forma con selector en bruto |
| `getText(String criterionName)` | Devuelve el texto de un criterio |
| `checkCriterionContents(String criterionName, String search)` | Comprueba el valor de un criterio de texto |
| `checkCriterionLabel(String criterionName, String text)` | Comprueba la etiqueta de un criterio |
| `checkCriterionUnit(String criterionName, String text)` | Comprueba el complemento de unidad de un criterio (el texto tras el input) |
| `checkCheckboxRadio(boolean isChecked, String... criteriaNames)` | Comprueba que unos criterios están marcados (o no) |
| `clickCheckbox(String criterionName)` | Hace clic en un checkbox o botón de radio |
| `clickCheckboxOption(String criterionName, String optionId)` | Hace clic en una opción de un grupo de botones (un checkbox o radio de botón dibujado como un solo grupo) |

### Criterios: fechas {#criteria-dates}

| Paso | Qué hace |
|---|---|
| `clickDate(String criterionName)` | Abre el selector de fechas de un criterio |
| `selectDate(String dateName, CharSequence dateValue)` | Elige una fecha completa |
| `selectDay(String dateName, Integer day)` | Elige un día del mes actual |
| `selectMonth(String dateName, String month)` | Elige un mes |
| `selectYear(String dateName, Integer year)` | Elige un año |
| `clickEnabledDatepickerDay()` | Hace clic en un día del selector de fechas abierto que se puede elegir |
| `getTodayDay()` | Devuelve el día del mes de hoy |
| `getTomorrowDay()` | Devuelve el día del mes de mañana |

### Criterios: select y suggest {#criteria-select-and-suggest}

| Paso | Qué hace |
|---|---|
| `selectFirst(String criterionName)` | Elige el primer valor de un select |
| `selectLast(String criterionName)` | Elige el último valor de un select |
| `selectContain(String criterionName, String label)` | Elige el valor de un select cuya etiqueta contiene un texto |
| `selectResult(String match)` | Elige un resultado de la lista de select abierta |
| `suggest(String criterionName, String search, String label)` | Escribe una búsqueda en un suggest y elige el resultado cuya etiqueta contiene un texto |
| `suggestLast(String criterionName, String search)` | Escribe una búsqueda en un suggest y elige el último resultado |
| `suggestResult(String match)` | Elige un resultado de la lista de suggest abierta |
| `suggestMultiple(String criterionName, String search, String label)` | Añade un valor a un select o suggest múltiple |
| `suggestMultiple(String criterionName, boolean clear, String search, String label)` | Igual, borrando antes los valores elegidos |
| `suggestMultipleList(String criterionName, String... items)` | Añade varios valores a un select o suggest múltiple |
| `openSuggest(String criterionName)` | Abre la lista de un select o suggest |
| `writeSuggestSearch(String criterionName, CharSequence text)` | Escribe en el cuadro de búsqueda de la lista abierta |
| `suggestReplacingSearch(String criterionName, String search1, String search2, String match, Integer pause)` | Escribe una búsqueda, la reemplaza por otra mientras la primera aún se está cargando y elige un resultado |
| `suggestMultipleReplacingSearch(String criterionName, String search1, String search2, String match, Integer pause)` | Igual, para un suggest múltiple |
| `checkSelectContents(String criterionName, String search)` | Comprueba el valor elegido de un **select** |
| `checkSuggestContents(String criterionName, String search)` | Comprueba el valor elegido de un **suggest**. El cliente lo muestra como texto (AngularJS) o en un input (React): el paso sabe cuál |
| `checkMultipleSelectorContents(String criterionName, String search)` | Comprueba un valor elegido de un select o suggest múltiple |
| `checkSelectNumberOfResults(String criterionName, Integer number)` | Abre un select y comprueba cuántos resultados lista |
| `checkSuggestResultCount(int expected)` | Comprueba cuántos resultados muestra la lista de select o suggest abierta |

Usa `checkSelectContents` para un select y `checkSuggestContents` para un suggest: un suggest no se dibuja como un select.

### Tablas: filas, celdas y columnas {#grids-rows-cells-and-columns}

Los pasos sobre una celda tienen dos formas: con un `rowId` (la celda de una fila dada) y sin él (la celda de la fila que se
está editando). La tabla lista una forma y dice "(también con `rowId`)" cuando existe la otra.

| Paso | Qué hace |
|---|---|
| `clickRowContents(String search)` | Selecciona la fila que contiene un texto |
| `clickRowContents(String gridId, String search)` | Igual en una tabla dada. Una fila que ya está seleccionada sigue seleccionada |
| `toggleRowContents(String gridId, String search)` | Hace clic en una fila aunque ya esté seleccionada, para alternar su selección |
| `editRow(String search)` | Inicia la edición de la fila que contiene un texto |
| `editRow(String gridId, String search)` | Igual en una tabla dada |
| `editRow(String gridId, String rowId, String columnId)` | Inicia la edición de una fila haciendo clic en una de sus celdas |
| `clickCell(String gridId, String columnId)` | Hace clic en una celda de la fila seleccionada |
| `clickCell(String gridId, String rowId, String columnId)` | Hace clic en una celda |
| `contextMenuRowContents(String search)` | Abre el menú contextual de la fila que contiene un texto |
| `contextMenuRowContents(String gridId, String search)` | Igual en una tabla dada |
| `contextMenu(String gridId, String rowId, String columnId)` | Abre el menú contextual de una celda |
| `selectAllRowsOfGrid(String gridId)` | Selecciona todas las filas con el checkbox de la cabecera |
| `checkAllRowsSelected(String gridId)` | Comprueba que todas las filas de una tabla están seleccionadas |
| `clickGridViewport(String gridId)` | Hace clic en la zona de la tabla que muestra las filas, fuera de cualquier fila |
| `sortGrid(String gridId, String columnId)` | Hace clic en la cabecera de una columna para ordenar |
| `scrollGrid(String gridId, int horizontal, int vertical)` | Desplaza la tabla en píxeles |
| `saveRow()` / `saveRow(String gridId)` | Hace clic en el botón de guardar de una tabla editable y espera |
| `getText(String gridId, String columnId)` | Devuelve el texto del editor de una celda de la fila que se está editando |
| `getText(String gridId, String rowId, String columnId)` | Devuelve el texto de una celda |
| `writeText(String gridId, String columnId, CharSequence text)` | Escribe en el editor de una celda de la fila que se está editando |
| `writeText(String gridId, String rowId, String columnId, CharSequence text)` | Escribe en el editor de una celda |
| `writeText(String gridId, String rowId, String columnId, CharSequence text, boolean clearText)` | Igual, eligiendo si se borra antes |
| `clickCheckbox(String gridId, String columnId)` | Hace clic en el checkbox o radio de una celda de la fila que se está editando |
| `clickCheckbox(String gridId, String rowId, String columnId)` | Hace clic en el checkbox o radio de una celda |
| `clickDate(String gridId, String columnId)` | Abre el selector de fechas de una celda (también con `rowId`) |
| `selectDate(String gridId, String columnId, CharSequence dateValue)` | Elige una fecha en una celda (también con `rowId`) |
| `selectDay(String gridId, String columnId, Integer day)` | Elige un día del mes actual en una celda (también con `rowId`) |
| `selectMonth(String gridId, String columnId, String month)` | Elige un mes en una celda (también con `rowId`) |
| `selectYear(String gridId, String columnId, Integer year)` | Elige un año en una celda (también con `rowId`) |
| `selectFirst(String gridId, String columnId)` | Elige el primer valor de una celda select (también con `rowId`) |
| `selectLast(String gridId, String columnId)` | Elige el último valor de una celda select (también con `rowId`) |
| `selectContain(String gridId, String columnId, String label)` | Elige un valor en una celda select (también con `rowId`) |
| `suggest(String gridId, String columnId, String search, String label)` | Sugiere en una celda (también con `rowId`) |
| `suggestLast(String gridId, String columnId, String search)` | Sugiere el último resultado en una celda (también con `rowId`) |
| `suggestMultiple(String gridId, String columnId, String search, String label)` | Añade un valor a una celda múltiple (también con `rowId`, y con `clear`) |

### Tablas: comprobaciones {#grids-checks}

| Paso | Qué hace |
|---|---|
| `checkRowContents(String... searchList)` | Comprueba que la tabla contiene todos los textos |
| `checkRowContentsGrid(String gridId, String... searchList)` | Igual en una tabla dada |
| `checkRowNotContains(String search)` | Comprueba que la tabla no contiene un texto |
| `hasRowContents(String search)` | Indica, sin fallar, si la tabla contiene un texto una vez cargada: un paso que crea o elimina un registro lo usa para averiguar si el registro ya está, de modo que pueda ejecutarse de nuevo tras un intento fallido |
| `hasRowContentsGrid(String gridId, String search)` | Igual en una tabla dada |
| `checkCellContents(String gridId, String rowId, String columnId, String search)` | Comprueba el contenido de una celda |
| `checkGridCellsHaveNoActiveContent(String gridId)` | Comprueba que las celdas de una tabla muestran texto y nada activo: ningún script, frame, imagen, formulario, manejador de eventos ni enlace a una url de script. Úsalo con un valor que lleve marcado para demostrar que la tabla no lo interpreta |
| `checkGridPresent(String gridId)` | Comprueba que una tabla existe en la pantalla, aunque esté oculta |
| `checkGridNotVisible(String gridId)` | Comprueba que una tabla no se muestra |
| `checkGridPageSize(String size)` | Comprueba el número de filas que una tabla muestra en cada página |
| `checkGridIconVisible(String gridId, String columnId, String icon)` | Comprueba que el icono de una columna de iconos se muestra (`plus`, sin el prefijo de la librería) |
| `checkColumnSuccessIcon(String columnId)` | Comprueba que una columna muestra un icono de éxito |

### Tablas en árbol {#tree-grids}

| Paso | Qué hace |
|---|---|
| `clickTreeButton(String gridId, String rowId)` | Expande o contrae una fila |
| `checkTreeRowVisible(String gridId, String rowId)` | Comprueba que una fila se muestra |
| `checkTreeRowNotVisible(String gridId, String rowId)` | Comprueba que una fila no se muestra |
| `checkTreeRowDeleted(String gridId, String rowId)` | Comprueba que una fila se muestra como eliminada (marcada para eliminar al guardar) |
| `checkTreeIconVisible(String gridId, String rowId)` | Comprueba que el icono de expandir/contraer de una fila se muestra |
| `checkTreeIconNotVisible(String gridId, String rowId)` | Comprueba que el icono de expandir/contraer de una fila no se muestra |

### Pestañas, asistentes, gráficos, listas de etiquetas y frames {#tabs-wizards-charts-tag-lists-and-frames}

| Paso | Qué hace |
|---|---|
| `clickTab(String tabName, String tabLabel)` | Hace clic en una pestaña de un criterio de pestañas. La etiqueta es la clave de locale (AngularJS) o el texto traducido (React) |
| `checkActiveWizardStep(String number)` | Comprueba el número del paso activo de un asistente |
| `checkTagListContains(String tagListId, String text)` | Comprueba que una lista de etiquetas (vista de texto) contiene un texto |
| `checkChartVisible(String chartId)` | Comprueba que un gráfico se muestra (el gráfico se ha dibujado) |
| `checkLogViewerContains(String text)` | Espera hasta que el visor de logs muestra un texto |
| `checkTextInEmbeddedFrame(String contentCssSelector, String text)` | Comprueba un texto dentro del frame que incrusta una aplicación externa. El selector pertenece a esa aplicación, por lo que es el único literal que puedes pasar |

### Comprobaciones en bruto {#raw-checks}

Estos pasos reciben un selector CSS. Existen para los ayudantes de tu producto y para componentes que AWE no conoce, no para las
clases de prueba: consulta [Mantener las pruebas de navegador libres de selectores](#keeping-browser-tests-free-of-selectors).

| Paso | Qué hace |
|---|---|
| `click(String cssSelector)` | Hace clic en un elemento |
| `checkText(String cssSelector, String text)` | Comprueba el texto completo de un elemento |
| `checkTextContains(String cssSelector, String text)` | Comprueba que el texto de un elemento contiene un texto |
| `checkTextNotContains(String cssSelector, String text)` | Comprueba que el texto de un elemento no contiene un texto |
| `checkPresence(String cssSelector)` | Comprueba que un elemento está presente |
| `checkVisible(String cssSelector)` | Comprueba que un elemento es visible (también con un `Locator`; la forma `By` está obsoleta) |
| `checkNotVisible(String cssSelector)` | Comprueba que un elemento no es visible (también con un `Locator`; la forma `By` está obsoleta) |
| `checkVisibleAndContains(String cssSelector, String search)` | Comprueba que un elemento es visible y contiene un texto |

## Compatibilidad de la API de `awe-testing` {#api-compatibility-of-awe-testing}

Los productos extienden `SeleniumUtilities` y compilan contra `awe-testing`, por lo que su **API pública y protegida es un contrato
de compatibilidad**: una nueva versión de `awe-testing` no debe romper una suite de pruebas que compilaba contra la anterior. Añadir miembros
o marcarlos como obsoletos (`@Deprecated`) está permitido; eliminar o cambiar la firma de un miembro público o protegido no lo está.

El contrato lo hace cumplir la compilación. El módulo `awe-testing` ejecuta [japicmp](https://siom79.github.io/japicmp/) en la
fase `verify` y compara el jar recién compilado con la última versión publicada, establecida por la
propiedad `awe-testing.api-baseline.version` en `awe-framework/awe-testing/pom.xml`. La compilación falla ante cualquier cambio
incompatible binario o de código fuente de un miembro público o protegido de una clase de `awe-testing`. El informe se escribe en
`awe-framework/awe-testing/target/japicmp/`.

La comprobación solo ve las clases de `awe-testing`: sus supertipos de terceros (Selenium, JUnit, Spring) no están en el
classpath de comparación. Un cambio que procede de esas librerías (por ejemplo una actualización de Selenium que cambia un tipo usado
en una firma) no lo detecta esta comprobación; aparece cuando se compila la suite del producto.

Si una ruptura es intencionada, documéntala en el changelog y añade una exclusión estrecha y comentada en ese pom (nunca una exclusión
general). Tras cada versión de `develop` (5.x), actualiza la propiedad de línea base a la versión recién publicada, de modo que la siguiente
versión se compruebe contra ella (consulta los pasos de publicación en `CONTRIBUTING.md`). Para ejecutar la comprobación por separado:

```
mvn verify -pl awe-framework/awe-testing -DskipTests
```

Usa `-Djapicmp.skip=true` para omitirla en local (por ejemplo cuando estás sin conexión y la línea base no está en tu repositorio local).

## Hooks de prueba estables (`data-testid`) {#stable-test-hooks-data-testid}

Los componentes de AWE exponen un pequeño conjunto fijo de atributos `data-testid`. Nombran la **parte** de un componente, nunca una
instancia, de modo que una prueba sigue funcionando cuando AWE reemplaza la librería que renderiza el componente (select2, datepicker,
ui-grid, Bootstrap...). Prefiere estos hooks a las clases de librería (`.select2-*`, `.datepicker-*`, `.ui-grid-*`, `.modal`,
`.nav-tabs`, `.fa-*`...) en tus propios selectores.

La instancia se identifica siempre con el atributo que AWE ya renderiza: `criterion-id` en el contenedor de un criterio,
`grid-id` / `tree-grid-id`, `row-id` y `column-id` en las tablas, `option-id` en un menú contextual, `name` en el menú de la
aplicación, `info-dropdown-id`, `dialog-id` y el `id` de botones y paneles. Algunas librerías añaden su DOM al final del
`<body>` (el desplegable del select, el popup del selector de fechas, los popovers). Esos elementos llevan `data-testid-owner="<component id>"`
para que una prueba pueda saber qué componente los abrió.

### Criterios {#criteria}

| `data-testid` | Elemento | Dónde encontrarlo |
|---|---|---|
| `criterion-input` | El control real del criterio: el `input`, `textarea`, checkbox, radio o input oculto, el valor de una vista de texto, o el selector de archivos de un uploader. En un select o suggest es el input oculto que contiene el valor: interactúa con `select`. Los editores de las celdas de una tabla editable también lo llevan | Dentro de `[criterion-id='X']`, o dentro de la celda de la tabla |
| `select` | Contenedor select2 visible de un select o suggest (el elemento en el que hacer clic) | Dentro de `[criterion-id='X']` |
| `select-value` | Valor elegido de un select simple | Dentro de `[criterion-id='X']` |
| `select-choice`, `select-choice-close` | Un elemento elegido de un select o suggest múltiple, y el enlace que lo elimina | Dentro de `[criterion-id='X']` |
| `select-search` | Input de búsqueda (el del desplegable para un select simple, el del contenedor para un select múltiple) | Desplegable, o dentro de `[criterion-id='X']` |
| `select-dropdown` | El desplegable abierto. Solo el desplegable abierto lleva el hook | Final de `<body>`, con `data-testid-owner` |
| `select-option` | Una opción del desplegable abierto | Dentro del desplegable, con `data-testid-owner` |
| `datepicker` | El popup de fecha abierto | Final de `<body>`, con `data-testid-owner` |
| `datepicker-day`, `datepicker-month`, `datepicker-year` | Celdas de día, mes y año del popup | Dentro del popup, con `data-testid-owner` |
| `upload-filename`, `upload-clear` | Nombre del archivo subido y botón que lo borra | Dentro de `[criterion-id='X']`, o dentro de la celda de la tabla |
| `loader` | Cargador de un criterio o de cualquier otro componente | Dentro del componente |
| `select-trigger` | La flecha que abre el panel de un select. Haz clic en ella en lugar de en el medio de un select corto, donde puede estar el icono de borrar. **Solo React** | Dentro de `[criterion-id='X']` |
| `criterion-unit` | El complemento de unidad de un criterio (el texto tras el input, como `EUR`). **Solo React** | Dentro de `[criterion-id='X']` |
| `criterion-error` | El error de validación de un criterio (AngularJS lo dibuja en el contenedor de errores compartido). **Solo React** | Dentro de `[criterion-id='X']` |

En un checkbox o radio de botones, el cliente React renderiza **un criterio para todo el grupo**, y cada opción es un
`criterion-input` que lleva `option-id` con su valor: `[criterion-id='X'] [data-testid='criterion-input'][option-id='Y']`.
AngularJS renderiza cada opción como un criterio propio. Usa `clickCheckboxOption(criterionName, optionId)`.

### Tablas y árboles {#grids-and-trees}

| `data-testid` | Elemento | Dónde encontrarlo |
|---|---|---|
| `grid` | Raíz de una tabla o tabla en árbol | `[grid-id='X']`, `[tree-grid-id='X']` |
| `grid-header-cell` | Celda de cabecera de una columna | Dentro de la tabla, con `column-id` |
| `grid-header-checkbox` | Etiqueta del checkbox "seleccionar todo". `data-selected` | Dentro de la tabla |
| `grid-viewport` | Zona desplazable de las filas. `data-container` es `body`, `left` o `right` (columnas fijas) | Dentro de la tabla |
| `grid-row` | Una fila, con `row-id`. `data-selected` | Dentro del viewport |
| `grid-cell` | Una celda: el elemento que lleva `column-id`, sea lo que sea lo que renderice (valor, editor, checkbox, icono de árbol) | Dentro de una fila, con `column-id` |
| `grid-row-checkbox` | Etiqueta del checkbox de selección de una fila. `data-selected` | Dentro de la celda |
| `grid-row-save`, `grid-row-cancel` | Botones de guardar y cancelar de una tabla editable | Dentro de la tabla |
| `grid-pagination`, `grid-page-previous`, `grid-page-next`, `grid-goto-page`, `grid-page-size` | Paginación del pie, sus flechas anterior/siguiente (`data-disabled`) del paginador compacto, el input "ir a la página" (solo AngularJS) y el select del tamaño de página. En React el tamaño de página lleva además `data-value`, porque el desplegable repite el texto | Dentro de la tabla |
| `grid-loader` | Cargador de la tabla (también de la tabla dinámica) | Dentro de la tabla |
| `tree-icon` | Icono de expandir/contraer de una fila de árbol. `data-expanded` y `data-loading` | Dentro de la celda |
| `tree-header-icon` | Icono de expandir/contraer todo de la cabecera. Solo AngularJS | Dentro de la tabla |
| `column-icon` | El icono de una columna de iconos. `data-icon` lleva el icono del valor de la celda (por ejemplo `fa-plus` en una tabla de multioperación) | Dentro de la celda |
| `grid-row-edit` | El botón que inicia la edición de una fila. **Solo React** | Dentro de la fila |

### Pestañas y asistentes {#tabs-and-wizards}

| `data-testid` | Elemento | Dónde encontrarlo |
|---|---|---|
| `tab-list` | Las cabeceras de pestañas de un criterio de pestañas. `data-disabled` | Dentro de `[criterion-id='X']` |
| `tab`, `tab-link`, `tab-label` | Una cabecera de pestaña (`li` con el id `tab-<value>` y `data-active`), el enlace en el que hacer clic y la etiqueta | Dentro de la lista de pestañas, o dentro del `tabdrop-menu` cuando la pestaña no cabe |
| `tab-pane` | Contenido de una pestaña, con el `id` del panel. `data-active` | Dentro de `[criterion-id='X']` |
| `tabdrop`, `tabdrop-toggle`, `tabdrop-menu` | El desplegable "más" que contiene las pestañas que no caben, el botón que lo abre y su menú. Solo AngularJS: la lista de pestañas de React no tiene menú "más" | Dentro de la lista de pestañas |
| `wizard-step`, `wizard-pane` | Una cabecera de paso (`data-active` y `data-completed`) y un panel de contenido (`data-active`) | Dentro de `[criterion-id='X']` |
| `wizard-step-number` | El número que muestra un paso. **Solo React**: el paso muestra un icono en lugar del número cuando tiene uno, y entonces `data-step-number` en el `wizard-step` lleva el número | Dentro del paso |

### Botones, menús e info {#buttons-menus-and-info-1}

| `data-testid` | Elemento | Dónde encontrarlo |
|---|---|---|
| `button` | El `<button>` de un botón, con el `id` del botón (también dentro de celdas de tabla) | Cualquier lugar |
| `context-menu`, `context-menu-option`, `context-menu-link`, `context-submenu` | Un menú contextual, una opción (con `option-id`), su enlace (`data-disabled`) y un menú anidado | Dentro del componente que posee el menú |
| `menu`, `menu-option`, `menu-link`, `menu-dropdown`, `menu-submenu` | El menú de la aplicación, una opción (`data-active`, `data-open`), su enlace (con `name`), el desplegable de primer nivel y los submenús anidados (`data-open`) | Dentro del menú |
| `info-dropdown`, `info-dropdown-toggle`, `info-dropdown-menu` | Un desplegable de info (con `info-dropdown-id`), el enlace que lo abre y su menú | Cualquier lugar |
| `info-button`, `info-button-link` | Un botón de info y su enlace (`info-button-link`: solo AngularJS) | Cualquier lugar |
| `avatar`, `avatar-name` | El avatar del usuario autenticado en la cabecera (lleva el id y el nombre de usuario como título) y el nombre junto a él. **Solo React** | Cabecera |

### Gráficos, listas de etiquetas y logs {#charts-tag-lists-and-logs}

| `data-testid` | Elemento | Dónde encontrarlo |
|---|---|---|
| `chart` | Un gráfico. Se identifica por `chart-id`, y `data-rendered` es `true` una vez dibujado el gráfico: espera a ello. **Solo React** (AngularJS: el `svg` de `[chart-id='X']`) | Dentro de la pantalla |
| `tag-list` | Una lista de etiquetas (vista de texto), identificada por `tag-list-id`. **Solo React** (AngularJS: `[awe-tag-list='X']`) | Dentro de la pantalla |
| `log-viewer` | El elemento que contiene el texto del visor de logs | Dentro de la pantalla |

Usa `checkChartVisible(chartId)`, `checkTagListContains(tagListId, text)` y `checkLogViewerContains(text)` en lugar de
estos selectores.

### Mensajes, diálogos y cargadores {#messages-dialogs-and-loaders}

| `data-testid` | Elemento | Dónde encontrarlo |
|---|---|---|
| `alert`, `alert-title`, `alert-message`, `alert-close` | Una alerta de la zona de alertas (`data-type` es `success`, `info`, `warning` o `danger`), su título, su texto y su botón de cerrar | Zona de alertas |
| `popover`, `popover-title`, `popover-content` | **Solo AngularJS**: el cliente React no tiene popover. El mensaje mostrado sobre un componente (`data-type`, y `data-testid-owner` con el componente al que apunta) | Final de `<body>` |
| `help-popover` | **Solo AngularJS.** La ayuda de un componente, renderizada una vez para toda la aplicación. Se muestra solo mientras `data-open` es `true` | Zona de alertas |
| `dialog`, `dialog-close` | Un diálogo modal (con `data-testid-owner` = id del diálogo, y `data-open`) y el botón de su cabecera que lo cierra | Dentro de `[dialog-id='X']` |
| `confirm-dialog`, `confirm-accept`, `confirm-cancel` | El diálogo de confirmación y sus botones | Final de la zona de alertas |
| `loader` | Un cargador de componente (criterios, columnas, selects). Las tablas usan `grid-loader` | Dentro del componente |
| `error-boundary`, `error-boundary-title`, `error-boundary-message`, `error-boundary-details`, `error-boundary-retry`, `error-boundary-reload` | **Solo React.** El panel que muestra un error boundary cuando una parte de la aplicación falla al renderizar (`data-scope` es `app` o `view`), su título, el mensaje del error, su pila de componentes y sus botones. Lee el mensaje y la pila del código fuente de la página de la evidencia del fallo para diagnosticar una pantalla en blanco | En lugar de la parte que falló (`view`) o de toda la aplicación (`app`) |
| `loading-bar`, `loading-spinner` | La barra de carga global (solo AngularJS) y su spinner. React no tiene barra de carga: muestra el spinner mientras se carga una vista. Existen solo mientras la aplicación se está cargando | Final de `<body>` |

### Estado {#state}

El estado que necesita una prueba se expone como atributos de datos, de modo que no dependa de clases de librería. Son siempre `"true"`
o `"false"`, excepto `data-type`, `data-container`, `data-icon`, `data-value` y `data-step-number`:

| Atributo | Significado |
|---|---|
| `data-selected` | Fila o checkbox seleccionado, celda del selector de fechas seleccionada |
| `data-active` | Pestaña, paso de asistente, asistente o panel de pestaña y opción de menú activos; la celda del selector de fechas que tiene el foco del teclado |
| `data-disabled` | Lista de pestañas, enlace de menú contextual, flecha de página anterior/siguiente o celda del selector de fechas deshabilitados |
| `data-open` | Opción de menú o submenú que está abierto; popover de ayuda que se muestra; diálogo que está abierto. Un diálogo vuelve a ser `false` solo cuando se ha eliminado su fondo (backdrop), de modo que la pantalla es interactiva |
| `data-expanded`, `data-loading` | Fila de árbol que está expandida, o que está cargando sus hijos |
| `data-completed` | Paso de asistente que ya está hecho |
| `data-outside-month` | Día del selector de fechas que pertenece al mes anterior o siguiente |
| `data-type` | Tipo de un mensaje |
| `data-container` | Contenedor del viewport de una tabla: `body`, `left` o `right` |
| `data-icon` | Clases de icono que muestra una columna de iconos. Haz coincidir una con `[data-icon~='fa-plus']` |
| `data-editing` | Fila de tabla que se está editando. **Solo React**: el cliente React puede editar una fila que no está seleccionada (AngularJS edita la fila seleccionada) |
| `data-deleted` | Fila de tabla marcada para eliminar cuando se guarda la tabla (tablas de multioperación y tablas en árbol). **Solo React**. Consulta `checkTreeRowDeleted(gridId, rowId)` |
| `data-rendered` | Un gráfico que se ha dibujado. **Solo React** |
| `data-step-number` | Número de un paso de asistente (un texto, no un booleano). **Solo React** |
| `data-value` | Valor de un control cuyo texto lo repite, como el tamaño de página de una tabla (un texto, no un booleano). **Solo React** |

```java
// Value of the input of a criterion
Locator input = Locator.css("[criterion-id='Txt'] [data-testid='criterion-input']");

// Option "Yes" of the open dropdown of the criterion "Sta"
Locator option = Locator.css("[data-testid='select-option'][data-testid-owner='Sta']");

// Enabled days of the current month in the popup of the criterion "Cal"
Locator days = Locator.css("[data-testid='datepicker'][data-testid-owner='Cal'] "
  + "[data-testid='datepicker-day'][data-outside-month='false'][data-disabled='false']");

// Cell "name" of the selected rows of the grid "Grd"
Locator cell = Locator.css("[grid-id='Grd'] [data-testid='grid-row'][data-selected='true'] "
  + "[data-testid='grid-cell'][column-id='name']");

// Active tab of the criterion "Tab" and the danger alerts
Locator tab = Locator.css("[criterion-id='Tab'] [data-testid='tab'][data-active='true']");
Locator danger = Locator.css("[data-testid='alert'][data-type='danger']");
```

El vocabulario vive en una constante JavaScript para cada cliente: `TestIds` y `TestAttributes` en
`awe-client-angular` (`js/awe/data/testIds.js`, disponible también como constante de AngularJS) y en `awe-client-react`
(`src/utilities/testIds.js`). El mismo concepto usa el mismo valor en ambos, y las constantes Java de `awe-testing`
(`TestIds`, `TestAttributes`) los reflejan: `TestIdsVocabularyTest` falla si se desvían. Una parte que solo renderiza un cliente
se marca como **Solo React** o **Solo AngularJS** en las tablas anteriores. Los hooks son aditivos: no se elimina ninguna clase, id o
atributo existente.

Rara vez necesitas estos hooks en una prueba: un [paso](#step-catalogue) ya los usa. Son para los ayudantes de tu
producto y para un componente para el que AWE aún no tiene paso (entonces plantéate [añadir el paso](#adding-a-missing-step)).

## Clientes AngularJS y React {#angularjs-and-react-clients}

AWE 5 tiene dos clientes web, y la aplicación de test de cada uno ejecuta su propia suite (`awe-tests/awe-boot` para AngularJS,
`awe-tests/awe-boot-react` para React). El cliente bajo prueba se establece con `awe.test.frontend` (`angular` por defecto, `react`).
Los pasos de `SeleniumUtilities` ocultan las diferencias, de modo que el mismo paso funciona en ambos. Estas son las diferencias con las que
un autor de pruebas todavía se encuentra, porque son visibles en las pantallas y no en el renderizado:

| Tema | AngularJS | React | Qué hacer en una prueba |
|---|---|---|---|
| Cierre de sesión | El botón de cerrar sesión es visible en el shell | El botón de cerrar sesión está dentro del menú del avatar, que `checkLogout()` abre | Usa `checkLogout()`. Si la aplicación pide una confirmación (la aplicación de referencia de React lo hace), usa `checkLogoutWithConfirmation()` Los pasos `ensureLoggedIn` y `ensureLoggedOut` eligen la variante por sí mismos |
| Etiqueta de pestaña | `clickTab` coincide con la **clave de locale** de la etiqueta (`ENUM_MATRIX_EDITABLE`) | `clickTab` coincide con el **texto traducido** que ve el usuario (`Editable`) | Pasa el valor del cliente que pruebas. Si una suite apunta a ambos, mantén la etiqueta en una constante por suite |
| Valor de suggest | El valor elegido se muestra como texto | El valor elegido es el **valor de un input** | Usa `checkSuggestContents` para un suggest y `checkSelectContents` para un select; nunca leas el valor con un selector |
| Grupos de botones | Cada opción de un checkbox o radio de botón es un criterio propio | Un grupo de botones es **un criterio** y cada opción lleva su `option-id` | Usa `clickCheckboxOption(criterionName, optionId)`, y `checkCheckboxRadio(...)` para el estado. Un grupo puede no tener ninguna opción marcada por defecto |
| Fila editada y fila seleccionada | La fila que editas es la fila seleccionada | Una fila puede **editarse sin estar seleccionada** (`data-editing`), y una tabla multiselección alterna la selección cuando haces doble clic para editar | Usa `editRow(...)` y los pasos de celda; usa `toggleRowContents(gridId, search)` cuando necesites que la selección cambie aunque la fila estuviera seleccionada |
| Gesto de edición | Un solo clic en la fila | Doble clic, reintentado hasta que el editor está abierto | Usa `editRow(...)`. No hagas clic tú mismo en la celda |
| Números | Nada especial: escribe el valor | El input numérico (`InputNumber` de PrimeReact) toma un `.` o `,` escrito como su **separador decimal** y añade él mismo el separador de miles | Escribe solo los dígitos y el separador decimal, no el separador de miles: `writeText("Unt", "325274,50")`, y después comprueba `325.274,50` con `checkCriterionContents` |
| Errores de criterio | Se muestran en el contenedor de errores compartido | Se muestran bajo el criterio (`criterion-error`) | Usa `checkValidationErrorVisible()` |
| Popovers de info | `popover`, `help-popover` | Ninguno | Usa `checkMessageTitle` y `checkMessageText` para los mensajes |
| Pestañas que no caben | Un menú "más" (`tabdrop`) las contiene | Sin menú "más" | `clickTab` abre el menú cuando el cliente tiene uno |
| Barra de carga | Una barra de carga en la parte superior de la página | Solo un spinner mientras se carga una vista | Usa `waitForLoadingBar()`, que sabe qué esperar |
| Tamaño de página de impresión | Un selector nativo | Un desplegable con un selector oculto: el texto repite el valor, por lo que el tamaño de página se expone en `data-value` | Usa `checkGridPageSize(size)` |

Cuando un comportamiento difiere y ninguna fila anterior ayuda, no bifurques según el cliente en tu prueba: añade un paso (consulta
[Añadir un paso que falta](#adding-a-missing-step)) y deja que decida el perfil de motor de cada cliente.

### Locators neutrales respecto a la herramienta (vista previa) {#tool-neutral-locators-preview}

Los perfiles de motor (`IAweFrontEndInstructions`) siguen devolviendo `By` de Selenium en AWE 5.0, por lo que nada cambia para tus pruebas.
Los pasos que reciben un `By` tienen un gemelo que recibe un `Locator`: consulta [Pasos personalizados sin tipos de Selenium](#custom-steps-without-selenium-types).
Para que las pruebas de navegador puedan ejecutarse más adelante en otra herramienta de automatización sin reescribirse, `com.almis.awe.testing.driver.Locator`
es un locator inmutable y neutral respecto a la herramienta (`Locator.css(...)` o `Locator.xpath(...)`, con `kind()` y `expression()`). Se obtiene uno
a partir de un `By` con `Locator.from(by)` (o `Locator.from(listOfBy)`): CSS y XPath se mantienen tal cual y `By.id` se convierte en el
selector CSS de id equivalente, con escapes. Otros tipos de `By` (name, class name, link text, tag name) se rechazan con una
`IllegalArgumentException`, porque los perfiles no los usan. `toBy()` vuelve a Selenium. Una prueba unitaria comprueba que todos los
locators de los perfiles de AngularJS y React se convierten. `Locator` es una vista previa: sus métodos de conversión a Selenium pueden pasar al
adaptador de Selenium en una versión posterior.

Detrás de él, `com.almis.awe.testing.driver.BrowserDriver` es el puerto interno y neutral respecto a la herramienta sobre el que se ejecutará `SeleniumUtilities`, con
`SeleniumBrowserDriver` como su adaptador de Selenium (disponible también desde `SeleniumModel.getBrowser()`). Es una vista previa sin
promesa de compatibilidad todavía: escribe tus pruebas con los pasos de `SeleniumUtilities`, no contra el puerto. Además de consultas y
acciones cubre la apertura de páginas, scripts, desplazamiento, frames, tamaño de ventana, evidencia de fallos (captura de pantalla, código fuente de la página, consola
del navegador) y el cierre del navegador.

### Herramienta de automatización (`awe.test.tool`) {#automation-tool-awetesttool}

La herramienta que controla el navegador se elige con `awe.test.tool`. Sus valores son `selenium` (el valor por defecto, por lo que nada cambia para
las suites existentes) y `playwright`, que es un **piloto** para proyectos propios (ver más abajo) aunque es la herramienta de
navegador bloqueante del pipeline de AWE. El nombre no distingue mayúsculas de minúsculas, y un valor que no es una herramienta soportada detiene las pruebas al arrancar con un mensaje que lista las soportadas.
`getDriver()` es la única parte de la API que pertenece a Selenium: devuelve el driver de Selenium con `selenium` y lanza una
`UnsupportedOperationException` con cualquier otra herramienta, así que escribe tus propios pasos con los pasos neutrales y `getBrowser()`.

```
mvn -f awe-tests/awe-boot-react/pom.xml verify -Dskip.junit=true -Dskip.selenium=false \
  -Dawe.test.tool=selenium -Dawe.test.browser=headless-chrome -Dit.test=SchedulerCalendarTestsIT
```

#### Playwright {#playwright-pilot}

`awe.test.tool=playwright` ejecuta las mismas pruebas con [Playwright for Java](https://playwright.dev/java/), que forma parte de
`awe-testing` (no hay nada más que añadir a tu proyecto). Los pasos se comportan como con Selenium: las mismas pausas, los
mismos desplazamientos antes de un clic, consultas que responden de inmediato y son sondeadas por los pasos. Para tus propios proyectos es nuevo: prueba tus
suites con él e informa de lo que difiere. El pipeline de AWE ejecuta sus suites de navegador con él: los jobs de Playwright bloquean las merge
requests, `develop` y `master` (ver más abajo).

El navegador se elige con el habitual `awe.test.browser`; Playwright ejecuta los navegadores que instala él mismo, no los de tu
máquina:

| `awe.test.browser` | Navegador de Playwright |
|---|---|
| `chrome` | Chromium, con ventana |
| `headless-chrome` | Chromium, sin ventana |
| `firefox` | Firefox, con ventana |
| `headless-firefox` | Firefox, sin ventana |

Cualquier otro valor (`edge`, `opera`, `ie`, `remote-*` y todo navegador `service-*`) detiene las pruebas al arrancar con un mensaje:
**los navegadores remotos y de servicio aún no están soportados por el piloto de Playwright**, ya que son grids de Selenium e imágenes Docker
que Playwright no usa. Usa `awe.test.tool=selenium` para ellos. `awe.test.browser-width` y `awe.test.browser-height` establecen
el viewport y `awe.test.timeout` el timeout por defecto de Playwright.

Playwright **descarga sus navegadores** (Chromium, Firefox y WebKit, aproximadamente 1 GB) en `~/.cache/ms-playwright` (en macOS
`~/Library/Caches/ms-playwright`) la primera vez que una ejecución de pruebas que lo elige inicia Playwright. Para instalarlos de antemano, por
ejemplo en la imagen de un job de CI, o para instalar solo el que necesitas, ejecuta su línea de comandos desde tu proyecto, que no necesita
ninguna instalación propia de Playwright (`--with-deps` instala también las librerías del sistema en Linux; `--only-shell` instala solo el
headless shell de Chromium, aproximadamente 200 MB, que es todo lo que necesita `headless-chrome`):

```
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"
```

Establece `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` para evitar que descargue nada por su cuenta, y `PLAYWRIGHT_BROWSERS_PATH` para
mantener los navegadores en otro lugar, por ejemplo en una carpeta que cachea la CI.

```
mvn -f awe-tests/awe-boot-react/pom.xml verify -Dskip.junit=true -Dskip.selenium=false \
  -Dawe.test.tool=playwright -Dawe.test.browser=headless-chrome -Dit.test=SchedulerCalendarTestsIT
```

Las pruebas unitarias de `awe-testing` que ejecutan un Chromium real mediante Playwright nunca descargan un navegador: usan el que está
instalado y se **omiten** donde no hay ninguno, de modo que una máquina sin navegadores sigue obteniendo una compilación en verde. El job de CI de las
pruebas unitarias (`All UT`) instala solo el headless shell de Chromium con sus librerías, lo cachea entre pipelines y pasa
`-Dawe.test.playwright.required=true`, que convierte un navegador ausente en un **fallo** en lugar de una omisión; usa el mismo interruptor
para asegurarte de que las pruebas se ejecutan en tu máquina. La CI demuestra Chromium en las pruebas del adaptador de `All UT`; las
suites de Playwright siguientes ejecutan Chromium y Firefox. Para comprobar el Firefox del adaptador en tu máquina, ejecuta las mismas pruebas unitarias con
`-Dawe.test.playwright.engine=firefox` (un Firefox que está instalado y falla al arrancar hace fallar la prueba; uno que no está
instalado la omite).

**Jobs de Playwright en el pipeline de AWE.** El pipeline de AWE ejecuta las mismas suites que los jobs de Selenium con
`awe.test.tool=playwright`: 4 jobs (`Playwright IT 1/4` a `Playwright IT 4/4`), cada uno una matriz de los 4 grupos de suites de
su homólogo de Selenium, lo que hace **16 jobs**. Los dos primeros (Chromium y Firefox) ejecutan la aplicación AngularJS
(`awe-tests/awe-boot`, los mismos grupos que `Selenium IT 1/4` y `2/4`) y los otros dos la aplicación React
(`awe-tests/awe-boot-react`, las clases de `Selenium IT 3/4` y `4/4`). Los nombres terminan en `n/4` para que el grafo del pipeline
muestre cada herramienta como un grupo (GitLab agrupa los jobs cuyos nombres terminan en un número sobre un total, y los jobs de una matriz, por el
resto del nombre); la primera variable de cada entrada de la matriz es `TARGET` (navegador y motor), de modo que un job fallido se lee
`Playwright IT 3/4: [chromium-react, ..., playwright-react-chromium-scheduler]`. Los
cuatro jobs de una herramienta no pueden ser un solo job con el navegador y el motor como variables de matriz: los jobs de React tienen sus propias
`rules` (se ejecutan en una merge request solo cuando cambia el backend o el cliente React) y las reglas de un job no ven las
variables de la matriz. Los jobs de Selenium tienen cada uno un servicio de navegador distinto.
Dos jobs pequeños, `Playwright Chromium browser` y `Playwright Firefox browser`, descargan y cachean los navegadores para ellos.
Cada job instala solo el navegador headless que necesita, lo ejecuta junto a la aplicación que arranca y escribe su evidencia
de fallos (captura de pantalla, código fuente de la página, consola, [trace y vídeo](#playwright-evidence)) en `browser-evidence/`, que encuentras
en los artefactos del job como la evidencia de los jobs de Selenium; el grabador de pantalla de Selenium está desactivado
(`awe.test.allowed-recording=false`), ya que Playwright graba la propia página.

- **Cuándo se ejecutan.** Automáticamente en cada merge request con cambios de código, en `develop` y en `master`. No se ejecutan en
  `support/*`, porque `support/4.x` no tiene adaptador de Playwright. Los jobs de Selenium (`Selenium IT 1/4` a `4/4`) se ejecutan en `master`, en `support/*` y en la planificación semanal del pipeline "Weekly Check" en `develop`, no en merge
  requests ni en pushes ordinarios.
- **Bloquean el pipeline.** Un job de Playwright que falla hace fallar el pipeline, detiene `Launch Sonar` y los jobs de publicación, e
  impide que Renovate haga automerge, exactamente igual que un job de Selenium. Tienen la misma [reejecución por prueba](#a-failed-test-is-rerun-alone)
  y el mismo reintento de job (#766), y lo mismo para Firefox que para Chromium. Sus archivos jacoco tienen un nombre distinto por job
  (`jacoco-${TEST_NAME}-it.exec`), de modo que la cobertura de ambas herramientas se conserva cuando se ejecutaron ambas.
- **Cómo comparar.** En `master` y en el pipeline planificado semanal se ejecutan ambas herramientas. Abre el job de Playwright y el job de Selenium
  de la misma suite y lee el resumen de failsafe (`Tests run: ...`) y la línea `Playwright ...` que el job de Playwright
  imprime al final de su log, con su estado y el tiempo desde que empezó el job.

Chromium se inicia con `--disable-dev-shm-usage` (la memoria compartida de un contenedor es demasiado pequeña para él) y, cuando el
sandbox no puede funcionar, sin su sandbox (`--no-sandbox`): ese es el caso cuando se ejecuta headless, cuando el proceso se ejecuta como
root, y dentro de un contenedor (Docker, Podman o Kubernetes, que se detectan por `/.dockerenv`, `/run/.containerenv` o
`KUBERNETES_SERVICE_HOST`). Un Chromium que un usuario muestra en un escritorio mantiene su sandbox. Establece
`awe.test.playwright.no-sandbox=false` para mantener el sandbox en cualquier caso, o `=true` para eliminarlo en cualquier caso; si se deja vacío se
aplica la regla anterior.

Diferencias que puedes notar respecto a Selenium:

- Una ventana no se puede mover (una página de Playwright tiene un viewport, no una ventana en una pantalla): la posición se ignora, con un
  log de depuración.
- Un script se abandona cuando la página no responde dentro del timeout de scripts (30 segundos, como en Selenium), de modo que una página cuyo
  hilo de scripts está bloqueado hace fallar el paso con una `ScriptTimeoutException` en lugar de colgar la ejecución. La consola de la evidencia
  de fallos espera dos segundos como máximo a una página así y devuelve lo que tenía. Un script que agotó su tiempo puede seguir ejecutándose en
  la página (no se puede decir al navegador que lo detenga), y la página permanece bloqueada hasta que termina; un script que no había comenzado
  no se inicia más tarde. Un script que navega la página (cambia la ubicación, envía un formulario) se ejecuta una vez y devuelve null.
- Un locator CSS encuentra también los elementos dentro de shadow roots abiertos.
- Visible significa que el elemento tiene una caja y no es `visibility: hidden`; habilitado tiene también en cuenta `aria-disabled`.
- El grabador de vídeo de la ejecución de pruebas (`awe.test.allowed-recording`) filma la pantalla de la máquina, no el navegador, de modo que una
  ejecución headless de Playwright no tiene nada de la prueba que filmar: desactívalo con `awe.test.allowed-recording=false`. Playwright
  graba la propia página, consulta [su evidencia](#playwright-evidence).

#### Evidencia de Playwright {#playwright-evidence}

Además de la captura de pantalla, el código fuente de la página y la consola de la [evidencia de fallos](#failure-evidence), Playwright deja dos archivos más
en `awe.test.screenshot-path` (`browser-evidence/` en CI, y enlazados desde el log como la captura de pantalla):

| Archivo | Qué es |
|---|---|
| `<test>.trace.zip` | El **trace** de una prueba fallida: cada acción con sus capturas de pantalla, la consola y la red (y el DOM en cada paso, consulta `trace-snapshots` más abajo). Un archivo por cada prueba fallida, con el nombre de su captura de pantalla |
| `<qualified.TestClass>.webm` | El **vídeo** de la página durante toda la clase de pruebas (con el nombre de la clase cualificada, p. ej. `com.almis.awe.test.selenium.CRUDSiteTestsIT.webm`). Uno por clase, y conservado solo cuando una prueba de la clase falló |
| `<qualified.TestClass>.video-times.txt` | Junto al vídeo: el inicio de cada prueba de la clase desde el comienzo del vídeo (`mm:ss.SSS`, aproximado), su resultado y su nombre. Busca la línea `FAILED` y lleva el vídeo hasta ahí |

El navegador dura toda la clase de pruebas (sus pruebas ordenadas comparten el inicio de sesión y los datos), de modo que el vídeo es de la clase y no
de la prueba; el archivo de tiempos indica dónde empieza cada prueba.

Para abrir un trace, usa el Trace Viewer de Playwright, que muestra la línea de tiempo, la página en cada acción y sus detalles:

```bash
npx playwright show-trace path/to/LoginIT-2026-10-06_10-00-00-000-[ERROR]-option-t020.trace.zip
```

o suelta el archivo en [trace.playwright.dev](https://trace.playwright.dev), que se ejecuta en tu navegador: el archivo no se sube
a ningún sitio.

Ambos se eligen con un modo, `off`, `on-failure` (el valor por defecto) o `always`:

| Propiedad | Valor por defecto | Significado |
|---|---|---|
| `awe.test.playwright.trace` | `on-failure` | `on-failure` guarda el trace de cada prueba fallida y descarta los demás; `always` los guarda todos; `off` ni siquiera los graba |
| `awe.test.playwright.trace-snapshots` | `false` | `true` toma también las instantáneas del DOM de cada acción, de modo que el Trace Viewer muestra la página que puedes inspeccionar antes y después de cada una. Hicieron que la suite del Scheduler fuera aproximadamente un 40% más lenta en nuestras mediciones (las capturas de pantalla del trace no cuestan nada apreciable), así que actívalas solo para investigar un fallo que las capturas no expliquen |
| `awe.test.playwright.video` | `on-failure` | `on-failure` conserva el vídeo de una clase con una prueba fallida y elimina los demás; `always` conserva todos; `off` no graba vídeo. El vídeo se graba a la mitad del tamaño de ventana (`awe.test.browser-width` × `awe.test.browser-height`): codificar un vídeo a tamaño completo hizo que las suites de Chromium fueran aproximadamente un 50% más lentas en los runners de CI |

Establece ambos modos a `off` para no grabar nada en absoluto, por ejemplo cuando depuras otra cosa.

## Escribir pruebas Selenium para tu producto {#writing-selenium-tests-for-your-product}

Las pruebas de tu producto no deberían saber qué librerías usa AWE para dibujar sus componentes. Si lo saben, reemplazar una librería (como AWE
5 hace con select2, ui-grid o el datepicker) rompe todas las suites. Localiza los componentes en este orden:

1. **Un método de `SeleniumUtilities`.** `clickButton`, `selectContain`, `suggest`, `selectDate`, `editRow`, `clickTab`,
   `checkAndCloseMessage`... ya saben cómo encontrar cada componente y se mantienen funcionando cuando AWE cambia. Las secciones
   siguientes los describen.
2. **Un hook `data-testid` más los atributos de AWE**, cuando no hay un método para lo que necesitas. El hook nombra la parte del
   componente y el atributo de AWE nombra la instancia: `criterion-id`, `grid-id`, `row-id`, `column-id`, `option-id`,
   `info-dropdown-id` o el `id` de un botón. `TestIds` y `TestAttributes` (`com.almis.awe.testing.selenium`) declaran
   el vocabulario para que no teclees las cadenas.
3. **Nunca una clase de librería** (`.select2-*`, `.datepicker`, `.ui-grid-*`, `.modal`, `.nav-tabs`, `.alert`, `.popover`,
   `.btn`, `fa-*`...) ni una posición en el marcado de una librería.

```java
import com.almis.awe.testing.selenium.TestAttributes;
import com.almis.awe.testing.selenium.TestIds;

// Wait for the warning alert of the login and check its text
checkText(TestIds.css(TestIds.ALERT) + TestAttributes.css(TestAttributes.TYPE, "warning") + " "
  + TestIds.css(TestIds.ALERT_MESSAGE), "The credentials entered for the user -test- are not valid");

// Click the first day of the open datepicker that is not disabled
click(TestIds.css(TestIds.DATEPICKER_DAY) + TestAttributes.css(TestAttributes.DISABLED, false));

// Type in the search box of the open select dropdown
writeText(Locator.css(TestIds.css(TestIds.SELECT_DROPDOWN) + " " + TestIds.css(TestIds.SELECT_SEARCH)), "tee");
```

### El atributo owner {#the-owner-attribute}

El desplegable de un select, el popup del selector de fechas y los popovers de mensajes se añaden al final del `<body>`, fuera
del componente. Llevan `data-testid-owner="<component id>"`. Úsalo cuando varios componentes puedan estar abiertos o cuando
necesites estar seguro de cuál abrió el elemento. Solo hay un desplegable de select (y un selector de fechas) abierto a la vez, y solo
el abierto lleva su hook, de modo que `select-dropdown` solo basta en la mayoría de las pruebas:

```java
// Dropdown and options opened by the select "Sta"
Locator dropdown = Locator.css("[data-testid='select-dropdown'][data-testid-owner='Sta']");
```

### Espera por estado, no por clases {#wait-on-state-not-on-classes}

Expón lo que esperas como estado: `data-selected`, `data-active`, `data-open`, `data-expanded`, `data-loading`...
(consulta *Estado* más arriba). Espera un estado que permanezca establecido hasta que la pantalla haya terminado de volver a renderizarse, por ejemplo
`data-loading='false'` en un icono de árbol, o el estado seleccionado de una fila tras hacer clic en ella. Las clases de librería como `.active`
o `.fa-spin` cambian por razones que no tienen nada que ver con AWE.

### Coincide con el texto completo de un elemento {#match-the-whole-text-of-an-element}

Una librería puede dividir el texto de un elemento en varios nodos: cuando escribes en un select, las letras coincidentes se
resaltan (`<span class="select2-match">B</span>ase`), y ningún nodo de texto contiene `Base`. Compara el texto del
elemento completo, con `contains(normalize-space(.), 'Base')` en un xpath, o con `getText()`. Nunca uses
`//text()[contains(., 'Base')]` sobre componentes de AWE.

```java
Locator option = Locator.xpath("//*[@data-testid='select-dropdown']//*[@data-testid='select-option']"
  + "[contains(normalize-space(.),'Base')]");
```

## Pasos personalizados sin tipos de Selenium {#custom-steps-without-selenium-types}

Un producto que necesita un paso que AWE no tiene lo escribe en una clase ayudante que extiende `SeleniumUtilities` (consulta
[Añadir un paso que falta](#adding-a-missing-step)). Escríbelo sin tipos de Selenium, para que siga funcionando si las pruebas se ejecutan
en otra herramienta de automatización: localiza con un `com.almis.awe.testing.driver.Locator` (`Locator.css(...)` o
`Locator.xpath(...)`), compón los pasos del [catálogo](#step-catalogue) que reciben un `Locator` y, cuando ningún paso
encaja, usa `getBrowser()`, que cualquier subclase puede llamar.

```java
import com.almis.awe.testing.driver.Locator;

public class MyProductSteps extends SeleniumUtilities {

  // Archive an invoice of the invoices screen and check that the screen confirms it
  protected void archiveInvoice(String invoiceId) {
    Locator archive = Locator.css("[row-id='" + invoiceId + "'] [data-testid='archive-action']");
    // A step of the catalogue, with a locator
    checkVisible(archive);
    // No step for the click on a product action: ask the browser
    getBrowser().click(archive);
    waitForText(Locator.css("[data-testid='archive-status']"), "Archived");
  }
}
```

`getBrowser()` devuelve el puerto `BrowserDriver`: consultas (`exists`, `isVisible`, `text`, `attribute`, `count`), acciones
(`click`, `type`, `hover`...) y operaciones de página, todo con un `Locator`. Es una vista previa sin promesa de compatibilidad
todavía, así que usa los pasos del catálogo cuando haya uno y mantén las llamadas al navegador dentro de tus ayudantes. Los
perfiles de motor (`IAweFrontEndInstructions`) siguen devolviendo `By` en 5.0; convierte uno en un `Locator` con `Locator.from(by)`.

:::note Obsoleto: `By` y `getDriver()`
Los pasos que reciben o devuelven un `By` de Selenium (consulta la tabla bajo [Pasos obsoletos](#step-catalogue)) y `getDriver()`
están `@Deprecated`. Siguen funcionando con la herramienta Selenium durante toda la línea 5.x y no se eliminan antes de la 6.0,
de modo que un ayudante existente compila sin cambios y solo muestra un aviso de obsolescencia. `getDriver()` solo está disponible cuando las
pruebas se ejecutan con la herramienta Selenium (`awe.test.tool=selenium`, el valor por defecto): con cualquier otra herramienta lanza una
`UnsupportedOperationException` que apunta a los pasos neutrales y a `getBrowser()`, por lo que un paso que lo use no funcionará
con otra herramienta. Migra un ayudante reemplazando
`By.cssSelector(x)` por `Locator.css(x)`, `By.xpath(x)` por `Locator.xpath(x)` y `By.id(x)` por `Locator.css("[id='x']")`
(`#x` deja de coincidir cuando el id tiene puntos, dos puntos u otros caracteres especiales de CSS, como suelen tener los ids generados), y las
llamadas al driver por los pasos neutrales o `getBrowser()`.
:::

## Mantener las pruebas de navegador libres de selectores {#keeping-browser-tests-free-of-selectors}

Las clases de prueba `*IT` solo deberían describir **pasos de pantalla**: haz clic en este botón, elige este valor, comprueba este mensaje.
Cómo se encuentra un elemento (un selector, un hook `data-testid`, un xpath) y qué herramienta controla el navegador pertenecen a las
instrucciones de frontend y a `SeleniumUtilities`, de modo que la misma prueba sigue funcionando cuando se reemplaza el motor web (AngularJS
o React) o cuando cambia la herramienta de automatización. Los locators escritos en los ejemplos anteriores (`TestIds`, `Locator`) son para las
clases ayudantes de tu producto que extienden `SeleniumUtilities`, no para las clases de prueba.

En una clase de prueba no uses:

- Imports de Selenium (`org.openqa.selenium.*`) o tipos: `By`, `WebDriver` (`getDriver()`), `WebElement`, `Select`,
  `JavascriptExecutor` (`executeScript(...)`), `Actions`.
- `Locator` y `getBrowser()`: son la forma neutral respecto a la herramienta de localizar y controlar elementos, y pertenecen a los ayudantes de
  tu producto como lo hacía `By`.
- `TestIds` y `TestAttributes`.
- Literales de selector en los ayudantes que reciben uno: `click`, `clearText`, `checkText`, `checkTextContains`,
  `checkTextNotContains`, `checkPresence`, `checkVisible`, `checkNotVisible`, `checkVisibleAndContains`,
  `checkTextInEmbeddedFrame`, `waitForCssSelector`, `waitForCssLocator`, `waitForText` con una clase, `checkLogin` con un selector y
  `checkLogout` con un selector. Usa en su lugar los pasos semánticos, por ejemplo `checkLogin("test", "test", "Manager (test)")`
  y `checkLogout()`.

`BrowserTestSourceGuard` (`com.almis.awe.testing.guard`, en `awe-testing`) comprueba esta regla sobre los fuentes Java, sin
necesitar un navegador ni ninguna clase de Selenium. Por defecto solo analiza las clases de prueba (`*IT.java`), porque las clases
ayudantes de tu producto son donde pertenecen los locators; `.files("*Page.java")` (cualquier glob sobre el nombre de archivo) analiza otros archivos. AWE lo aplica a sus propias aplicaciones de test en una prueba unitaria (`All UT`), y un
producto puede hacer lo mismo con una prueba propia:

```java
@Test
void shouldKeepTheBrowserTestsFreeOfSelectors() throws IOException {
  Report report = BrowserTestSourceGuard.create()
    .allow("FileManagerIT.java", "checkTextInEmbeddedFrame(\"ol.breadcrumb a\", \"Files\")",
      "The file manager is a third-party application inside a frame")
    .scan(Path.of("src/test/java/com/mycompany/selenium")); // only the *IT.java files unless .files(glob) is set

  assertThat(report.isClean()).as(report.describe()).isTrue();
}
```

El informe lista cada violación con su archivo, línea, regla y fragmento. La comprobación de selectores es heurística: solo mira
el argumento selector de los ayudantes anteriores, y un literal cuenta como selector cuando contiene `[ ] # . > + ~ * / :`
o un espacio. Un nombre de etiqueta simple (`"button"`) no se detecta, ni tampoco los selectores construidos en una variable, de modo que el guard
previene los casos comunes pero no sustituye a la revisión.

### Permitir una excepción justificada {#allowing-a-justified-exception}

Cuando una excepción está justificada, permite ese fragmento exacto en ese archivo e indica por qué. El motivo es obligatorio, y una
autorización que ya no coincide con el código se notifica (como obsoleta) para eliminarla junto con el código que disculpaba. El fragmento es
el que se imprime en el informe: la línea ofensora, o la llamada completa para un literal de selector. Usa
`allow(file, snippet, reason, times)` para permitir un número exacto de apariciones.

### Migrar una suite existente paso a paso {#migrating-an-existing-suite-step-by-step}

Si una suite ya tiene violaciones, lístalas en un archivo de línea base y deja que el guard funcione como un trinquete: una nueva violación falla,
y también falla una violación listada que ha desaparecido, de modo que la línea base solo se reduce hasta que la suite está migrada y el
archivo se elimina.

```java
String baseline = BrowserTestSourceGuard.create().scan(testSources).toBaseline(); // write it to a file once

Report report = BrowserTestSourceGuard.create()
  .allowBaseline(Path.of("src/test/resources/browser-test-guard-baseline.txt"), "Pending migration to semantic steps")
  .scan(testSources);
```

Cada línea de la línea base es `file:snippet`; las líneas en blanco y las que empiezan por `#` se ignoran, y una violación repetida
en un archivo se lista una vez por aparición.

## Añadir un paso que falta {#adding-a-missing-step}

Si el catálogo no tiene un paso para lo que necesita tu prueba, no escribas un selector en la prueba: añade el paso una vez, para todos los
clientes. Hazlo en este orden, primero la prueba:

1. **Comprueba lo que existe.** Busca en el [catálogo de pasos](#step-catalogue) y en los [hooks](#stable-test-hooks-data-testid).
   Muchos pasos "que faltan" son una sobrecarga (el mismo paso con un `gridId` y un `rowId`).
2. **Escribe el paso en la fachada.** Añade un método `protected` a `SeleniumUtilities` (`com.almis.awe.testing.utilities`)
   con un javadoc. Declara **qué hace o ve el usuario**, recibe solo cadenas, números y booleanos (nunca un
   tipo de Selenium, `By`, `Locator` o CSS) y pregunta a `frontEndInstructions` dónde está el elemento. Espera un estado (`data-*`) antes de
   actuar. Escribe primero la prueba unitaria (consulta `SeleniumUtilitiesSemanticStepsTest`) y observa cómo falla.
3. **Añade el locator al perfil de motor.** En `IAweFrontEndInstructions` añade un método `default` que devuelva el
   locator para el renderizado de AngularJS, mediante un hook `data-testid` siempre que haya uno. Nunca añadas un método abstracto:
   una implementación escrita antes dejaría de compilar. Cuando el cliente React renderiza algo distinto, sobrescríbelo
   en `ReactAweInstructions` (y en `AngularAweInstructions` si el valor por defecto no basta). Los locators deben usar hooks y
   atributos de AWE, nunca clases de librería: `AngularAweInstructionsSelectorGuardTest`, `ReactAweInstructionsSelectorGuardTest`
   y `SeleniumUtilitiesLiteralsGuardTest` fallan en caso contrario.
4. **Añade el hook si el cliente no tiene ninguno.** Declara el valor en el vocabulario de cada cliente que lo renderiza
   (`awe-client-angular/src/main/resources/js/awe/data/testIds.js` y `awe-client-react/src/utilities/testIds.js`) y en
   `TestIds` / `TestAttributes` (`awe-testing`). El mismo concepto usa el mismo valor en ambos clientes, y una parte que solo
   renderiza un cliente se marca como "Solo React" o "Solo AngularJS" en un comentario. `TestIdsVocabularyTest` y las pruebas Jest
   de los clientes fallan cuando los vocabularios se desvían. Los hooks son aditivos: nunca elimines ni renombres una clase, id o
   atributo existente.
5. **Documéntalo.** Añade una fila al [catálogo de pasos](#step-catalogue) y, si hay un nuevo hook o atributo, a
   [Hooks de prueba estables](#stable-test-hooks-data-testid). `SeleniumTestGuideSyncTest` hace fallar la compilación mientras un paso público o
   protegido no se menciona en esta guía como `name(`.
6. **Comprueba el contrato de compatibilidad.** El cambio debe ser aditivo: no elimines ni cambies un miembro público o
   protegido. Ejecuta el módulo completo, incluida la comprobación de la API:

   ```
   mvn verify -pl awe-framework/awe-testing
   ```

   Después ejecuta la suite del cliente que cambiaste (consulta [Ejecutar las suites](#running-the-suites-and-troubleshooting)).
   Reemplaza un paso solo marcando como obsoleto el antiguo (`@Deprecated`, indicando el reemplazo en su javadoc y en el
   catálogo).

## Ejecutar las suites y resolver problemas {#running-the-suites-and-troubleshooting}

### Ejecutar una suite en local {#run-a-suite-locally}

Las aplicaciones de test son `awe-tests/awe-boot` (AngularJS) y `awe-tests/awe-boot-react` (React). Compila el proyecto una vez
(`mvn install -DskipTests`), y después ejecuta una clase IT con un navegador headless desde la raíz del repositorio:

```
mvn -f awe-tests/awe-boot-react/pom.xml verify -Dskip.junit=true -Dskip.selenium=false \
  -Dawe.test.browser=headless-chrome -Dit.test=SchedulerCalendarTestsIT
```

- `-Dawe.test.browser` admite `headless-chrome` o `headless-firefox` (también `chrome` y `firefox` para ver el navegador).
  `-Dawe.test.tool` es `selenium` y puede omitirse (consulta [Herramienta de automatización](#automation-tool-awetesttool)).
- `-Dit.test=` admite una clase, varias separadas por comas (`CRUDSiteTestsIT,CriteriaTestsIT`) o un método
  (`SchedulerCalendarTestsIT#t001_...`). En `awe-boot` las suites también se seleccionan por etiqueta (`-Dgroups=SchedulerIT`).
- La aplicación arranca en el puerto 8080. Para usar otro, establécelo en el entorno (`SERVER_PORT=8090`); el navegador
  lee la dirección de `awe.test.start-url`, que sigue a `server.port`.
- Añade `xvfb-run -a` delante de `mvn` cuando la máquina no tiene pantalla (por ejemplo WSL).
- Las pruebas de una clase se ejecutan en orden de nombre (`t000_...` a `t999_...`). Las clases que son independientes (consulta
  [Clases de prueba independientes](#independent-test-classes)) pueden ejecutarse una prueba cada vez; en las otras una prueba continúa
  donde la anterior lo dejó, así que ejecuta la clase, no un método aislado, salvo que el método no dependa de los anteriores.

### Evidencia de fallos {#failure-evidence}

Cuando una prueba falla, `awe-testing` recopila su evidencia junta en `awe.test.screenshot-path`
(`target/tests/selenium/screenshots/` por defecto; la CI establece `browser-evidence/`) e imprime un enlace a cada archivo en
la salida de la prueba:

| Archivo | Qué es |
|---|---|
| `<test>.png` | Captura de pantalla en el momento del fallo |
| `<test>.html` | Código fuente de la página (DOM): mira aquí los hooks y atributos del elemento que la prueba no encontró |
| `<test>.console.log` | Consola del navegador (errores JavaScript, avisos y logs). Las entradas severas también se imprimen en la salida de la prueba, ya que un error del cliente a menudo explica una pantalla en blanco |
| vídeo | La grabación de la prueba. Los vídeos se conservan solo para las pruebas fallidas (`awe.test.video-save=FAILED`); `awe.test.allowed-recording=false` desactiva la grabación |
| `<test>.trace.zip`, `<qualified.TestClass>.webm` | **Solo Playwright**: el trace de la prueba fallida y el vídeo de la clase, consulta [Evidencia de Playwright](#playwright-evidence) |

Empieza por la consola y el HTML: un paso fallido normalmente significa que el elemento aún no estaba en el DOM, no llevaba el
atributo de estado esperado, o el cliente lanzó un error antes de renderizarlo.

### Pruebas inestables: reejecución, cadenas y cuarentena {#flaky-tests-rerun-chains-and-quarantine}

Los jobs de navegador bloquean el pipeline, de modo que una prueba que falla de vez en cuando sin un defecto (una prueba inestable, "flaky") bloquearía las merge
requests y las publicaciones al azar. Tres mecanismos se ocupan de ello, del más barato al más fuerte: una prueba fallida se
**reejecuta sola** una vez, una clase cuyas pruebas no se pueden reejecutar solas se declara una **cadena dependiente**, y una prueba que sigue
fallando se pone en **cuarentena**.

#### Una prueba fallida se reejecuta sola {#a-failed-test-is-rerun-alone}

Las aplicaciones de test ejecutan las pruebas de integración con `rerunFailingTestsCount` de failsafe establecido a 1 (la propiedad de Maven
`it.rerun-count`). Cuando falla una prueba de una [clase independiente](#independent-test-classes), esa única prueba se ejecuta una vez más, tras todas las pruebas de la ejecución, en una
nueva instancia de su clase con un nuevo navegador, y la preparación de sesión de la clase inicia sesión de nuevo. Las pruebas que ya
pasaron no se ejecutan de nuevo.

- Si el segundo intento pasa, la prueba pasa y se notifica como **flaky**: el log del job la lista bajo `Flakes:` con el
  fallo del primer intento, el resumen dice `Flakes: 1`, y el informe XML de la clase (`TEST-*.xml` en los artefactos
  del job) conserva el primer fallo en un elemento `<flakyFailure>` de la prueba. La [evidencia de fallos](#failure-evidence) del
  primer intento (captura de pantalla, código fuente de la página, consola, vídeo o trace) permanece en `browser-evidence/`. El informe de pruebas de GitLab cuenta
  la prueba como pasada, así que lee la sección `Flakes:` del log del job, y abre una incidencia cuando una prueba aparezca ahí.
- Si el segundo intento también falla, la prueba falla y también el job. Una regresión real falla dos veces.
- El propio job solo se reintenta cuando falla el runner (`runner_system_failure`), no porque una prueba falle: un reintento de job
  ejecutaría de nuevo todas las pruebas del job y ocultaría las inestables. Un timeout tampoco se reintenta. (Un job que ejecuta una
  [cadena dependiente](#dependent-chains) también se reintentaría ante un fallo de prueba: hoy ningún job lo hace.)
- Ejecuta sin reejecución, como cuando reproduces una prueba inestable, con `-Dit.rerun-count=0`.

**Una reejecución llega después de toda la ejecución.** failsafe reejecuta las pruebas fallidas cuando se han ejecutado todas las pruebas de la ejecución, en una nueva
instancia de la clase, de modo que una prueba se reejecuta en el estado que dejó el *final* de la ejecución, no el que dejó su fallo. Para una
prueba que abre su pantalla y usa datos propios eso es lo mismo, y la reejecución funciona. Para un paso de una secuencia de crear, actualizar y
eliminar no lo es: los pasos posteriores de la clase (que se ejecutaron tras el fallo) ya han actualizado o eliminado el
registro, y una reejecución simple del paso lo encontraría desaparecido, o encontraría un registro que no espera. Un paso que debe funcionar
cuando se reejecuta se asegura de lo que necesita antes de usarlo, y no hace nada cuando ya está ahí:

- un paso que necesita un registro (una actualización, un duplicado, una vista) lo crea primero con un ayudante que no hace nada cuando el registro
  está ahí, y el ayudante crea de la misma manera los registros de los que depende (un módulo crea primero su sitio);
- un paso que crea un registro no hace nada cuando ya está ahí y después ejecuta sus propias comprobaciones, de modo que un intento que falló
  tras guardar no se repite como duplicado;
- un paso que elimina un registro no hace nada cuando ya no está y después comprueba que no está;
- un paso que edita una fila la encuentra por lo que muestra antes y después del paso (el perfil de un módulo es `TST` antes de la
  actualización y `ADM` después de ella).

`hasRowContents` (consulta el [catálogo de pasos](#grids-checks)) es la sonda, y las clases CRUD (`AbstractCrudTests`) son el
ejemplo: cada paso ejecuta todas sus comprobaciones en una reejecución, nunca menos. Las clases Application y Scheduler aún no están escritas de esta
manera: sus pasos que actualizan o eliminan un registro fallan cuando se reejecutan tras terminar la ejecución, por lo que la reejecución no les ayuda (nunca
las hace pasar por error). Hasta que lo estén, un paso inestable de esas clases se corrige o se pone en cuarentena, no se deja a la
reejecución. Una clase cuyos pasos no se pueden hacer funcionar solos es una cadena dependiente.

#### Cadenas dependientes {#dependent-chains}

Ninguna clase de prueba de navegador es hoy una cadena dependiente: las pruebas CRUD se dividieron en clases independientes (sitios, perfiles,
módulos, conexiones de base de datos y usuarios, cada una con el sitio o el perfil que necesita), y ese es el camino a seguir.
La facilidad se mantiene para una clase cuyas pruebas realmente son una secuencia ordenada, cada una usando lo que creó la anterior,
y que no se puede dividir. Una clase así se declara con `@DependentChain` y el motivo, y conserva su `@TestMethodOrder`:

```java
@DependentChain("The import creates the records that the following tests check and delete")
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("ImportIT")
class ImportTestsIT extends SeleniumUtilities {
```

La compilación ejecuta las cadenas en una segunda ejecución de failsafe del mismo job (`integration-test-chains`), después de las clases
independientes y contra la misma aplicación, **sin reejecución**: una reejecución de un paso encontraría los datos que dejó el intento fallido
(un registro duplicado, un registro que ya estaba eliminado). La clase es la unidad a ejecutar de nuevo, y la forma de hacerlo es el
**reintento del job completo**: `.chain-retry` en `.gitlab-ci.yml` (un reintento ante `script_failure` y `runner_system_failure`, en un
contenedor nuevo con una base de datos limpia), que solo extiende un job que ejecuta una cadena, listado después de la plantilla que trae `.browser-testing` (GitLab fusiona las
plantillas en orden y gana la última que establece una clave). GitLab reintenta un job y no una entrada de su
matriz, de modo que las clases que comparten el job de la cadena se reintentan con él. `CiBrowserClassesGuardTest` comprueba la regla
desde los fuentes: un job cuyas clases incluyen una clase `@DependentChain` extiende `.chain-retry`, y un job sin ella no
lo hace. Una cadena solo se puede poner en cuarentena como una clase completa, porque no puede ejecutarse con un paso ausente. No añadas
`@DependentChain` para ocultar una prueba inestable: haz las pruebas independientes (consulta [Clases de prueba independientes](#independent-test-classes)).

#### Poner en cuarentena una prueba inestable {#quarantine-a-flaky-test}

Una prueba inestable que falla dos veces seguidas, o que sigue apareciendo bajo `Flakes:`, no se elimina ni se ignora: se
**pone en cuarentena**, lo que la saca de los jobs bloqueantes y sigue ejecutándola donde no puede bloquear nada.

```java
@Test
@Quarantine(issue = "#812", reason = "The suggest answers after the test has read the text")
void t002_loadSuggestOnGrid() {
  ...
}
```

`@Quarantine` (`com.almis.awe.testing.annotations`) va en un método de prueba, o en una clase para poner en cuarentena todas sus pruebas. Etiqueta
la prueba como `quarantine` para JUnit, y entonces:

- **Los jobs bloqueantes la dejan fuera.** Las aplicaciones de test excluyen la etiqueta `quarantine` de su ejecución de integración (la
  propiedad de Maven `it.excluded-groups` de `awe-tests/awe-boot` y `awe-tests/awe-boot-react`), de modo que ni los jobs de Playwright ni los de
  Selenium la ejecutan.
- **La ejecuta un job que nunca bloquea.** `Quarantine Playwright IT` y `Quarantine Selenium IT` ejecutan las pruebas en cuarentena de
  ambas aplicaciones (AngularJS y React), con Chromium. Tienen `allow_failure: true`, ningún reintento de job ni reejecución por prueba (`-Dit.rerun-count=0`, de modo que una prueba inestable se muestra como el fallo que
  fue), y no son
  necesarios para `Launch Sonar` ni para los jobs de publicación, de modo que una prueba en cuarentena que falla muestra una advertencia en el pipeline y nada
  más. Mantienen el mismo informe y la misma [evidencia de fallos](#failure-evidence) que los demás jobs de navegador, en los
  artefactos del job. Cuando no hay nada en cuarentena, los jobs terminan de inmediato sin arrancar la aplicación.
- **Ejecútala tú mismo** con `-Dgroups=quarantine -Dit.excluded-groups= -Dit.rerun-count=0` (vacío y cero) añadido al comando de
  [Ejecutar una suite en local](#run-a-suite-locally).

Las reglas de una cuarentena:

1. **Pon en cuarentena con evidencia, no para conseguir un pipeline en verde.** Una prueba que falló en un pipeline por un defecto real no es
   inestable: corrige el defecto. Pon una prueba en cuarentena cuando la hayas visto fallar y pasar con el mismo código (el enlace del job va en la
   incidencia).
2. **Toda cuarentena tiene una incidencia y un motivo.** `issue` es el número (`#812`) o la URL de la incidencia que hace seguimiento de la
   inestabilidad, y `reason` dice por qué la prueba es inestable en la medida en que se sabe. `BrowserTestDeclarationsGuardTest`, una prueba unitaria
   de ambas aplicaciones, hace fallar la compilación sin ellos, y también rechaza `@Tag("quarantine")` escrito a mano, que
   ocultaría una prueba sin incidencia.
3. **Una cuarentena es temporal.** Corrige la causa, elimina la anotación y cierra la incidencia en la misma merge request: el
   pipeline de esa merge request ejecuta la prueba de nuevo como una prueba bloqueante. El job `Quarantine ... IT` es la evidencia de que
   la prueba sigue siendo inestable o de que se ha recuperado, así que míralo antes de eliminar la anotación.
4. **Pon en cuarentena lo más pequeño que falla.** Un método antes que una clase. Los productos pueden usar la misma anotación y el mismo
   guard (`BrowserTestDeclarationsGuard.scan(...)`) en sus propias pruebas de navegador.

### Reproducir un fallo del navegador de la CI {#reproduce-a-failure-of-the-ci-browser}

Una prueba que pasa con `headless-chrome` puede fallar en CI porque la CI ejecuta el navegador desde una imagen de contenedor Selenoid, en una
máquina más lenta y con una compilación distinta de Firefox o Chrome. Para reproducirlo, ejecuta la misma imagen en local:

1. Toma la imagen fijada del navegador de `.gitlab-ci.yml` (`.chrome-testing` y `.firefox-testing`, los
   servicios `selenoid/chrome` y `selenoid/firefox`, con su digest `@sha256:`) e inícialo:

   ```
   docker run -d --rm --platform linux/amd64 -p 4444:4444 -e SCREEN_RESOLUTION=1440x1080x24 \
     selenoid/chrome:latest@sha256:<digest from .gitlab-ci.yml>
   ```

2. Arranca la aplicación de modo que el contenedor pueda alcanzarla, enlazada a todas las interfaces (`SERVER_ADDRESS=0.0.0.0`).
3. Ejecuta la clase contra el navegador del contenedor:

   ```
   SERVER_ADDRESS=0.0.0.0 mvn -f awe-tests/awe-boot-react/pom.xml verify -Dskip.junit=true -Dskip.selenium=false \
     -Dawe.test.browser=service-chrome -Dawe.test.browser-host=localhost -Dawe.test.browser-port=4444 \
     -Dawe.test.server-host=host.docker.internal -Dawe.test.server-port=8080 \
     -Dawe.test.allowed-recording=false -Dit.test=SchedulerCalendarTestsIT
   ```

   Usa `service-firefox` con la imagen `selenoid/firefox` para Firefox. `-Dawe.test.allowed-recording=false` es necesario
   porque el grabador de vídeo es otro servicio del job de CI que no estás ejecutando.

:::warning El navegador abre `server-host:server-port`, no `start-url`
Un navegador remoto (`service-*` y `remote-*`) no usa `awe.test.start-url`: la prueba construye la URL de la aplicación a partir de
`awe.test.server-host` y `awe.test.server-port` (por defecto `8080`) más `awe.test.context-path`. Si la aplicación escucha
en otro puerto, o el host no es alcanzable desde el contenedor, el navegador abre la dirección equivocada y **todas las pruebas
fallan en el primer paso** con una pantalla en blanco. Cuando no se establece `server-host`, usa la IP de la máquina en Linux y
`host.docker.internal` en el resto.
:::

:::tip Haz reproducibles los fallos de tiempo
Muchos fallos exclusivos de la CI son fallos de tiempo: el contenedor de la CI tiene poca CPU. Limita la CPU del contenedor del navegador
(`docker update --cpus=1 <container>`) antes de la ejecución para ralentizar el navegador como lo hace la CI, y súbela de nuevo
(`--cpus=4`) para comprobar que una corrección no depende de una máquina rápida.
:::

Cuando el fallo solo aparece en la CI, descarga los artefactos del job (`browser-evidence/` tiene la captura de pantalla, el código fuente de la página,
la consola del navegador y el vídeo de cada prueba fallida) antes de cambiar código: la evidencia suele indicar si el paso
necesita una mejor espera (un estado que esperar) o si el cliente tiene un defecto.

## Criterios {#criteria-1}

Los siguientes puntos describen cómo rellenar los distintos tipos de criterios disponibles en las pantallas de AWE:

### Input y Textarea {#input-and-textarea}

Simplemente llama al método `writeText` con los siguientes parámetros:
 - **criterionId** - Identificador del criterio
 - **text** - Texto a escribir

```java
// Insert text
writeText("criterionId", "textToWrite");
```

### Fecha {#date}

#### Elegir una fecha concreta en el selector de fechas {#pick-a-specific-date-in-the-datepicker}

Llama al método `selectDate` con los siguientes parámetros:
 - **criterionId** - Identificador del criterio de fecha
 - **date** - Fecha a seleccionar

```java
// Select a date
selectDate("Cal", "23/10/1978");
```

#### Elegir un día del mes actual {#pick-a-day-from-the-current-month}

Llama al método `selectDay` con los siguientes parámetros:
 - **criterionId** - Identificador del criterio de fecha
 - **day** - Día a seleccionar

```java
// Select a day in current month
selectDay("Cal", 23);
```

#### Elegir un mes en el selector de meses {#pick-a-month-in-the-month-selector}

Llama al método `selectMonth` con los siguientes parámetros:
 - **criterionId** - Identificador del criterio de fecha
 - **month** - Mes a seleccionar

```java
// Select a month in the month selector
selectMonth("Cal", 23);
```

#### Elegir un año en el selector de años {#pick-a-year-in-the-year-selector}

Llama al método `selectYear` con los siguientes parámetros:
 - **criterionId** - Identificador del criterio de fecha
 - **year** - Año a seleccionar

```java
// Select a year in the year selector
selectYear("Cal", 2019);
```

### Hora {#time}

Igual que [input y textarea](#input-and-textarea):

```java
// Write hour
writeText("Tim", "12:23:41");
```

### Select {#select}

Para elegir un resultado en un criterio select, llama al método `selectContain`:
 - **criterionId** - Identificador del criterio
 - **text** - Texto a buscar en la lista de resultados

```java
// Select on selector
selectContain("Sta",  "Yes");
```

### Suggest {#suggest}

Para usar un criterio suggest, llama al método `suggest`:
 - **criterionId** - Identificador del criterio
 - **text to suggest** - Texto a buscar
 - **result label** - Texto a buscar en la lista de resultados

```java
// Suggest on selector
suggest("Pro", "TS1", "TS1");
```

### Select y suggest múltiples {#multiple-select-and-suggest}

#### Seleccionar un valor {#select-one-value}

Selecciona un único valor con el criterio `suggestMultiple`:
 - **criterionId** - Identificador del criterio
 - **text to suggest** - Texto a buscar
 - **result label** - Texto a buscar en la lista de resultados

```java
// Suggest
suggestMultiple("CrtOpc", "application-info", "application-info");
```
#### Más de un valor {#more-than-one-value}

Selecciona más de un valor con el criterio `suggestMultipleList`:
- **criterionId** - Identificador del criterio
- **text 1 to suggest** - Texto 1 a buscar
- **text 2 to suggest** - Texto 2 a buscar
- **...** - Más textos a buscar

```java
// Suggest
suggestMultipleList("CrtOpc", "application-info", "application-warning", "application-error");
```

### Pestañas {#tabs}

#### Comprobar la pestaña activa {#check-active-tab}

Para comprobar si una pestaña está activa, llama al método `checkText`:
- **cssSelector** - Selector para encontrar el nodo de texto a comprobar
 - **text** - Texto a comprobar

```java
// Check if tab is active
checkText("[criterion-id='" + tabId + "'] li.active a", "Tab text");
```

#### Hacer clic en una pestaña {#click-on-a-tab}

Para hacer clic en una pestaña puedes llamar al método `clickTab`:
 - **tabId** - Identificador de la pestaña
 - **tabOption** - Etiqueta de la opción de pestaña: la clave de locale en el cliente AngularJS, el texto traducido en el cliente React
   (consulta [Clientes AngularJS y React](#angularjs-and-react-clients))
 
```java
// Click on tab
clickTab("TabSelMat", "ENUM_MATRIX_MULTISELECT");
```

### Checkbox y botón de radio {#checkbox-and-radio-button}

Haz clic en un checkbox o en un botón de radio de la misma manera con el método `clickCheckbox`:
 - **checkboxRadioId** - Identificador del criterio

```java
// Click checkbox or radio button
clickCheckbox("ChkBoxVa1");
```

### Vista de texto {#text-view}

Para comprobar si un componente de vista de texto contiene un texto depende de la estructura de la vista de texto:
- **cssSelector** - Selector para encontrar el nodo de texto a comprobar
- **text** - Texto a comprobar

```java
// Check the contents of a tag list (text view)
checkTagListContains(textViewId, textToCheck);
```

### Verificar los valores de los criterios {#verify-criteria-values}

#### Criterios de texto {#text-criteria}

Para verificar el contenido de los criterios de texto, usa el método `checkCriterionContents`:
 - **criterionId** - Identificador del criterio
 - **text** - Texto con el que coincidir

```java
// Check criterion
checkCriterionContents("Nam", "Inf Changed");
```

#### Criterios select y suggest {#select-and-suggest-criteria}

Para verificar el contenido de los criterios select y suggest, usa el método `checkSelectContents` para un select y el
método `checkSuggestContents` para un suggest (un suggest no se dibuja como un select, y los dos clientes muestran su valor
de forma distinta):
 - **criterionId** - Identificador del criterio
 - **text** - Texto con el que coincidir

```java
// Check a select
checkSelectContents("Scr", "Usr");

// Check a suggest
checkSuggestContents("Sug", "Test");
```

## Celdas de tabla {#grid-cells}

### Columna de input y textarea {#input-and-textarea-column}

Simplemente llama al método `writeText` con los siguientes parámetros:
 - **gridId** - Identificador de la tabla
 - **columnId** - Identificador de la columna
 - **text** - Texto a escribir

```java
// Write on text
writeText("GrdMuo", "Des2", "asdasda");
```

### Columna de fecha {#date-column}

#### Elegir una fecha concreta en el selector de fechas en una columna {#pick-a-specific-date-in-the-datepicker-in-a-column}

Para elegir una fecha en una fila de tabla, llama al método selectDate:
 - **gridId** - Identificador de la tabla
 - **columnId** - Identificador de la columna
 - **date** - Fecha a elegir

```java
// Click on date
selectDate("GrdEdi", "Dat", "23/10/1978");
```

#### Elegir un día del mes actual en una columna {#pick-a-day-from-the-current-month-in-a-column}

Llama al método `selectDay` con los siguientes parámetros:
 - **gridId** - Identificador de la tabla
 - **columnId** - Identificador de la columna
 - **day** - Día a seleccionar

```java
// Select a day in current month
selectDay("GrdEdi", "Dat", 23);
```

### Columna de hora {#time-column}

Igual que [columna de input y textarea](#input-and-textarea-column):
 - **gridId** - Identificador de la tabla
 - **columnId** - Identificador de la columna
 - **time** - Hora a seleccionar
 
```java
// Write hour
writeText("gridId", "timeColumn", "12:23:41");
```

### Columna select {#select-column}

Para elegir un resultado en un criterio select, llama al método `selectContain`:
 - **gridId** - Identificador de la tabla
 - **columnId** - Identificador de la columna
 - **text** - Texto a buscar en la lista de resultados

```java
// Select text
selectContain("GrdScrCnf", "Act", "Yes");
```

### Columna suggest {#suggest-column}

Para usar un criterio suggest, llama al método `suggest`:
 - **gridId** - Identificador de la tabla
 - **columnId** - Identificador de la columna
 - **text to suggest** - Texto a buscar
 - **result label** - Texto a buscar en la lista de resultados

```java
// Search for text
suggest("GrdScrCnf", "Atr", "visible", "Visible");
```

### Select y suggest múltiples en una columna {#multiple-select-and-suggest-in-a-column}

Estos dos criterios se pueden probar de la misma manera con el criterio `suggestMultiple`:
 - **gridId** - Identificador de la tabla
 - **columnId** - Identificador de la columna
 - **text to suggest** - Texto a buscar
 - **result label** - Texto a buscar en la lista de resultados

```java
// Suggest
suggestMultiple("gridId", "columnId", "application-info", "application-info");
```

### Columna de checkbox {#checkbox-column}

Haz clic en un checkbox o en un botón de radio de la misma manera con el método `clickCheckbox`:
 - **gridId** - Identificador de la tabla
 - **columnId** - Identificador de la columna

```java
// Click checkbox on a grid
clickCheckbox("gridId", "columnId");
```

### Botón de guardar {#save-button}

Para hacer clic en un botón de guardar de una tabla, llama al método `saveRow`.

```java
// Save line
saveRow();
```

Si hay varias tablas en la pantalla, necesitas añadir el identificador de la tabla al método:

```java
// Save line
saveRow("myGridIdentifier");
```

### Comprobar el valor de una fila {#check-a-row-value}

Para comprobar si hay textos concretos dentro de una tabla, llama al método `checkRowContents`:

```java
// Check row contents
checkRowContents("test", "ADM", "Site changed");
```

Puedes añadir tantos textos como quieras comprobar.

Si quieres comprobar el contenido de una celda concreta, puedes llamar al método `checkCellContents`:
 - **gridId** - Identificador de la tabla
 - **rowId** - Identificador de la fila
 - **columnId** - Identificador de la columna
 - **text** - Texto con el que coincidir

```java
// Check date on second row
checkCellContents("GrdEdi", "2", "Dat", date);
```

### Hacer clic en una fila {#click-on-a-row}

Para hacer clic en una fila con un texto definido, llama al método `clickRowContents`:
 - **gridId** - Identificador de la tabla
 - **text** - Texto con el que coincidir

```java
// Click on grid
clickRowContents("GrdEdi", "asphalt");
```

O si quieres hacer clic en una celda concreta, puedes llamar al método `clickCell`:
 - **gridId** - Identificador de la tabla
 - **rowId** - Identificador de la fila
 - **columnId** - Identificador de la columna

```java
  // Click on a cell
  clickCell("GrdMuo", "1", "Des2");
```

### Expandir o contraer una fila {#expand-or-collapse-a-row}

Para expandir o contraer una fila de una tabla en árbol, hemos definido el método `clickTreeButton`:
 - **gridId** - Identificador de la tabla
 - **rowId** - Identificador de la fila
 
```java
// Click on button
clickTreeButton("TreGrdLoaEdi", "Prooperator");
```

### Menú contextual en una fila {#context-menu-on-a-row}

Para abrir un menú contextual en una fila de tabla, simplemente llama al método `contextMenu`:
 - **gridId** - Identificador de la tabla
 - **rowId** - Identificador de la fila
 - **columnId** - Identificador de la columna
 
```java
// Context menu
contextMenu("TreGrdLoaEdi", "Progeneral-ModBase", "TreGrdLoaEdi_Nam");
```

### Opción de menú contextual {#context-menu-option}

Puedes hacer clic en una opción de menú contextual con la opción `clickContextButton`.
 - **menuOptions** - Opciones en las que hacer clic (ordenadas) en el menú contextual

```java
// Select context menu option
clickContextButton("CtxTreGrdLoaEdiAddSel", "CtxTreGrdLoaEdiAddChl");
```

## Ejemplos {#samples}

### Añadir un nuevo sitio {#add-a-new-site}

```java
/**
 * Add a new site
 * @throws Exception
 */
@Test
public void t001_newSite() throws Exception {
  // Title
  setTestTitle("Add a new site");

  // Go to screen
  gotoScreen("tools", "sites");

  // Click on new button
  clickButton("ButNew", true);

  // Wait for button
  waitForButton("ButCnf");

  // Write on criterion
  writeText("Nam", "Site test");

  // Select last element
  selectLast("Act");

  // Write on criterion
  writeText("Ord", "3");

  // Click on button
  clickButton("ButGrdAdd");

  // Suggest on column selector
  suggest("SitModDbsLst", "IdeMod", "Base", "Base");

  // Suggest on column selector
  suggest("SitModDbsLst", "IdeDbs", "awedb", "awedb");

  // Write on criterion
  writeText("SitModDbsLst", "Order", "3");

  // Save line
  saveRow();

  // Check row values
  checkRowContents("Base", "awedb", "3");

  // Store and confirm
  clickButtonAndConfirm("ButCnf");
  
  // Wait for button
  clickButton("ButRst");

  // Suggest on column selector
  suggest("CrtSit", "Site test", "Site test");

  // Search on grid
  searchAndWait();

  // Click row
  clickRowContents("Site test");

  // Click on button
  clickButton("ButViw", true);

  // Wait for button
  waitForButton("ButBck");
  
  // Check row contents
  checkRowContents("Base", "awedb");
}
```

### Eliminar un módulo {#delete-a-module}

```java
/**
 * Delete a module
 * @throws Exception
 */
@Test
public void t056_deleteModule() throws Exception {
  // Title
  setTestTitle("Delete a module");

  // Go to screen
  gotoScreen("tools", "modules");

  // Wait for button
  clickButton("ButRst");

  // Suggest on column selector
  suggest("CrtMod", "Inf", "Inf");

  // Search on grid
  searchAndWait();

  // Click row
  clickRowContents("Inf");

  // Store and confirm
  clickButtonAndConfirm("ButDel");

  // Wait for button
  clickButton("ButRst");

  // Search on grid
  searchAndWait();

  // Click row
  checkRowNotContains("Inf");  
}
```

### Actualizar una base de datos {#update-a-database}

```java
/**
 * Update a database connection
 * @throws Exception
 */
@Test
public void t033_updateDatabase() throws Exception {
  // Title
  setTestTitle("Update a database connection");

  // Go to screen
  gotoScreen("tools", "databases");

  // Wait for button
  clickButton("ButRst");

  // Suggest on column selector
  suggest("CrtAls", "DBSTest", "DBSTest");

  // Search on grid
  searchAndWait();

  // Click row
  clickRowContents("DBSTest");

  // Click on button
  clickButton("ButUpd", true);

  // Wait for button
  waitForButton("ButCnf");

  // Insert text
  writeText("Als", "DBSTest Changed");

  // Select on selector
  selectContain("Dct",  "Jdbc");

  // Insert text
  writeText("Dbc", "Test");

  // Insert text
  writeText("Des", "This is a database connection update test case");

  // Click on row
  clickRowContents("Site changed");

  // Suggest on column selector
  suggest("SitModDbsLst", "IdeMod", "Test", "Test");

  // Save line
  saveRow();

  // Check row
  checkRowContents("Test");

  // Store and confirm
  clickButtonAndConfirm("ButCnf");

  // Wait for button
  clickButton("ButRst");

  // Suggest on column selector
  suggest("CrtAls", "DBSTest Changed", "DBSTest Changed");

  // Search on grid
  searchAndWait();

  // Click row
  clickRowContents("DBSTest Changed");

  // Click on button
  clickButton("ButViw", true);

  // Wait for button
  waitForButton("ButBck");

  // Check contents
  checkCriterionContents("Als", "DBSTest Changed");

  // Check row contents
  checkRowContents("Test");
}
```
