# Beacon Desk: AWE sample application (help desk)

Beacon Desk is an IT and customer support desk. Visitors read the knowledge base, open a ticket through a guided
support chat and check a ticket without signing in; agents and supervisors work the queue (two profiles with different
permissions); requesters follow only their own tickets; integrations use a REST API. It shows how AWE 5 on the React
engine builds a ticket workflow, an application widget and a REST API. Everything is declared in XML
(`src/main/resources/application/helpdesk`); the Java code is the Spring Boot starter and the chat service.

| At a glance | |
|-------------|---|
| URL | <http://localhost:18103> (Swagger UI at `/swagger-ui.html`) |
| Engine | React (AWE 5) |
| Database | HSQLDB file database, created and seeded by Flyway (600 tickets, about 2,000 messages, 25 articles, 6 teams) |
| Main showcase | Guided support chat, server-enforced workflow, REST API |
| Languages | en-GB, es-ES, fr-FR |

## Features

**Public portal (no sign-in)**

- Support portal: service status and popular articles: `hd-home.xml`.
- Knowledge base: published articles only, read only: `hd-kb.xml`.
- Support chat: a scripted assistant suggests articles and opens a ticket; it answers with the ticket code and its
  access key: widget `support-chat`, `HdkChatTicket` (see [Support chat](#support-chat)).
- Check a ticket by access key AND requester email; only public replies are shown: `hd-check.xml`.

**Agent area**

- Your queue: filters, group selector (status, priority, team), status tones, print and export: `hd-queue.xml`.
- Workflow in the queue panel: **Take**, **Waiting for requester**, **Resolve**, **Close the ticket**, **Reopen**;
  each transition is enforced on the server (NEW > OPEN > PENDING > RESOLVED > CLOSED).
- Conversation in the same panel: public replies and internal notes (`HdkMsgNew`).
- New ticket wizard in 4 steps (requester, category, details, review): `hd-new-ticket.xml`.
- Knowledge base, read only for agents.

**Supervisor area (everything of the agent, plus)**

- Service pulse: SLA dashboard with tickets per day, backlog by priority, resolution time per team and an SLA donut:
  `hd-dashboard.xml`.
- Reassign tickets: inline-editable grid for assignment and priority, critical included: `hd-assign.xml`.
- Teams as a tree grid: `hd-teams.xml`. Knowledge base editing: `hd-kb-admin.xml`.

**Requester area**

- My tickets: own tickets only, with the access key, replies and closing: `hd-my-tickets.xml`.

**Integrations**

- REST API: `/api/authenticate`, `/api/data/<query>`, `/api/maintain/<target>`, with the same permissions as the web
  profiles (see [REST API walkthrough](#rest-api-walkthrough)).

## Deploy

### Prerequisites

- JDK 21 and Maven. The build downloads Node for the React bundle, so no Node install is needed.
- Port 18103 free (see [Configuration](#configuration) to change it).

### Build and run

Run from the repository root:

```bash
# Build the application and the framework modules it needs (first time; skip the tests to save time)
mvn -pl awe-samples/help-desk -am install -DskipTests

# Run it from Maven ...
mvn -f awe-samples/help-desk/pom.xml spring-boot:run

# ... or run the jar the build produces
java -jar awe-samples/help-desk/target/help-desk.jar
```

Open <http://localhost:18103>. Add `-Dskip.frontend=true` to build or test the descriptors without the frontend
(`mvn test -pl awe-samples/help-desk -Dskip.frontend=true`).

### What the first start creates

An HSQLDB file database at `target/db/help-desk` (`spring.datasource.url=jdbc:hsqldb:file:target/db/help-desk`, user
`sa`, empty password). The path is relative to the working directory, so `java -jar` creates it under the directory
where you run it. Flyway runs the AWE migrations and the help desk ones (`HELPDESK`) and generates 600 tickets over 60
days, about 2,000 messages, 25 articles and 6 teams, relative to the first start.

### Configuration

Every property of `src/main/resources/application.properties` can be overridden with a Spring mechanism: an
environment variable (`SERVER_PORT`), a command-line argument (`--server.port=9000`) or an `application.properties`
next to the jar.

| Property | Default | Purpose |
|----------|---------|---------|
| `server.port` | `18103` | HTTP port (`SERVER_PORT`) |
| `spring.datasource.url` | `jdbc:hsqldb:file:target/db/help-desk` | Database location (`SPRING_DATASOURCE_URL`) |
| `awe.security.master-key` | sample value | Encrypts stored data. Set `AWE_MASTER_KEY` |
| `awe.rest.api.jwt.secret` | sample value | Signs the REST tokens. Set `AWE_JWT_SECRET` |
| `awe.rest.api.jwt.expiration-time` | `60m` | Lifetime of a REST token |
| `awe.rest.api.auth.mode` | `local-jwt` | REST authentication mode |
| `awe.application.theme-primary` | `violet` | Primary colour of the theme |

The chat and the REST API need no other service: the chat is scripted (no AI and no external service).

### Reset the demo data

Stop the application, delete `target/db/help-desk*` (relative to the working directory it ran in) and start it again.

### Behind a reverse proxy or on another port

Set `SERVER_PORT` (or `--server.port`) to the port you want and point the proxy to it. The application serves at the
root path (`server.servlet.context-path=/`), so proxy the whole host or a dedicated host name, not a sub-path. For the REST
API, call the proxied host instead of `localhost:18103` in the examples.

### Before using this outside a demo

- Set the master key with the environment variable `AWE_MASTER_KEY` and the REST signing secret with `AWE_JWT_SECRET`
  (`application.properties` reads `${AWE_MASTER_KEY:...}` and `${AWE_JWT_SECRET:...}` and falls back to sample values that
  are public in this repository). Anyone who knows the sample secret can forge a REST token.
- Deactivate or delete the demo users (`alex`, `blake`, `casey`, `sam`, `emma`, `liam`, `olivia`) and above all the
  `test` / `test` administrator, then create your own users and passwords.
- `/v3/api-docs` (the OpenAPI document of the REST API) is public by default in this sample. Restrict it, or disable it,
  when the API is exposed.
- The public check of a ticket needs the access key and the email, but it is still an anonymous query: put a rate limit in
  front of it.
- The support chat allows 3 tickets per session, but a client that drops its cookies starts a new session on every
  request. Put a rate limit by address (or a CAPTCHA) in front of `HdkChatTicket` too.
- Replace the HSQLDB file database with a managed database for real data, and back it up.

## Use it

### Demo users

The home page of the application shows this same table in its **Try the demo** section, and the sign-in page repeats one pair per role.

| User | Password | What this user sees |
|------|----------|---------------------|
| `alex` | `helpdemo` | Agent: the queue, the new ticket wizard and the knowledge base (read only). |
| `blake` | `helpdemo` | Agent: the queue, the new ticket wizard and the knowledge base (read only). Language: Spanish. |
| `casey` | `helpdemo` | Agent: the queue, the new ticket wizard and the knowledge base (read only). Language: French. |
| `sam` | `helpdemo` | Supervisor: everything of the agent plus the SLA dashboard, reassignment, critical priority, teams and knowledge base changes. |
| `emma` | `deskdemo` | Requester: only their own tickets. |
| `liam` | `deskdemo` | Requester: only their own tickets. Language: Spanish. |
| `olivia` | `deskdemo` | Requester: only their own tickets. Language: French. |
| `test` | `test` | AWE administrator: users, profiles and screen access. Demo only. |

Agents and supervisors work with the sequential code (`HD-1`, `HD-2`, ...). The **public** check does not accept it: it asks
for the ticket's **access key** (8 characters, derived from the ticket key by a fixed scrambling formula, so it is
deterministic in the seed but cannot be guessed by counting) and the email of the requester. The requester sees the key in
**My tickets** (column "Access key"); agents see it in the ticket panel of the queue.
Try the public check with the access key **G6AP6EHR** (ticket HD-1) and the email `ava.parker@contoso.example`; any other
combination returns nothing. In production, rate-limit the public check.

The `test` / `test` administrator comes from the AWE schema and is **demo-only**: it is kept so that the AWE administration
screens can be shown and it is not deactivated. All these are sample credentials: see
[Before using this outside a demo](#before-using-this-outside-a-demo).

### Tour 1: visitor

1. Signed out, open <http://localhost:18103>: the portal shows the service status and popular articles.
2. Open the floating chat button and describe a problem. The assistant offers up to three articles; if they do not solve it,
   answer the category and email questions and confirm. You get the ticket code and its access key.
3. Open **Check a ticket** and enter that access key and email (or **G6AP6EHR** with `ava.parker@contoso.example` for the
   seeded ticket HD-1) to see its status and public replies.
4. **Knowledge base** lists the published articles.

### Tour 2: agent

1. Sign in as `alex` / `helpdemo`. **Your queue** opens first: filter it, or group it by status, priority or team.
2. Select a ticket to open its panel: read the conversation and the access key.
3. Use **Take** (NEW > OPEN), write a **Reply** (public) or an internal note, then **Resolve**.
4. Transitions are checked by the server: a ticket in the wrong state is left untouched (try it in the
   [REST API walkthrough](#rest-api-walkthrough)).
5. **New ticket** runs the 4-step wizard (requester, category, details, review).

### Tour 3: supervisor

1. Sign in as `sam` / `helpdemo`. **Service pulse** shows the SLA dashboard.
2. **Reassign tickets**: change the agent or the priority in the grid (critical is allowed here) and save.
3. **Teams** shows the tree of teams; the knowledge base screen lets you create, edit and delete articles.

### Tour 4: requester

1. Sign in as `emma` / `deskdemo` and open **My tickets**: only the tickets of this requester appear, with their access key.
2. Open one to read the replies, answer, or close it.

### REST API walkthrough

The module `awe-rest-spring-boot-starter` publishes `/api/authenticate`, `/api/data/<query>` and `/api/maintain/<target>`
(JWT signed with `awe.rest.api.jwt.secret`, 60 minutes), authorized by the same permissions of the profile of the user. No target is
public beyond the ones of the portal. Swagger UI is at `/swagger-ui.html`. Targets that run with `multiple="true"` (the
workflow ones) take their values as lists: `"IdeTkt": [602]`. Run against the running application:

```bash
# 1. Authenticate (JSON body) and keep the token
TOKEN=$(curl -s -X POST http://localhost:18103/api/authenticate -H "Content-Type: application/json" \
  -d '{"username":"alex","password":"helpdemo"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 2. Create a ticket for the requester 2 (the same target as the wizard)
curl -s -X POST http://localhost:18103/api/maintain/HdkTktNew -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"parameters":{"IdeReq":2,"Cat":"SOFTWARE","Pri":"HIGH","Chn":"API","Sub":"Monitoring: CRM export timeout","Dsc":"Opened by the monitoring integration."}}'
# {"type":"OK","title":"Operation successful",...,"resultDetails":[{"operationType":"UPDATE","rowsAffected":0,"parameterMap":{...,"NewTkt":602}}]}
# the new key (602) is in parameterMap.NewTkt; the code of the ticket is HD-602

# 3. Query its status
curl -s -X POST http://localhost:18103/api/data/HdkTktByCode -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"parameters":{"Cod":"HD-602"}}'
# {"type":"OK",...,"dataList":{"total":1,"page":1,"records":1,"rows":[{"Cod":"HD-602","StaTxt":"New","PriTxt":"High","SlaTxt":"Within SLA","ReqNam":"Liam Walker",...}]}}

# 4. Move it along the workflow: take it (NEW > OPEN), then try to close it (not allowed from OPEN: 0 rows affected)
curl -s -X POST http://localhost:18103/api/maintain/HdkTktTake  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"parameters":{"IdeTkt":[602]}}'   # rowsAffected 1
curl -s -X POST http://localhost:18103/api/maintain/HdkTktClose -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"parameters":{"IdeTkt":[602]}}'   # rowsAffected 0

# 5. Priorities: an agent can set HIGH but never CRITICAL (the server refuses it: HTTP 400), only the supervisor can (HdkTktPrioAny)
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:18103/api/maintain/HdkTktPrio -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"parameters":{"IdeTkt":[602],"Pri":"HIGH"}}'      # OK
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:18103/api/maintain/HdkTktPrio -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"parameters":{"IdeTkt":[602],"Pri":"CRITICAL"}}'  # ERROR, HTTP 400

# 6. The permissions apply: an agent cannot reassign (HTTP 403), the supervisor can; without a token it is HTTP 401
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:18103/api/maintain/HdkTktAssign -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"parameters":{"IdeTkt":[602],"IdeAgt":[1]}}'
# {"type":"ERROR","title":"Access denied","message":"You do not have permission to perform this action..."}  HTTP 403
```

Workflow transitions are enforced by the `WHERE` of each maintain target, one or several source states, for one or many
tickets in the same call: `HdkTktTake` NEW > OPEN, `HdkTktPend` OPEN > PENDING, `HdkTktResolve` OPEN or PENDING > RESOLVED,
`HdkTktClose` RESOLVED > CLOSED, `HdkTktReopen` RESOLVED or PENDING > OPEN. A ticket in any other state is left untouched.
The targets with two source states use an `or` of constant filters (a list variable does not work in the `multiple` mode).

A ticket created through REST by an agent is created for the requester you name; the agent of `HdkTktTake` comes from the web
session user, so a ticket taken through REST stays without agent until a supervisor assigns it.

Note: `awe.security.permissions.require-undeclared=true` would also close the AWE targets that declare no permission when
the REST API is exposed, but it removes the Login button of the sign-in page, so it stays off. Every target of this
application declares its permission.


## How it is built

| Feature | Screen or file |
|---------|----------------|
| Public portal: service status, popular articles | `screen/hd-home.xml`, public queries `HdkSvcPub`, `HdkArtPub` |
| Public knowledge base, published articles only, read only | `screen/hd-kb.xml`, `HdkArtPub` (the `Pub = 1` filter is a constant of the query) |
| Support chat: a visitor opens a ticket without signing in | custom widget `support-chat` (`src/js/chat/`), `HdkChatTicket`, see "Support chat" |
| Public ticket check: code AND requester email, only that ticket | `screen/hd-check.xml`, `HdkStatusPub`, `HdkRepliesPub` (internal notes are not in the view) |
| Ticket queue: filters, group selector (status, priority, team), status tones, print and export | `screen/hd-queue.xml` |
| Workflow NEW > OPEN > PENDING > RESOLVED > CLOSED, enforced on the server | `HdkTktTake`, `HdkTktPend`, `HdkTktResolve`, `HdkTktClose`, `HdkTktReopen`: each updates only from the allowed previous states (`WHERE Sta in (...)`) |
| Conversation: public replies and internal notes | side panel of the queue, `HdkMsgNew` |
| New ticket wizard in 4 steps: requester, category, details, review | `screen/hd-new-ticket.xml`, `HdkTktNew` |
| Teams as a tree grid | `screen/hd-teams.xml`, view `HdkTeamTree` |
| SLA dashboard: tickets per day with zoom, backlog by priority, resolution time per team, SLA donut | `screen/hd-dashboard.xml` |
| Inline-editable grid to reassign tickets and change priority | `screen/hd-assign.xml` (supervisors) |
| Per-profile permissions | `global/Permissions.xml`, `profile/agent.xml`, `profile/supervisor.xml`, `profile/requester.xml` |
| Requester area, row-level security on the server | `HdkSelfReq`, `HdkMy*` queries, `HdkMyTktNew`, `HdkMyReply`, `HdkMyClose` |
| REST API for integrations | see below |
| Own login: logo (light and dark), background, texts and demo users, three languages | `application.properties`, `static/images`, `locale/` |

### Support chat

The portal has a floating chat button. A scripted assistant (no AI and no external service) asks what is wrong, looks in the
knowledge base (`HdkArtPub`) and shows up to three articles; if they do not solve it, it asks for the category
(`HdkCatPub`) and an email, shows a summary and opens the ticket. The answer is the ticket code and its access key, which
with the email open the public ticket check. The chat is an application widget: `src/js/main.js` registers it with
`registerWidget("support-chat", ...)` and `hd-home.xml` places it with `<widget type="support-chat"/>`, so the engine is
not changed. The conversation is a pure module (`src/js/chat/chatFlow.mjs`).

The only public maintain is `HdkChatTicket` (Java service `ChatTicketService`). It never trusts the client: a hidden
honeypot field that a person leaves empty (a bot that fills it gets a normal answer and no ticket), 3 tickets per session,
a valid email, a category of the enumerated list and a description of 5 to 1000 characters. A visitor is a requester
identified by its email (the existing one or a new one, `HdkChatReqNew`). Migration `HELPDESK_V1.0.1` adds the key sequence
of the requesters.

### Agent and supervisor

The two profiles differ by permissions, which the queries, the maintain targets and the buttons check:

| Permission | Agent | Supervisor | Used by |
|------------|:-----:|:----------:|---------|
| `tickets.read`, `tickets.create`, `tickets.update`, `kb.read` | yes | yes | queue, workflow, replies, wizard, reading the knowledge base |
| `tickets.reassign` | no | yes | `HdkTktAssign` (screen Reassign tickets) |
| `tickets.critical` | no | yes | `HdkTktPrioAny`: any priority, critical included. The agents' `HdkTktPrio` takes the new priority from the server-only query `HdkPriAgent`, which reads the lookup table `HdkPri` (`Agt = 0` for CRITICAL): CRITICAL resolves to no value and the update is refused, whatever the client sends. The wizard lowers a critical ticket to HIGH. The queue panel offers both buttons, and the supervisor one carries `permission="tickets.critical"` |
| `kb.create`, `kb.update`, `kb.delete` | no | yes | the buttons of the knowledge base screen are removed for the agents |
| `reports.read`, `teams.read` | no | yes | SLA dashboard and teams |

The descriptor test `HelpDeskDescriptorTest` parses the descriptors and proves that every supervisor-only target declares its
permission and that the agent profile does not grant it.

### Tests

24 Java tests plus 21 Node tests (`mvn test -pl awe-samples/help-desk -Dskip.frontend=true` runs the Java ones; `npm test`
in the module runs the Node ones, which also run in the test phase of the build; needs Node):

- `HelpDeskDescriptorTest` (11) extends `AbstractApplicationDescriptorTest` of `awe-testing`: plain JUnit, no Spring context
  and no browser. It checks that every XML parses into its AWE model, that every locale key exists in the three
  languages, that every target a screen launches exists, that only the five public reads and the support chat maintain are
  public and that the supervisor-only targets require supervisor permissions.
- `ChatTicketRulesTest` (8) and `ChatTicketServiceTest` (5) check the rules of the chat on the server (honeypot, limit,
  validation, requester and ticket created with their access key).
- `chatFlow.test.mjs` (21) runs with Node's own test runner (no extra dependency) and checks the conversation of the chat.

## Look and assets

The look is calm and modern: a violet primary (`awe.application.theme-primary=violet`), a centred hero with a search-like
pill, soft gradients built from the tokens and generous white space. The CSS (`src/css/main.css`) uses only `--awe-*`
tokens. The beacon mark of the sidebar logo, the chat bubbles and the signal waves are hand-written SVG files in
`static/images/illustrations/`, used as CSS masks coloured with the primary token. The sidebar logo replaces the AWE one
with the beacon mark plus the name.

The application has one logo, the beacon mark (`static/images/illustrations/beacon-mark.svg`), used everywhere: the public top bar
and the private sidebar draw it as a CSS mask in the primary colour next to the name; the sign-in page uses
`static/images/logo/help-desk.svg` and `help-desk-dark.svg` (the same mark plus the name, light and dark); the browser tab uses
`help-desk-icon.svg` (`awe.application.paths.icon-favicon`) and the touch icon `help-desk-icon.png` (`icon-tablet`, a PNG because the
touch icons do not take SVG).

## Photo credits

The photos are free to use under the [Unsplash License](https://unsplash.com/license) and are converted to WebP. They sit
in `src/main/resources/static/images/photos/`.

| File | Photo by | Source page | Licence |
|------|----------|-------------|---------|
| `hd-agent-1200.webp` (1200 px) | BaljkanN4 | https://unsplash.com/photos/wnpf3Q5pkXA | Unsplash License |
| `hd-team-600.webp` (600 px) | Charanjeet Dhiman | https://unsplash.com/photos/mHusyBu4bxM | Unsplash License |
| `hd-office-600.webp` (600 px) | Arlington Research | https://unsplash.com/photos/kN_kViDchA0 | Unsplash License |
| `hd-laptop-600.webp` (600 px) | Scott Graham | https://unsplash.com/photos/5fNmWej4tAA | Unsplash License |
