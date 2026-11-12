# Meridian Freight: AWE sample application (fleet and logistics)

Meridian Freight is a freight company. Visitors track a shipment on a map and request a quote without signing in;
dispatchers run the operations (shipments, vehicles, drivers, routes, assignments); customers follow only their own
shipments, pickups and invoices. It shows how AWE 5 on the React engine does maps with real road routes, and it is
where the AWE scheduler and notifier show what they do. Everything is declared in XML
(`src/main/resources/application/fleet`); the Java code is the Spring Boot starter and `FleetMapService`.

| At a glance | |
|-------------|---|
| URL | <http://localhost:18102> |
| Engine | React (AWE 5) |
| Database | HSQLDB file database, created and seeded by Flyway (about 1,000 shipments over 90 days) |
| Main showcase | Maps and road routes, scheduler and notifier, large grid |
| Languages | en-GB, es-ES, fr-FR |

## Features

**Public site (no sign-in)**

- Landing page: `fleet-home.xml`.
- Shipment tracking by tracking code AND postal code: timeline plus numbered points on a map:
  `fleet-track.xml`, `FltTrackPub`, `FltTrackRoute`.
- Quote request form, insert-only and checked by the database: `fleet-quote.xml`, `FltQuoNew`.

**Dispatcher area**

- Dispatch board: cards, ECharts (line with zoom, weekly on-time columns, stacked columns by region, donut) and a map
  of the vehicles on route that refreshes itself: `fleet-dashboard.xml`.
- Shipments: grid of 1,000 rows with column filters, group selector with subtotals, print and export:
  `fleet-shipments.xml`.
- Fleet yard and crew roster: vehicles and drivers with status tones; create, edit, delete:
  `fleet-vehicles.xml`, `fleet-drivers.xml`.
- Route planner: tree grid of routes and stops; selecting a route draws the numbered stops and the road on a map:
  `fleet-routes.xml`.
- Today's assignments: inline-editable grid to assign a vehicle and a driver to routes: `fleet-assignments.xml`.
- Quote requests and pickups: review them and **Mark as handled**: `fleet-quotes.xml`, `fleet-pickups.xml`.

**Customer area**

- My shipments, My pickups (**Request a pickup**, **Cancel pickup**) and My invoices, limited to the customer account:
  `fleet-my-shipments.xml`, `fleet-my-pickups.xml`, `fleet-my-invoices.xml`.

**Background jobs and integrations**

- Scheduler: two seeded tasks, **Flag late shipments** (every 5 minutes) and **Close past routes** (every hour).
- Notifier: late shipments notify the dispatchers and the affected customer account (notification bell).
- Map tiles from OpenStreetMap, configurable with `awe.application.map.tile-url`.

**Administration**

- Scheduler and notifier screens (tasks, servers, calendars, subscriptions, notifications) and the AWE user, profile and
  access screens for the `test` user.

## Deploy

### Prerequisites

- JDK 21 and Maven. The build downloads Node for the React bundle, so no Node install is needed.
- Port 18102 free (see [Configuration](#configuration) to change it).
- Internet access from the browser for the map tiles, unless you set your own tile server.

### Build and run

Run from the repository root:

```bash
# Build the application and the framework modules it needs (first time; skip the tests to save time)
mvn -pl awe-samples/fleet-logistics -am install -DskipTests

# Run it from Maven ...
mvn -f awe-samples/fleet-logistics/pom.xml spring-boot:run

# ... or run the jar the build produces
java -jar awe-samples/fleet-logistics/target/fleet-logistics.jar
```

Open <http://localhost:18102>. Add `-Dskip.frontend=true` to build or test the descriptors without the frontend
(`mvn test -pl awe-samples/fleet-logistics -Dskip.frontend=true`).

### What the first start creates

An HSQLDB file database at `target/db/fleet-logistics` (`spring.datasource.url=jdbc:hsqldb:file:target/db/fleet-logistics`,
user `sa`, empty password). The path is relative to the working directory, so `java -jar` creates it under the directory
where you run it. Flyway runs the migrations of AWE, the scheduler, the notifier and the application (`FLEET`). The seed
generates about 1,000 shipments over the last 90 days, relative to the first start, and the two scheduler tasks, which
are loaded on start (`awe.scheduler.tasks-load-on-start=true`).

### Configuration

Every property of `src/main/resources/application.properties` can be overridden with a Spring mechanism: an
environment variable (`SERVER_PORT`), a command-line argument (`--server.port=9000`) or an `application.properties`
next to the jar.

| Property | Default | Purpose |
|----------|---------|---------|
| `server.port` | `18102` | HTTP port (`SERVER_PORT`) |
| `spring.datasource.url` | `jdbc:hsqldb:file:target/db/fleet-logistics` | Database location (`SPRING_DATASOURCE_URL`) |
| `awe.security.master-key` | sample value | Encrypts stored data. Set `AWE_MASTER_KEY` |
| `awe.application.map.tile-url` | `https://tile.openstreetmap.org/{z}/{x}/{y}.png` | Map tile server (not in the sample file) |
| `awe.scheduler.tasks-load-on-start` | `true` | Load the scheduler tasks when the application starts |
| `awe.application.theme-primary` | `blue` | Primary colour of the theme |

The map tile server is the framework default and is not set in the sample's properties file. Add the property to
`application.properties` or pass `--awe.application.map.tile-url=...` to change it.

### Reset the demo data

Stop the application, delete `target/db/fleet-logistics*` (relative to the working directory it ran in) and start it
again. The dates of the seed are relative to that first start.

### Behind a reverse proxy or on another port

Set `SERVER_PORT` (or `--server.port`) to the port you want and point the proxy to it. The application serves at the
root path (`server.servlet.context-path=/`), so proxy the whole host or a dedicated host name, not a sub-path.

### Before using this outside a demo

- Set the master key with the environment variable `AWE_MASTER_KEY` (`application.properties` reads
  `${AWE_MASTER_KEY:...}` and falls back to a sample value that is public in this repository).
- Deactivate or delete the demo users (`maria`, `jorge`, `acme`, `globex`, `initech`) and above all the `test` / `test`
  administrator, then create your own users and passwords.
- The public quote request accepts unlimited anonymous inserts, a demo simplification so that the landing page works
  without sign-in. A real deployment needs a CAPTCHA and a rate limit in front of it, and the public tracking query needs a
  rate limit too.
- Set `awe.application.map.tile-url` to your own tile server or a commercial provider: the public OpenStreetMap server is
  for light use only (see [Maps](#maps)).
- Replace the HSQLDB file database with a managed database for real data, and back it up.

## Use it

### Demo users

The home page of the application shows this same table in its **Try the demo** section, and the sign-in page repeats one pair per role.

| User | Password | What this user sees |
|------|----------|---------------------|
| `maria` | `fleetdemo` | Dispatcher: operations, routes, scheduler and notifier. |
| `jorge` | `fleetdemo` | Dispatcher: operations, routes, scheduler and notifier. Language: Spanish. |
| `acme` | `shipdemo` | Customer: only their own shipments, pickups and invoices. |
| `globex` | `shipdemo` | Customer: only their own shipments, pickups and invoices. Language: Spanish. |
| `initech` | `shipdemo` | Customer: only their own shipments, pickups and invoices. Language: French. |
| `test` | `test` | AWE administrator: users, profiles and screen access. Demo only. |

The `test` / `test` administrator comes from the AWE schema and is **demo-only**: it is kept so that the AWE administration
screens can be shown and it is not deactivated. All these are sample credentials: see
[Before using this outside a demo](#before-using-this-outside-a-demo).

### Tour 1: visitor

1. Signed out, open <http://localhost:18102> and choose **Track a shipment**.
2. Enter the code **MF-T9JSDP6N** with the postal code **36919** (or **MF-MPL9GK42** with **41838**). Both values must match:
   any other combination returns nothing. You get the timeline and the numbered points on the map.
3. Open **Request a quote**, fill the form and send it: it is stored as a request for the dispatchers.

The tracking codes are not sequential: the seed derives them from the shipment key with a fixed scrambling formula, so
they are deterministic (the same on every fresh database) but cannot be guessed by counting. In production, generate them
randomly at insert time and rate-limit the public tracking query.

### Tour 2: dispatcher

1. Sign in as `maria` / `fleetdemo`. The **Dispatch board** shows the charts and the map of the vehicles on route; select a
   vehicle to see its driver and load.
2. **Shipments**: filter a column, use the group selector for subtotals, then print or export.
3. **Route planner**: select a route, for example **R1003** (Pontevedra, Redondela, O Porriño, Baiona). Its stops are
   numbered on the map and the road between them is drawn.
4. **Today's assignments**: change the vehicle or driver of a route in the grid and use **Save assignments**.
5. **Quote requests** and **Pickups**: select rows and use **Mark as handled**.
6. Under **Automation**, open **Scheduler > Tasks** to see the two seeded tasks. After the first run of **Flag late
   shipments**, the notification bell shows the late shipments and **Delayed shipments** of
   the board grows. **Notifier > Notifications** lists them.

### Tour 3: customer

1. Sign in as `acme` / `shipdemo` and open **My shipments**: only the shipments of Acme appear.
2. **My pickups**: use **Request a pickup**, then **Cancel pickup**.
3. **My invoices**: only the invoices of this account.
4. Sign in as `globex` and check that the lists show different records.

## How it is built

| Feature | Screen or file |
|---------|----------------|
| Public landing, no sign-in | `menu/public.xml`, `screen/fleet-home.xml` (styles in `src/css/main.css`, AWE tokens only) |
| Public tracking: code AND postal code, only that timeline and its numbered points on a map | `screen/fleet-track.xml`, public query `FltTrackPub` and public target `FltTrackRoute` (service `FleetMapService`, private query `FltTrackPts`: both variables are mandatory), views `FltTrk` and `FltTrkPts` |
| Public quote form, insert-only, checked by the database | `screen/fleet-quote.xml`, `FltQuoNew`, constraints of `FltQuo` |
| Dashboard: cards and ECharts (line with date axis and zoom, weekly on-time columns, stacked columns by region, donut) and a map of the vehicles on route (one marker per vehicle, plate and driver, auto-refresh, selecting one shows its driver and load) | `screen/fleet-dashboard.xml`, query `FltDshVehMap`, view `FltVehMap` |
| Large grid: 1,000 rows loaded at once, column filters, group selector with subtotals and totals, print and export | `screen/fleet-shipments.xml` |
| Vehicles and drivers: status and maintenance tones, create, edit, delete | `fleet-vehicles.xml`, `fleet-drivers.xml` |
| Tree grid: routes with their stops; selecting a route shows its numbered stops and the road between them on a map (`fill` and `polyline` client actions) | `screen/fleet-routes.xml`, view `FltRteTree`, target `FltRouteMap`, `FleetMapService` |
| Inline-editable grid: assign vehicle and driver to today's routes | `screen/fleet-assignments.xml`, `FltRteAssign` |
| Scheduler: two tasks that run maintain targets | `AweSchTsk` rows of the migration, screens of `awe-scheduler` |
| Notifier: notifications to subscribed users, per customer account | `FltFlagLate`, subscriptions `FLEET` and `CUS1`...`CUS4`, screens of `awe-notifier` |
| Customer area, row-level security on the server | `FltSelfCus`, `FltMy*` queries, `FltMyPckNew`, `FltMyPckCan` |
| Two profiles with permissions on every query and maintain target | `profile/dispatcher.xml`, `profile/customer.xml`, `global/Permissions.xml` |
| Three languages (en-GB, es-ES, fr-FR) | `locale/` |
| Own login: logo (light and dark), background, texts and demo users | `application.properties`, `static/images` |

### Maps

The three maps use the AWE [`map` component](../../website/docs/api/map.md) (Leaflet, OpenStreetMap tiles) and, for the
routes, the `fill` (numbered stops, `marker-number`) and `polyline` (road) client actions returned by
`FleetMapService`. The service never reads the points itself: it asks the AWE queries, so the public points need the
tracking code AND the postal code like the timeline, and the route planner needs the `routes.read` permission. The
coordinates are columns of the seed (`Lat`, `Lon` of `FltReg`, `FltRegStp` and `FltStp`; the vehicle positions are
derived in the view `FltVehMap`). Try the tracking with **MF-T9JSDP6N** and **36919**.

The roads of the route planner (`FltRegGeo`) were precomputed once with OSRM from OpenStreetMap data (© OpenStreetMap
contributors, ODbL) and stored in the seed: no routing service is called at runtime.

The tiles come from the public OpenStreetMap server, which has a
[usage policy](https://operations.osmfoundation.org/policies/tiles/) for light use only. A production deployment must set
`awe.application.map.tile-url` to its own tile server or a commercial provider.

### Scheduler and notifier

Two tasks are seeded in the migration (see them in Scheduler > Tasks):

- **Flag late shipments**, every 5 minutes, runs the maintain `FltFlagLate`. It selects the shipments still `IN_TRANSIT`
  whose estimated arrival has passed and, in this order, inserts one notification for the dispatchers (subscription
  `FLEET`) and one for each affected customer (subscription `CUSn`), adds a `DELAYED` event to the tracking timeline and
  flags the shipments. The notifications are plain rows of the notifier table `AweNot`, inserted from views that return
  no row when nothing is late, and a user sees those of the subscriptions it is subscribed to. The first run flags the
  seeded late shipments (about 7), so the bell of a dispatcher shows a notification and "Delayed shipments" of the dashboard grows.
- **Close past routes**, every hour, runs `FltCloseRoute`.

The scheduler runs without a session, so these targets are `public="true"`. They take no input and are idempotent (they
only touch what is already late or past), which makes calling them again harmless; `publicTargets()` of the test lists
them with that reason. Every other target of the application requires a permission.

### Row-level security

The login (`ope`) of a customer is linked to its account (`FltCus.IdeOpe`). Every query of the customer area filters with
`IdeCus in (FltSelfCus)`, where `FltSelfCus` reads the session user, and the maintain targets take the account from the
same query. The customer profile also restricts the dispatcher screens, and the dispatcher permissions are not granted to it.

### Tests

15 tests, no Spring context and no browser (`mvn test -pl awe-samples/fleet-logistics -Dskip.frontend=true`):

- `FleetLogisticsDescriptorTest` (8) extends `AbstractApplicationDescriptorTest` of `awe-testing`. It checks that every
  XML parses into its AWE model, that every locale key exists in the three languages, that every target a screen
  launches exists and that only the declared targets are public.
- `FleetMapServiceTest` (7) checks the points and the road returned for the maps.

## Look and assets

The look is robust and industrial: a strong blue primary (`awe.application.theme-primary=blue`), square corners
(`--awe-radius-sm`), headings in capitals, thick primary rails on the cards and a full-bleed photo band with a strip of
figures. The CSS (`src/css/main.css`) uses only `--awe-*` tokens; the text over the band sits on a token overlay of the
emphasis text colour, so it keeps its contrast in light and dark. The truck mark of the sidebar logo, the route, the map
and the motion arrows are hand-written SVG files in `static/images/illustrations/`, used as CSS masks coloured with the
primary token. The sidebar logo replaces the AWE one with the truck mark plus the name.

The application has one logo, the truck mark (`static/images/illustrations/truck-mark.svg`), used everywhere: the public top bar
and the private sidebar draw it as a CSS mask in the primary colour next to the name; the sign-in page uses
`static/images/logo/fleet-logistics.svg` and `fleet-logistics-dark.svg` (the same mark plus the name, light and dark); the browser tab uses
`fleet-logistics-icon.svg` (`awe.application.paths.icon-favicon`) and the touch icon `fleet-logistics-icon.png` (`icon-tablet`, a PNG because the
touch icons do not take SVG).

## Photo credits

The photos are free to use under the [Unsplash License](https://unsplash.com/license) and are converted to WebP. They sit
in `src/main/resources/static/images/photos/`.

| File | Photo by | Source page | Licence |
|------|----------|-------------|---------|
| `fleet-truck-road-1200.webp` (1200 px) | Sven Brandsma | https://unsplash.com/photos/YnSWpIPX3pE | Unsplash License |
| `fleet-trucks-highway-600.webp` (600 px) | Bhargav Panchal | https://unsplash.com/photos/P0bVatS8Jdw | Unsplash License |
| `fleet-truck-mountain-1200.webp` (1200 px) | Bailey Alexander | https://unsplash.com/photos/CuCnnDyvFSM | Unsplash License |
| `fleet-warehouse-600.webp` (600 px) | Jacques Dillies | https://unsplash.com/photos/warehouse-shelving-with-cardboard-boxes-jcav1COVvOc | Unsplash License |
