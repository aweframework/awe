FROM eclipse-temurin:21-jre-alpine@sha256:82cf3d9ccbe1ed46252008d51b2f2329e6b9e8a2f4394c4bebefc3ea7f4b1372

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


