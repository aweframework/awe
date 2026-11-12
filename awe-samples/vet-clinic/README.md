# Happy Paws: AWE sample application (veterinary clinic)

Happy Paws is a small veterinary clinic. Visitors read the public site and request an appointment without signing in;
veterinarians run the clinic (owners, pets, visits, vaccinations, invoices); pet owners follow only their own pets,
appointments and invoices. It shows how AWE 5 on the React engine builds an application only from XML descriptors
(`src/main/resources/application/vetclinic`): reports and charts, master-detail grids, styled PDF invoices, file upload
and row-level security. The only Java code starts Spring Boot and renders the invoice PDF.

| At a glance | |
|-------------|---|
| URL | <http://localhost:18101> |
| Engine | React (AWE 5) |
| Database | HSQLDB file database, created and seeded by Flyway on first start |
| Main showcase | Dashboard charts, print and export of lists, PDF invoices |
| Languages | en-GB, es-ES, fr-FR |

## Features

**Public site (no sign-in)**

- Landing page with the sign-in and request buttons and the demo credentials: `vet-home.xml`.
- Appointment request form (name, email, phone, pet, species, preferred date, reason, notes). It only inserts a pending request,
  and the database constraints check it: `vet-request.xml`, target `VetReqNew`.

**Staff area (veterinarians)**

- Dashboard "Today's patients": cards and charts (columns, area, donut): `vet-dashboard.xml`.
- Owners and pets with master-detail grids; a pet shows its medical history; side panel to create and edit:
  `vet-owners.xml`, `vet-pets.xml`.
- Visits: search by period, type and vet; **New visit** creates the visit and its invoice in one maintain
  (`VetVisNew`); print and export the list: `vet-visits.xml`.
- Appointment book with status tones and actions on the selected rows (confirm, mark as done, cancel):
  `vet-appointments.xml`.
- Web requests: review the public requests and mark them as handled or reject them: `vet-requests.xml`.
- Vaccinations: register and search, with the applied date and the next dose: `vet-vaccinations.xml`.
- Documents: upload files of a pet with a drop zone: `vet-documents.xml`.
- Invoices: detail in a side panel (lines and totals), **Mark as paid**, **Download PDF**:
  `vet-invoices.xml`, `InvoiceService`, `InvoicePdfRenderer`.

**Owner area**

- "My pets' corner" with the next appointment, pets, vaccinations due and invoices due: `vet-my-home.xml`.
- Own pets, vaccinations and appointments; request or cancel an appointment: `vet-my-pets.xml`,
  `vet-my-vaccinations.xml`, `vet-my-appointments.xml`.
- Own invoices with the PDF download: `vet-my-invoices.xml`.

**Administration**

- The `test` user manages users, profiles and screen access with the AWE administration screens.

## Deploy

### Prerequisites

- JDK 21 and Maven. The build downloads Node for the React bundle, so no Node install is needed.
- Port 18101 free (see [Configuration](#configuration) to change it).

### Build and run

Run from the repository root:

```bash
# Build the application and the framework modules it needs (first time; skip the tests to save time)
mvn -pl awe-samples/vet-clinic -am install -DskipTests

# Run it from Maven ...
mvn -f awe-samples/vet-clinic/pom.xml spring-boot:run

# ... or run the jar the build produces
java -jar awe-samples/vet-clinic/target/vet-clinic.jar
```

Open <http://localhost:18101>. Add `-Dskip.frontend=true` to build or test the descriptors without the frontend
(`mvn test -pl awe-samples/vet-clinic -Dskip.frontend=true`).

### What the first start creates

An HSQLDB file database at `target/db/vet-clinic` (`spring.datasource.url=jdbc:hsqldb:file:target/db/vet-clinic`, user
`sa`, empty password). The path is relative to the working directory, so `java -jar` creates it under the directory
where you run it. Flyway runs the AWE migrations and the clinic ones
(`src/main/resources/db/migration/hsqldb/VETCLINIC_V1.0.0__Init_vet_clinic.sql`) and loads the demo data.

### Configuration

Every property of `src/main/resources/application.properties` can be overridden with a Spring mechanism: an
environment variable (`SERVER_PORT`), a command-line argument (`--server.port=9000`) or an `application.properties`
next to the jar.

| Property | Default | Purpose |
|----------|---------|---------|
| `server.port` | `18101` | HTTP port (`SERVER_PORT`) |
| `spring.datasource.url` | `jdbc:hsqldb:file:target/db/vet-clinic` | Database location (`SPRING_DATASOURCE_URL`) |
| `awe.security.master-key` | sample value | Encrypts stored data. Set `AWE_MASTER_KEY` |
| `awe.application.language-default` | `en-GB` | Language before the user chooses one |
| `awe.application.theme-primary` | `orange` | Primary colour of the theme |
| `awe.security.authorization.mode` | `enforce` | Every target declares a permission and is checked |

The sample sends no email: the PDF invoices are generated in the application and downloaded from the browser.

### Reset the demo data

Stop the application, delete `target/db/vet-clinic*` (relative to the working directory it ran in) and start it again.

### Behind a reverse proxy or on another port

Set `SERVER_PORT` (or `--server.port`) to the port you want and point the proxy to it. The application serves at the
root path (`server.servlet.context-path=/`), so proxy the whole host or a dedicated host name, not a sub-path.

### Before using this outside a demo

- Set the master key with the environment variable `AWE_MASTER_KEY` (`application.properties` reads
  `${AWE_MASTER_KEY:...}` and falls back to a sample value that is public in this repository). Anything encrypted with the
  sample key can be decrypted by anyone.
- Deactivate or delete the demo users (`dr.garcia`, `dr.lopez`, `ana`, `luis`, `marie`) and above all the `test` / `test`
  administrator, then create your own users and passwords.
- The public appointment request (`VetReqNew`) accepts unlimited anonymous inserts, a demo simplification so that the
  landing page works without sign-in. A real deployment needs a CAPTCHA and a rate limit in front of it (reverse proxy or
  gateway).
- Replace the HSQLDB file database with a managed database for real data, and back it up.
- Owner notes: staff write the internal note (`Nte`) of an appointment. The owner area never reads it: `VetMyAptLst` shows
  `PubNte`, a column of the view `VetAptLst` that is only filled when the owner wrote the appointment (`Src = 'OWNER'`).

## Use it

### Demo users

The home page of the application shows this same table in its **Try the demo** section, and the sign-in page repeats one pair per role.

| User | Password | What this user sees |
|------|----------|---------------------|
| `dr.garcia` | `vetdemo` | Veterinarian: the whole clinic (patients, visits, invoices, dashboard). |
| `dr.lopez` | `vetdemo` | Veterinarian: the whole clinic (patients, visits, invoices, dashboard). Language: Spanish. |
| `ana` | `petdemo` | Pet owner: only their own pets, appointments and invoices. Language: Spanish. |
| `luis` | `petdemo` | Pet owner: only their own pets, appointments and invoices. |
| `marie` | `petdemo` | Pet owner: only their own pets, appointments and invoices. Language: French. |
| `test` | `test` | AWE administrator: users, profiles and screen access. Demo only. |

The `test` / `test` administrator comes from the AWE schema and is **demo-only**: the sample keeps it so that the AWE
administration screens can be shown, and it is not deactivated. All these are sample credentials: see
[Before using this outside a demo](#before-using-this-outside-a-demo).

### Tour 1: visitor to paid invoice

1. Signed out, open <http://localhost:18101> and choose **Request an appointment**. Fill the form and send it.
2. Sign in as `dr.garcia` / `vetdemo`. Open **Web requests** under "The clinic": the new request is there. Select it
   and use **Mark as handled** (or **Reject**).
3. Open **Visits** and choose **New visit**: pick one of the pets of `ana`, a vet, type, diagnosis and fee, then create. The visit and
   its invoice are saved together.
4. Open **Invoices**, select the new invoice to see its lines and totals in the side panel, then **Mark as paid**.
5. Sign in as `ana` / `petdemo` (the owner of the pet) and open **My invoices**; use **Download PDF** to get the
   styled invoice.

### Tour 2: the veterinarian

1. `dr.garcia`: start at **Today's patients** (dashboard cards and charts).
2. **Owners**, then **Our patients**: open a pet to see its medical history and edit it in the side panel.
3. **Appointment book**: use the actions on the selected rows (**Confirm**, **Mark as done**, **Cancel**).
4. **Visits**: set a period, search, then print or export the list.
5. **Documents**: upload a file with the drop zone.

### Tour 3: the pet owner

1. `luis` / `petdemo`: open **My pets' corner** (next appointment, vaccinations due, invoices due).
2. **My appointments**: request an appointment for one of your pets, then cancel it.
3. **My pets**, **Vaccinations**, **My invoices**: only the records of this owner appear, whatever the screen asks for.

## How it is built

| Feature | Screen or file |
|---------|----------------|
| Public initial screen, no sign-in | `menu/public.xml`, `screen/vet-home.xml` (styles in `src/css/main.css`, AWE tokens only) |
| Public form with a public maintain that only inserts a pending request, checked by the database | `screen/vet-request.xml`, `VetReqNew`, constraints of `VetReq` |
| Dashboard: cards and ECharts charts (columns, area, donut) | `screen/vet-dashboard.xml` |
| Master-detail: owners with their pets, pets with their medical history (nested grids), side panel to create and edit | `vet-owners.xml`, `vet-pets.xml` |
| Rich criteria: date range, selects from enumerated lists and queries, suggest, numeric with currency, time, validation | `vet-visits.xml`, `vet-appointments.xml`, `vet-vaccinations.xml` |
| One maintain with two inserts (visit and its invoice) sharing the generated key | `VetVisNew` |
| Status tones, actions on the selected rows | `vet-appointments.xml`, `vet-requests.xml`, `vet-invoices.xml` |
| File upload (uploader criterion with drop zone) | `vet-documents.xml` |
| Print and export of a list | `vet-visits.xml`, `vet-invoices.xml`, `vet-my-invoices.xml` (print dialog `PrnOpt`) |
| Invoice detail in a side panel (lines and totals) and a styled PDF of the invoice (OpenPDF, downloaded with `server-download`); owners only reach their own invoices | `vet-invoices.xml`, `vet-my-invoices.xml`, `VetMyInvHdr`, `InvoiceService`, `InvoicePdfRenderer` |
| Two profiles, permissions on every query and maintain, screen access | `profile/vet.xml`, `profile/owner.xml`, `global/Permissions.xml` |
| Row-level security of the pet owner area, in the queries and maintain targets (not only in the screens) | `VetSelfOwn`, `VetMy*` queries, `VetMyAptNew`, `VetMyAptCan` |
| Three languages (en-GB, es-ES, fr-FR); the demo users use different ones | `locale/` |
| Light and dark, primary colour and mode from properties | `awe.application.theme-*` |

### Row-level security

The login (`ope`) of a pet owner is linked to its owner record (`VetOwn.IdeOpe`). Every query of the owner area filters
with `IdeOwn in (VetSelfOwn)`, where `VetSelfOwn` reads the session user, and the maintain targets take the owner from
the same query (`VetMyAptNew` stores the pet only if `VetMyPetCheck` finds it among the pets of the connected owner;
`VetMyAptCan` cancels only open appointments of the owner). The owner profile also restricts the staff screens, and the
staff queries require permissions that the owner profile does not have.

### Public targets

Only three targets run without a session: the species and reason lists of the request form (`VetSpcPub`,
`VetReaPub`) and the request (`VetReqNew`). The descriptor test fails when another target becomes public.

### Tests

14 tests, no Spring context and no browser (`mvn test -pl awe-samples/vet-clinic -Dskip.frontend=true`):

- `VetClinicDescriptorTest` (10) extends `AbstractApplicationDescriptorTest` of `awe-testing`. It checks that every XML
  parses into its AWE model, that every locale key exists in the three languages, that every target a screen launches
  exists and that only the declared targets are public.
- `InvoiceServiceTest` (2) and `InvoicePdfRendererTest` (2) check the invoice data and the generated PDF.

## Look and assets

The look is warm and playful: the primary colour is `orange` (`awe.application.theme-primary`; the framework generates the
scale and keeps the text contrast), the shapes are round and the cards carry a pinch of colour from the semantic tokens.
The CSS (`src/css/main.css`) uses only `--awe-*` tokens. The paw mark of the sidebar logo, the blob, the paw prints, the
bone and the wave are hand-written SVG files in `static/images/illustrations/`, used as CSS masks so they follow the theme
in light and dark. The sidebar logo replaces the AWE one with the paw mark in the primary colour plus the name as text.

The application has one logo, the paw mark (`static/images/illustrations/paw-mark.svg`), used everywhere: the public top bar
and the private sidebar draw it as a CSS mask in the primary colour next to the name; the sign-in page uses
`static/images/logo/vet-clinic.svg` and `vet-clinic-dark.svg` (the same mark plus the name, light and dark); the browser tab uses
`vet-clinic-icon.svg` (`awe.application.paths.icon-favicon`) and the touch icon `vet-clinic-icon.png` (`icon-tablet`, a PNG because the
touch icons do not take SVG).

## Photo credits

The photos are free to use under the [Unsplash License](https://unsplash.com/license) and are converted to WebP (the
width is the one stored in the repository). They sit in `src/main/resources/static/images/photos/`.

| File | Photo by | Source page | Licence |
|------|----------|-------------|---------|
| `dog-1200.webp` (1200 px) | Faber Leonardo | https://unsplash.com/photos/a-golden-retriever-is-sitting-and-smiling-for-the-camera-CLhFS67ni1c | Unsplash License |
| `cat-puppy-2000.webp` (2000 px, sign-in) | Andrew S | https://unsplash.com/photos/ouo1hbizWwo | Unsplash License |
| `cat-600.webp` (600 px) | Ivan Lopatin | https://unsplash.com/photos/a-close-up-of-a-cat-with-big-blue-eyes-7KGaAh7OaA8 | Unsplash License |
| `vet-exam-600.webp` (600 px) | Alexander Mass | https://unsplash.com/photos/I3KxEBS6iOc | Unsplash License |
| `vet-dachshund-600.webp` (600 px) | Alexander Mass | https://unsplash.com/photos/e4f87NzUJsU | Unsplash License |
| `puppy-1200.webp` (1200 px) | Haberdoedas | https://unsplash.com/photos/a-happy-golden-retriever-puppy-looking-up-ZfH0aYWwHCE | Unsplash License |
