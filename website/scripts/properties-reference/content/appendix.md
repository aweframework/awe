## Encoding property value

There's a way to store encoded properties (such as passwords) in a properties file. You need to deploy
the application, and enter into `Settings -> Security access -> Encrypt util`, fill the text criterion with the password
and push the `Encrypt` button. The `Encrypted property` field is the value of the encoded password.

<img alt="Almis" src={require('@docusaurus/useBaseUrl').default('img/EncryptTool.png')} />

Once encoded, you just need to put it on any of your properties files and use it:

```properties
my.encoded.password=ENC(Pit1Q1bNt3uNQpZbldKbLg==)
```

---

> :information_source: The master key used to encode the properties is the `awe.security.master-key` property from `security.properties` file.
> You can overwrite it on your own `application.properties` file.

---

> :warning: The `Phrase key` field on the `Encrypt util` screen allows you to generate the encoded properties with other passwords, but don't forget that
> the encoded properties with a phrase key and the `awe.security.master-key` must match to be decoded successfully.

---
## Overwriting properties
You can overwrite any property of awe framework by adding it to your `application.properties` file of the project.

Awe has the same reading properties order than Spring (1. Is the highest preference).

1. Devtools global settings properties on your home directory (`~/.spring-boot-devtools.properties` when devtools is active).
2. `@TestPropertySource` annotations on your tests.
3. `@SpringBootTest#properties` annotation attribute on your tests.
4. Command line arguments.
5. Properties from `SPRING_APPLICATION_JSON` (inline JSON embedded in an environment variable or system property)
6. `ServletConfig` init parameters.
7. `ServletContext` init parameters.
8. JNDI attributes from `java:comp/env`.
9. Java System properties (`System.getProperties()`).
10. OS environment variables.
11. A `RandomValuePropertySource` that only has properties in `random.*.`
12. Profile-specific application properties outside of your packaged jar (`application-{profile}.properties` and YAML variants)
13. Profile-specific application properties packaged inside your jar (`application-{profile}.properties` and YAML variants)
14. Application properties outside of your packaged jar (`application.properties` and YAML variants).
15. Application properties packaged inside your jar (`application.properties` and YAML variants).
16. `@PropertySource` annotations on your `@Configuration` classes.
17. Default properties (specified using `SpringApplication.setDefaultProperties`).

### Externalized configuration
If you need load the configuration from one external file, you have to consider:

AWE like `SpringApplication` will load properties from application.properties files in the following locations and add them to the Spring `Environment`:

1. A `/config` subdirectory of the current directory
2. The current directory
3. A classpath `/config` package
4. The classpath root

Also, you can set `spring.config.location` environment property the run command.

```shell script
java -jar myAweProject.jar --spring.config.location=file:/external_path/specific.properties
```

---

> :information_source: You can find more information about this in the [Spring Boot reference documentation](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files).

---
