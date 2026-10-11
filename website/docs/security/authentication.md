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

:::info
You can visit [this](https://spring.io/guides/topicals/spring-security-architecture) for more info.
:::

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
| Set | `PwdExp` days ago or more | The password is expired: the user has to [set a new one](#login-with-an-expired-password). |
| Set | Never (empty date) | The password is expired: the user has to [set a new one](#login-with-an-expired-password) at the first login. |

### Passwords set by an administrator

A user who has never changed the password has no change date. This is the case of a user that is created without a
change date and of a user whose password an administrator sets in the users screen (the screen clears the date).

**Decision:** with `PwdExp` set, a password set by an administrator counts as expired, so the user has to change it at
the first login ("must change at first login"). The administrator knows the new password, so it should not stay valid
as the user's own secret. Without `PwdExp` the missing date has no effect. If you do not want this behaviour for an
account, do not set `PwdExp`, or change the password of the account through the `change-password` screen.

### Login with an expired password

The expiration is checked after the password is verified, so only the user who knows the password sees it. Accounts
that authenticate through [SSO](authentication-sso.md) are not affected, because the password is not used there, and
neither are the users of an LDAP or any other non-local authentication mode.

In the web login (AngularJS and React engines) a user with an expired password:

1. Gets the message *Your password has expired. Set a new password to continue.* (localized in every language of the
   framework: `ERROR_TITLE_EXPIRED_PASSWORD` and `ERROR_MESSAGE_EXPIRED_PASSWORD`) and is taken to the public
   `change-expired-password` screen, which asks for the new password twice.
2. Sets the new password. The password has to meet the password policy of the application
   ([`awe.security.password-pattern` and `awe.security.password-min-length`](../properties.md)), which is checked in
   the screen and again in the server, and has to be different from the expired one. The change date is stored, so the
   new password is not expired.
3. Is taken back to the sign in screen and logs in with the new password through the regular login (which includes the
   second factor, if enabled).

The user never gets an authenticated session with the expired password. After the server verifies the password it only
keeps, in the HTTP session of that login attempt, a request to change the password of that user:

- it is bound to the user whose password was verified, so the user to change is never taken from the browser;
- it expires after five minutes and can be used once: it is discarded when the password is changed, and also when it
  expires or when the user is disabled or locked in the meantime;
- it only allows the `ChdPwdExpired` maintain target; the rest of the application still answers *unauthorized*;
- a wrong password does not create it, and the change step cannot be used to guess passwords: the password is verified
  by the regular login, exactly as before, so whatever limits the failed logins of your installation (a gateway, a
  custom authentication provider, or an administrator locking the account) keeps applying. AWE has no failed login
  counter of its own.

:::warning Custom public menus
The public menu of the framework defines the `change-expired-password` option. An application that replaces the whole
`public` menu has to keep the option (`<option name="change-expired-password" screen="change-expired-password"
invisible="true" />`), and an application that overrides the `signin` screen can keep the user flow by leaving the
`login` server action as it is: the server adds the redirection.
:::

To change the password when the user is logged in, the `change-password` screen (`ChdPwd` maintain target) stores the
current date as the new date of the last change. Since AWE 5 the `ChdPwd` maintain target is not public (it used to
be callable without a session) and changes the password of the logged in user only.

:::note
The date is stored without time of day, so a password is valid until `PwdExp` days after the start of the day it was
changed.
:::

### REST authentication

`POST /api/authenticate` answers `401 Unauthorized` for a user whose password has expired, with a localized message and
the code `PASSWORD_EXPIRED` to tell it from invalid credentials:

```json
{
  "type": "ERROR",
  "title": "Unauthorized",
  "message": "Your password has expired. Set a new password to continue.",
  "code": "PASSWORD_EXPIRED"
}
```

The message uses the default language of the application. There is no REST flow to change an expired password: the
change needs the HTTP session of the web login, so REST clients (and technical users, such as the one used by the
scheduler to call the API) have to change the password in the web application, or an administrator has to set
`PwdExp` to a larger value for them. Keep in mind that a technical user whose password was set by an administrator
counts as expired while `PwdExp` is set.

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
