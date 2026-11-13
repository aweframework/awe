---
title: Autenticación en dos pasos
authors:
  - pgarcia
tags:
  - awe
  - 2fa
  - seguridad
  - novedad
---

<img style={{ width: "60%", margin: "10% 20%", padding: "50" }} alt="Código TOTP" src={require('@docusaurus/useBaseUrl').default('img/totp-code.png')} />

{/* truncate */}

Hemos añadido una nueva función de seguridad a nuestro framework: **Autenticación en dos pasos**.

Este desarrollo añade la posibilidad de habilitar un factor de autenticación adicional basado en aplicaciones como **Google Authenticator** para entrar en la aplicación. También hemos añadido la posibilidad de forzar a todos los usuarios a activar este segundo factor para mejorar la seguridad de acceso.

To see more information about this new functionality, you can read about it on [this link](/docs/security/security-authentication#two-factor-authentication-2fa)
