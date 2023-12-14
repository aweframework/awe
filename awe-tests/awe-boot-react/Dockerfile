FROM eclipse-temurin:17-jre-alpine

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
ENTRYPOINT ["java", "-jar", "/awe-react.jar"]


