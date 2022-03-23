FROM twosixlabsdart/java-fatjar-runner:latest

LABEL maintainer="john.hungerford@twosixlabs.com"

ENV SCALA_VERSION '2.12'

ENV JAVA_OPTS="-Xms2g -Xmx6g -XX:+UseG1GC"
ENV PROGRAM_ARGS "--env default"

COPY ./concepts-microservice/target/scala-$SCALA_VERSION/*assembly*.jar $APP_DIR

RUN chmod -R 755 /opt/app

ENTRYPOINT ["/opt/app/run-jar.sh"]
