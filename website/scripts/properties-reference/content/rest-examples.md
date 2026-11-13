### Examples Rest services definitions

```shell title="application.properties"
################################################
# Microservices properties
################################################
awe.rest.services.clients-microservice.base-url=http://localhost:8081
# Microservice parameters
awe.rest.services.clients-microservice.parameters[0].name=database
awe.rest.services.clients-microservice.parameters[0].type=session
awe.rest.services.clients-microservice.parameters[1].name=username
awe.rest.services.clients-microservice.parameters[1].type=session
awe.rest.services.clients-microservice.parameters[2].name=currentDate
awe.rest.services.clients-microservice.parameters[2].type=session
awe.rest.services.clients-microservice.parameters[3].name=numPar
awe.rest.services.clients-microservice.parameters[3].value=3
# microservices.[service-name] to overwrite microservice base endpoint
awe.rest.services.other-service.name=other-service-bis
awe.rest.services.other-service.base-url=http://localhost:8082

################################################
# Rest services properties
################################################
awe.rest.services.local.base-url=http://localhost:18081
awe.rest.services.island.base-url=https://apis.is
awe.rest.services.postman.base-url=https://postman-echo.com
# Rest security
awe.rest.services.postman.authentication.type=basic
awe.rest.services.postman.authentication.username=postman
awe.rest.services.postman.authentication.password=password

```
