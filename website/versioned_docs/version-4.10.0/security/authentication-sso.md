---
id: authentication-sso
title: Single Sign On authentication
sidebar_label: Single sign-on
---

<img style={{ width: "40%", margin: "10% 30% 10% 30%" }}
alt="AWE security"
src={require('@docusaurus/useBaseUrl').default('img/undraw_security-on_btwg.svg')}
/>

# Single sign On
In AWE applications you can use SSO authentication method. This feature allows a user to utilize a single account to access different apps (user name and password).

## Azure EntraID
AWE provide Azure oauth2 authentication service integration using native `spring-cloud-azure-starter-active-directory`. Uses the Spring Boot Starter for Microsoft Entra ID enables you to connect your web application to a Microsoft Entra tenant and protect your resource server with Microsoft Entra ID. It uses the Oauth 2.0 protocol to protect web applications and resource servers.

<img style={{ width: "70%", margin: "30px 15% 0% 15%" }}
alt="Azure Entra ID"
src={require('@docusaurus/useBaseUrl').default('img/Azure_entraID.png')}
/>
<div style={{textAlign:"center",fontStyle:"italic"}}>Azure Entra ID</div>

To enable Azure oauth2 active directory, you have to add spring-cloud-azure starter and configure your organization tenantId and application ID and secret.

```xml title="Add dependency"
    <dependency>
      <groupId>com.azure.spring</groupId>
      <artifactId>spring-cloud-azure-starter-active-directory</artifactId>
    </dependency>
```
```properties title="Configure azure EntraID properties"
# Enable related features.
spring.cloud.azure.active-directory.enabled=true
# Specifies your Active Directory ID:
spring.cloud.azure.active-directory.profile.tenant-id={CONFIGURE YOUR TENANT ID}
# Specifies your App Registration's Application ID:
spring.cloud.azure.active-directory.credential.client-id={CONFIGURE YOUR CLIENT ID}
# Specifies your App Registration's secret key:
spring.cloud.azure.active-directory.credential.client-secret={CONFIGURE YOUR SECRET KEY}
```

:::info You can visit [this](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/spring-boot-starter-for-azure-active-directory-developer-guide?tabs=SpringCloudAzure4x) for more info. 
:::

By default, if the user logged in the application with this  doesn't exist in database, it  will be provisioned by registering it by adding a new record in the user table.
If you do not want this behavior, you can disable it setting false the configuration property `awe.security.auto-provision-use`.

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 2"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-client-2.png')}
/>

Configure the client by setting the Root URL, Web origins, Admin URL to the hostname (https://{hostname}).

Also you can set Home URL to /applications path and Valid Post logout redirect URIs to "https://{hostname}/applications".

The Valid Redirect URIs should be set to https://{hostname}/auth/callback (you can also set the less secure https://{hostname}/* for testing/development purposes, but it's not recommended in production).

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 3"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-client-3.png')}
/>

Make sure to click Save.

There should be a tab called Credentials. You can copy the Client Secret that we'll use in our app configuration.

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 4"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-client-4.png')}
/>

The following configuration properties need to be added in order to integrate an AWE application with the Keycloak server

```properties title="Configure keycloak oauth client properties"
################################################
# SSO login
################################################
# Enable AWE SSO
awe.security.sso.enabled=true
# Auto launch sso flow (skip native window sign in)
awe.security.sso.auto-launch=true
# Oauth provider name
spring.security.oauth2.client.registration.keycloak.provider=[PROVIDER_NAME]
# Provider issuer uri
spring.security.oauth2.client.provider.awe.issuer-uri=[PROVIDER_URI]
# Authorization grant type for login
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
# Client Id
spring.security.oauth2.client.registration.keycloak.client-id=[CLIENT_ID]
# Client Secret
spring.security.oauth2.client.registration.keycloak.client-secret=[CLIENT_SECRET]
# Scope request
spring.security.oauth2.client.registration.keycloak.scope=openid
```
### Add identity providers
You can integrate others identity provider to use in your authentication process. In this guide, we use Azure EntraID as example.

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 5"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-provider.png')}
/>

In the detail page, fill out the details as required below:
* Enter the alias of your choice. Enable use discovery endpoint, if not already enabled
* Input the Discovery URL from Azure (copied before) into the Discovery endpoint

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 6"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-provider-2.png')}
/>

* Input the Client ID. This is the application (client) ID copied from Azure app registration.
* Input Client Secret. This is the application secret copied from Azure app registration

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 7"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-provider-3.png')}
/>

### Mappers

When configuring roles/groups, the process is a bit more tedious since the claims used for this are not standard. Each provider uses a different method.

In order to collect the information sent to us by the provider, we will have to create some mappers that retrieve the information and translate it into the keycloak environment.

- Group mappers

By default, Azure EntraID does not display the groups associated with each user. In order to retrieve the groups to which a user belongs, it is necessary to configure Azure to send a custom claim `groups` with token type ClientID.

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 8"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-provider-azure.png')}
/>

Then, you have to create a new mapper for identity provider to map user group objectId to keycloak role.

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 9"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-provider-azure-1.png')}
/>

- App role mappers

In order to retrieve the app roles of an application registered in Azure EntraID, it is necessary to create an `advanced claim custom role mapper` that maps the claim *role* key with the name of application role in Azure with the keycloak role.

<img style={{ width: "100%", margin: "30px 5% 5% 0%" }}
    alt="Keycloak new client 10"
    src={require('@docusaurus/useBaseUrl').default('img/keycloak-add-provider-azure-2.png')}
/>

