# awe-react-client

React client library for **AWE (Almis Web Engine)** — the Almis web application framework.

## What is this?

`awe-react-client` is the front-end component library that powers AWE-based web applications. It provides the full set of React components, Redux state management, routing, and integration utilities that AWE applications rely on.

> **This package is not a generic standalone widget library.**  
> It is designed to be consumed exclusively within AWE-based applications, which provide the server-side configuration (XML descriptors for screens, queries, maintain actions, services) that drive the client's behaviour at runtime.

## Installation

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
    "awe-react-client": "^2.1.0"
  }
}
```

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
| Source repository | <https://gitlab.com/aweframework/awe-react> |
| Issue tracker | <https://gitlab.com/aweframework/awe-react/-/issues> |
| AWE Framework | <https://gitlab.com/aweframework/awe> |
| Demo / live instance | <https://react-demo.aweframework.com> |

## License

ISC — see [`LICENSE`](https://gitlab.com/aweframework/awe-react/-/blob/master/LICENSE) for details.
