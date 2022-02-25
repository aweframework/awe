---
title: Two-factor authentication
author: Pablo García
author_title: Senior Software Engineer @ Almis
author_url: https://gitlab.com/pablo.garcia.almis
author_image_url: https://gitlab.com/uploads/-/system/user/avatar/3234791/avatar.png?width=400
tags: [awe, 2fa, seguridad, novedad]
---

<img style={{ width: "60%", margin: "10% 20%", padding: "50" }} 
    alt="TOTP Code" 
    src={require('@docusaurus/useBaseUrl').default('img/totp-code.png')}
/>

We have added a new security function to our framework: **Two-factor authentication**.

This development adds the possibility to enable an extra authentication factor based on apps like **Google Authenticator** to
enter the application. We have also added the possibility to force all the users to activate this second factor to improve
access security.

To see more information about this new functionality, you can read about it on [this link](/docs/security#two-factor-authentication-2fa)
