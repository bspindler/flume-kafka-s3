FROM probablyfine/flume

ARG MAVEN_VERSION=3.3.9
ARG USER_HOME_DIR="/root"

RUN apt-get update && apt-get install -y curl

RUN mkdir -p /opt/flume

WORKDIR /opt/flume

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

COPY flume/pom.xml /opt/flume

RUN mvn dependency:copy-dependencies

COPY flume/ /opt/flume

ENV FLUME_CONF_FILE /opt/flume/kafka.conf
ENV FLUME_AGENT_NAME kagent

RUN mv target/dependency/zookeeper-3.4.5.jar /opt/flume/lib/zookeeper-3.4.5.jar

