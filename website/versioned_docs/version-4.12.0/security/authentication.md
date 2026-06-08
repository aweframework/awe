---
id: security-authentication
title: Authentication
sidebar_label: Authentication
---

<img style={{ width: "40%", margin: "10% 30% 10% 30%" }}
alt="AWE security"
src={require('@docusaurus/useBaseUrl').default('img/undraw_security-on_btwg.svg')}
/>

# Authentication and Authorization
Awe lets you choose which authentication and authorization system you want to use, instead of bundling any specific one.
Awe is fully compatible with the most used security solutions in the Spring Boot ecosystem like `In memory`, `Database`, `LDAP`, `OAuth`, `Oauth2`, ...

:::info You can visit [this](https://spring.io/guides/topicals/spring-security-architecture) for more info.:::

## Spring Security in Awe
Awe provides configuration beans to manage security in your application. You can use them or overwrite and create your custom auth method.
The security configuration is in `SecurityConfig` and `AWEScreenSecurityAdapter` classes and select the authentication method that you want.

```shell title="Configuration properties"
################################################
# Authentication
################################################
# Authentication mode (ldap | bbdd | in_memory | custom)
awe.security.auth-mode=bbdd

################################################
# Custom authentication
################################################
#Provider class beans, separated by comma for multiple providers.
awe.security.auth-custom-providers=
```

You can always create your own Http web security config class extending `WebSecurityConfigurerAdapter`.

```java title="Custom Http security configuration"
@Configuration
public class CustomSecurityConfig extends WebSecurityConfigurerAdapter {
  
   /**
   * Spring security configuration
   *
   * @param http Http security object
   * @throws Exception Configure error
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Your custom configuration
  }
}
```

## Two-factor authentication (2fa)
We've recently developed a new two-factor authentication system based on _authentication apps_ such as **Google Authenticator**.

There are three ways to manage this two-factor authentication in AWE based on the `awe.totp.security.enabled` property:

- `disabled`: Two-factor authentication is disabled and it won't ask for a temporal code on access.
- `optional`: The user **can enable** two-factor authentication on the **settings screen** and temporal code will be asked on login.

<img style={{ width: "70%", margin: "30px 15% 0% 15%" }}
alt="Settings screen"
src={require('@docusaurus/useBaseUrl').default('img/security-settings.png')}
/>
<div style={{textAlign:"center",fontStyle:"italic"}}>Security settings screen</div>

<img style={{ width: "60%", margin: "30px 20% 0% 20%" }}
alt="TOTP Code screen"
src={require('@docusaurus/useBaseUrl').default('img/totp-code.png')}
/>
<div style={{textAlign:"center",fontStyle:"italic",marginBottom:"30px"}}>TOTP code screen</div>


- `force`: On login, **if user has not enabled two-factor authentication**, a screen will raise with the QR code to force the user to
  enable two-factor authentication. After that screen, user will be asked for the temporal code based on the previously generated secret code.

<img style={{ width: "40%", margin: "30px 30% 0% 30%" }}
alt="Force two-factor authentication screen"
src={require('@docusaurus/useBaseUrl').default('img/force-2fa.png')}
/>
<div style={{textAlign:"center",fontStyle:"italic"}}>Force two-factor security screen</div>
