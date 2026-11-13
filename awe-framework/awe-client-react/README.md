# awe-react-client

React client library for **AWE (Almis Web Engine)** — the Almis web application framework.

## What is this?

`awe-react-client` is the front-end component library that powers AWE-based web applications. It provides the full set of React components, Redux state management, routing, and integration utilities that AWE applications rely on.

> **This package is not a generic standalone widget library.**  
> It is designed to be consumed exclusively within AWE-based applications, which provide the server-side configuration (XML descriptors for screens, queries, maintain actions, services) that drive the client's behaviour at runtime.

## Installation

The package version follows the AWE Framework version exactly: use the `awe-react-client` version that matches the AWE version of your application (for example, AWE `5.0.0` uses `awe-react-client@5.0.0`). Prerelease versions are published under the `next` dist-tag, final versions under `latest`.

```bash
npm install awe-react-client
# or
yarn add awe-react-client
```

## Usage

`awe-react-client` is intended to be used as the client-side dependency of an AWE Spring Boot application built with the [AWE Framework](https://gitlab.com/aweframework/awe).  
The library entry point is resolved automatically when your AWE project bundles the front end via Webpack.

Typical consumer setup (inside an AWE-based project's `package.json`):

```json
{
  "dependencies": {
    "awe-react-client": "5.0.0"
  }
}
```

Pin the exact version (or a `5.x` range if your project updates the framework and the client together): the client and the server of an AWE application are released together and are tested only in matching versions.

Upgrading from `awe-react-client` 2.x? See the [React client upgrade guide](https://docs.aweframework.com/docs/guides/react-client-upgrade).

## Key capabilities

- AWE screen, grid, chart, form and wizard components
- Redux Toolkit store wired to AWE's server-side action/maintain system
- i18n support via `i18next`
- Real-time updates over STOMP/WebSocket
- PrimeReact-based UI component layer
- Apache ECharts integration for data visualisation

## Project links

| Resource | URL |
|----------|-----|
| Source repository | <https://gitlab.com/aweframework/awe> (`awe-framework/awe-client-react`) |
| Issue tracker | <https://gitlab.com/aweframework/awe/-/issues> |
| Documentation | <https://docs.aweframework.com> |
| 2.x changelog (history) | <https://gitlab.com/aweframework/awe/-/blob/master/awe-framework/awe-client-react/CHANGELOG-2.x.md> |

## License

Apache License 2.0, like the rest of AWE — see [`LICENSE.md`](https://gitlab.com/aweframework/awe/-/blob/master/LICENSE.md) (also included in the published package).
