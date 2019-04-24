FROM openjdk:8-alpine
RUN apk add --update ca-certificates && rm -rf /var/cache/apk/* && \
  find /usr/share/ca-certificates/mozilla/ -name "*.crt" -exec keytool -import -trustcacerts \
  -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts -storepass changeit -noprompt \
  -file {} -alias {} \; && \
  keytool -list -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts --storepass changeit

ENV MAVEN_VERSION 3.6.1
ENV MAVEN_HOME /usr/lib/mvn
ENV PATH $MAVEN_HOME/bin:$PATH

RUN wget http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  tar -zxvf apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  rm apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  mv apache-maven-$MAVEN_VERSION /usr/lib/mvn

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
ADD . .
RUN mvn package

FROM openjdk:8-alpine
EXPOSE 8080
VOLUME /tmp

# Add JAR from project (required docker-maven-plugin at least 0.23.0)
RUN mkdir -p /app && apk --update add pwgen
WORKDIR /app
COPY --from=0 /usr/src/app/target/*-exec.jar /app/app.jar
ADD wait-for /app/wait-for
ADD run-app /app/run-app

# Runtime parameters (docker run ... org docker-compose environment: ...)
ENV JAVA_OPTS=""
ENV JAVA_RUN="-Djava.security.egd=file:/dev/./urandom -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -jar /app/app.jar"
ENV WAITFOR=""

# Rename java so it can be identified in processes
# Specify at build time (JAVA_BINARY=xxx docker build)
ARG JAVA_BINARY="/usr/bin/java"
RUN if [ "$JAVA_BINARY" != "/usr/bin/java" ]; then echo "Making link $JAVA_BINARY"; ln -s /usr/bin/java $JAVA_BINARY; fi

# Also specify at runtime
ENV JAVA_BINARY="/usr/bin/java"

# Create user
ARG USERNAME=java
ARG USERID=1000
ARG HOMEDIR=/app

# Test for /sbin/apk (Alpine) and run proper command to add user
RUN if [ -x /sbin/apk ]; then \
        adduser -D -u $USERID -h $HOMEDIR -s /bin/sh $USERNAME; \
    else \
        useradd -m -u $USERID -d $HOMEDIR -s /bin/sh $USERNAME; \
    fi

USER $USERNAME
ENV HOME=/app

# Run unprivileged, specify JAVA_BINARY, WAITFOR, JAVA_OPTS, JAVA_RUN or use defaults.
ENTRYPOINT [ "/app/run-app", "if [ \"$WAITFOR\" != \"\" ]; then /app/wait-for $WAITFOR -- $JAVA_BINARY $JAVA_OPTS $JAVA_RUN; else $JAVA_BINARY $JAVA_OPTS $JAVA_RUN; fi" ]
