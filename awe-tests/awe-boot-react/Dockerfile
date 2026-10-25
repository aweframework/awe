FROM eclipse-temurin:21-jre-alpine@sha256:b48e02e0ea88d7bfb305335df94923bdef394905f4147dccd52e873cf17b1088

# Add fonts package
RUN apk add --no-cache fontconfig ttf-dejavu && fc-cache -f

# Image maintainer
LABEL maintainer="awe@almis.com"

# Copy the current directory contents into the container at /app
ADD target/awe-react.jar awe-react.jar

# Volume of app logs
VOLUME /logs

# Volume of app data
VOLUME /tmp

# Execute jar
ENTRYPOINT ["java", "-Xms256m", "-Xmx1g", "-XshowSettings:vm", "-Dspring.devtools.restart.enabled=false", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/awe-react.jar"]


