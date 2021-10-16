# Use an official Open jdk runtime as a parent image
FROM adoptopenjdk/openjdk11:jdk-11.28-alpine

# Add fonts package
RUN apk add --no-cache fontconfig msttcorefonts-installer ttf-opensans && fc-cache -f

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


