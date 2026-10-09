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

## Password expiration (local login)

The `PwdExp` application parameter sets the number of days a password stays valid. It applies to the local login
(`bbdd` authentication mode, users stored in the `ope` table). The date of the last password change is the `l1_psd`
column of the user.

| `PwdExp` | Last password change | Result |
|---|---|---|
| Not set or inactive | Any, including never | The password never expires. |
| Set (for example `30`) | Fewer than `PwdExp` days ago | The password is accepted. |
| Set | `PwdExp` days ago or more | The password is expired and the login is rejected. |
| Set | Never (empty date) | The password is expired and the login is rejected: the user has to change it. |

A user who has never changed the password has no change date. This is the case of a user that is created without a
change date and of a user whose password an administrator sets in the users screen (the screen clears the date). With `PwdExp` set, such a user is rejected as expired, and without `PwdExp` the
missing date has no effect.

The expiration is checked after the password is verified. An expired login is rejected with the
message `User credentials have expired`. Accounts that authenticate through [SSO](authentication-sso.md)
are not affected, because the password is not used there.

To change the password, the `change-password` screen (`ChdPwd` maintain target) stores the current date as the new
date of the last change.

:::note
The date is stored without time of day by the `change-password` screen, so a password is valid until `PwdExp` days
after the start of the day it was changed.
:::

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
