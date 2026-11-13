---
id: sample-applications
title: Aplicaciones de ejemplo
sidebar_label: Aplicaciones de ejemplo
---

El repositorio incluye tres aplicaciones completas de AWE 5 en `awe-samples/`. Funcionan sobre el motor React, se
declaran en XML (la única clase Java arranca Spring Boot) y rellenan su propia base de datos en el primer arranque.

Úselas como **demostración** y como **referencia**: abra el XML junto a la pantalla en ejecución para ver cómo se
declara cada funcionalidad.

## Los ejemplos {#the-samples}

| Ejemplo | Dominio | Puerto | Funcionalidades de AWE que muestra |
|---------|---------|--------|------------------------------------|
| `vet-clinic` | Clínica veterinaria | 18101 | [Pantallas](../api/screen.md), [criterios](../api/criteria.md), [grids](../api/grids.md) anidados, [gráficos](../api/chart.md), subida de ficheros, [impresión y exportación](print-guide.md), [permisos](../security/permissions.md), [tres idiomas](../api/i18n-internationalization.md) |
| `fleet-logistics` | Transporte y logística | 18102 | [Planificador](../scheduler-module.md), [notificador](../notifier-module.md), [grids](../api/grids.md) grandes y en árbol, [gráficos](../api/chart.md), [mapas](../api/map.md) con rutas, seguimiento público, [permisos](../security/permissions.md) |
| `help-desk` | Soporte informático y de clientes | 18103 | [Asistente](../api/wizard-and-wizard-panel.md), [panel lateral](../api/side-panel.md), [API REST](../rest-module.md), [permisos](../security/permissions.md), flujo de trabajo aplicado en el servidor, [gráficos](../api/chart.md), [widget propio](custom-widgets-react.md) (chat de soporte que abre un ticket sin iniciar sesión) |

Cada ejemplo tiene un `README.md` con sus usuarios de demostración, la tabla completa de funcionalidades por fichero y
sus notas de puesta en marcha.

## Un recorrido por los ejemplos {#a-tour-of-the-samples}

Tres recorridos breves por el aspecto de cada ejemplo en ejecución. Cada pie de imagen indica la pantalla y la funcionalidad de AWE que hay detrás; el XML de cada pantalla está en el ejemplo.

### Happy Paws (clínica veterinaria) {#happy-paws-veterinary-clinic}

Una clínica con sitio público, un área de personal para los veterinarios y un portal donde los propietarios siguen a sus mascotas. Se ejecuta en `http://localhost:18101` y muestra [criterios](../api/criteria.md), [grids](../api/grids.md) anidados, [gráficos](../api/chart.md), [paneles laterales](../api/side-panel.md) e [impresión y exportación](print-guide.md).

<img alt="Página pública de Happy Paws con una foto principal y tres cifras" src={require('@docusaurus/useBaseUrl').default('img/samples/vet-home.webp')} />

*La página pública: portada y cifras, sin iniciar sesión.*

<img alt="Servicios y galería de fotos de la página de Happy Paws" src={require('@docusaurus/useBaseUrl').default('img/samples/vet-gallery.webp')} />

*Servicios y galería más abajo en la misma página pública, declarados en XML.*

<img alt="Formulario de solicitud de cita de Happy Paws" src={require('@docusaurus/useBaseUrl').default('img/samples/vet-appointment.webp')} />

*La solicitud de cita: un formulario público que solo inserta una solicitud pendiente.*

<img alt="Panel del veterinario en modo oscuro con tarjetas y gráficos" src={require('@docusaurus/useBaseUrl').default('img/samples/vet-dashboard.webp')} />

*El panel del veterinario en modo oscuro: tarjetas con tendencias, tres gráficos y un grid con las vacunas por renovar.*

<img alt="Panel lateral de una factura con sus líneas y un botón para descargar el PDF" src={require('@docusaurus/useBaseUrl').default('img/samples/vet-invoice-panel.webp')} />

*Una factura en un panel lateral, con sus líneas y la acción **Descargar PDF**.*

<img alt="Portal del propietario con la próxima cita y el pago pendiente" src={require('@docusaurus/useBaseUrl').default('img/samples/vet-owner-portal.webp')} />

*El portal del propietario: el servidor filtra cada consulta a las mascotas del propietario conectado.*

### Meridian Freight (logística) {#meridian-freight-logistics}

Una empresa de transporte con seguimiento público de envíos, un área de despacho y un área de clientes. Se ejecuta en `http://localhost:18102` y muestra [mapas](../api/map.md) con rutas, [grids](../api/grids.md) grandes, el [planificador](../scheduler-module.md) y el [notificador](../notifier-module.md).

<img alt="Seguimiento público del envío MF-T9JSDP6N con su cronología y puntos numerados en un mapa" src={require('@docusaurus/useBaseUrl').default('img/samples/fleet-tracking.webp')} />

*Seguimiento público: el código `MF-T9JSDP6N` y el código postal `36919` devuelven la cronología y los puntos numerados en el mapa.*

<img alt="Panel de despacho con gráficos, un mapa de la flota y el vehículo seleccionado" src={require('@docusaurus/useBaseUrl').default('img/samples/fleet-dispatch-board.webp')} />

*El panel de despacho: gráficos, la flota en un mapa y el vehículo seleccionado en él.*

<img alt="Planificador de rutas con la ruta R1003 seleccionada y sus paradas numeradas unidas por una ruta por carretera" src={require('@docusaurus/useBaseUrl').default('img/samples/fleet-route-planner.webp')} />

*El planificador de rutas con **R1003** seleccionada: paradas numeradas y la carretera real que las une.*

<img alt="Página de inicio de sesión dividida de Meridian Freight" src={require('@docusaurus/useBaseUrl').default('img/samples/fleet-signin.webp')} />

*La página de inicio de sesión, dividida en dos: el formulario a un lado y la marca al otro.*

### Beacon Desk (mesa de ayuda) {#beacon-desk-help-desk}

Una mesa de soporte informático con un portal público, una cola para los agentes y un panel para los supervisores. Se ejecuta en `http://localhost:18103` y muestra el [panel lateral](../api/side-panel.md), los [gráficos](../api/chart.md), la [API REST](../rest-module.md) y un [widget propio](custom-widgets-react.md).

<img alt="Chat de soporte abierto en la página de Beacon Desk con un artículo sugerido de la base de conocimiento" src={require('@docusaurus/useBaseUrl').default('img/samples/desk-chat.webp')} />

*El chat de soporte responde con artículos de la base de conocimiento antes de ofrecer abrir un ticket, sin iniciar sesión.*

<img alt="Consulta pública de un ticket con su estado y sus respuestas" src={require('@docusaurus/useBaseUrl').default('img/samples/desk-ticket-check.webp')} />

*Consulta de ticket: la clave de acceso `G6AP6EHR` y el correo del solicitante (`ava.parker@contoso.example`) muestran el estado y las respuestas.*

<img alt="Cola del agente con el panel lateral de un ticket y su conversación" src={require('@docusaurus/useBaseUrl').default('img/samples/desk-agent-queue.webp')} />

*La cola del agente: al seleccionar un ticket se abre su panel con la conversación y el formulario de respuesta.*

<img alt="Panel de SLA en modo oscuro con tarjetas y cuatro gráficos" src={require('@docusaurus/useBaseUrl').default('img/samples/desk-sla-dashboard.webp')} />

*El panel del supervisor en modo oscuro: tickets abiertos, incumplimientos de SLA y tiempos de resolución.*

## Compilar y ejecutar uno {#build-and-run-one}

```bash
mvn -f awe-samples/help-desk/pom.xml spring-boot:run
```

Sustituya `help-desk` por `vet-clinic` o `fleet-logistics` y abra `http://localhost:<puerto>` (vea la tabla).

| Necesidad | Comando |
|-----------|---------|
| Primera compilación (necesita el cliente React en `awe-framework/awe-client-react/dist`) | `mvn -pl awe-samples/help-desk -am install -DskipTests` |
| Jar ejecutable | `mvn package && java -jar awe-samples/help-desk/target/help-desk.jar` |
| Solo los tests de descriptores | `mvn test -pl awe-samples/help-desk -Dskip.frontend=true` |
| Empezar de nuevo con datos de demostración limpios | Borre la carpeta `target/db` |

`-Dskip.frontend=true` omite la compilación del cliente, de modo que los tests de descriptores se ejecutan sin Node.

## El patrón que comparten {#the-pattern-they-share}

Todos los ejemplos tienen una parte pública y una parte privada:

| Zona | Quién | Cómo se separa |
|------|-------|----------------|
| Pública | Cualquiera, sin iniciar sesión | `menu/public.xml`; unas pocas queries y targets de maintain marcados `public="true"` |
| Personal | Veterinarios, despachadores, agentes, supervisores | Perfiles de AWE (`profile/*.xml`) y los permisos de cada target |
| Clientes finales | Dueños de mascotas, clientes de transporte, solicitantes | Un perfil con filtrado por fila según el usuario conectado |

- **La seguridad por fila está en el servidor.** El login del cliente está enlazado con su registro. Cada query del área
  de cliente filtra con una query solo de servidor que lee el usuario de la sesión (`VetSelfOwn`, `FltSelfCus`,
  `HdkSelfReq`), y los targets de maintain toman el propietario de esa misma query. Ocultar un botón no es seguridad.
- **Los targets públicos son los mínimos.** Solo es público lo que necesita la página de inicio: listas de valores, una
  petición de solo inserción y, en el seguimiento y la consulta de tickets, una query que exige un código secreto y un
  segundo dato. Nada más se ejecuta sin sesión.
- **Permisos en cada target.** El perfil los concede; un rol que no debe hacer algo no recibe el permiso. Vea
  [Permisos](../security/permissions.md).

## Tests de descriptores para su aplicación {#descriptor-tests-for-your-application}

`AbstractApplicationDescriptorTest` (módulo `awe-testing`) comprueba el XML de una aplicación con JUnit puro: sin
contexto de Spring ni navegador, por lo que se ejecuta en milisegundos. Extiéndala e indique el directorio del módulo:

| Regla | Qué hace fallar la compilación |
|-------|--------------------------------|
| Análisis | Un fichero XML que no se convierte en su clase del modelo de AWE |
| Claves de locale | Una clave usada por una pantalla, menú, grupo enumerado o permiso y ausente en un fichero de locale |
| Targets de servidor | Una query, target de maintain o grupo enumerado que lanza una pantalla y ningún descriptor declara |
| Targets públicos previstos | Un target con `public="true"` que `publicTargets()` no lista, o al revés |
| Secretos desde el entorno | Una propiedad `master-key` o `secret` que no tiene la forma `${NOMBRE:ejemplo}` |

Los módulos de AWE del classpath cuentan como targets y claves conocidos, así que la aplicación necesita sus
dependencias de AWE, igual que para ejecutarse. Este es el test del help desk, abreviado:

```java
class HelpDeskDescriptorTest extends AbstractApplicationDescriptorTest {

  @Override
  protected Path moduleDirectory() {
    return Path.of("src/main/resources/application/helpdesk");
  }

  // Solo lecturas: ningún maintain es público
  @Override
  protected Set<String> publicTargets() {
    return Set.of("HdkSvcPub", "HdkArtPub", "HdkCatPub", "HdkStatusPub", "HdkRepliesPub");
  }

  // Las reglas propias de la aplicación van junto a las heredadas
  @Test
  void supervisorOnlyTargetsRequireSupervisorPermissions() throws Exception {
    // ...
  }
}
```

`propertiesFile()` apunta por defecto al `application.properties` dos niveles por encima del directorio del módulo;
sobrescríbalo si el suyo está en otro sitio. Añada sus propios métodos `@Test` para reglas específicas de su
aplicación, como hace el ejemplo.

## No listo para producción tal cual {#not-production-ready-as-is}

Los ejemplos son demostraciones. Antes de usar uno como base de una aplicación real:

- Defina `AWE_MASTER_KEY` (y `AWE_JWT_SECRET` en `help-desk`). Sin ellas se usan los valores de ejemplo, públicos en
  este repositorio.
- Elimine o desactive los usuarios de demostración y, sobre todo, el administrador `test` / `test`.
- Ponga un CAPTCHA y un límite de peticiones delante de cada inserción y consulta anónima (solicitudes de presupuesto y
  de cita, consultas de envíos y de tickets).
- Restrinja o desactive `/v3/api-docs` cuando se exponga la API REST (`help-desk`).
- Genere los códigos de seguimiento de forma aleatoria al insertar; los datos de ejemplo los derivan con una fórmula
  fija para que la demostración sea repetible.
