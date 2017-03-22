FROM probablyfine/flume

ARG MAVEN_VERSION=3.3.9
ARG USER_HOME_DIR="/root"

RUN apt-get update && apt-get install -y curl

RUN mkdir -p /opt/app

WORKDIR /opt/app

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

COPY pom.xml /opt/app

RUN mvn dependency:copy-dependencies

COPY . /opt/app

ENV FLUME_CONF_FILE /opt/app/kafka.conf
ENV FLUME_AGENT_NAME kagent

RUN mv target/dependency/zookeeper-3.4.5.jar /opt/flume/lib/zookeeper-3.4.5.jar

