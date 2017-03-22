FROM probablyfine/flume

ARG MAVEN_VERSION=3.3.9
ARG USER_HOME_DIR="/root"

RUN apt-get update && apt-get install -y curl

RUN mkdir -p /opt/flume
RUN mkdir -p /opt/app

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

WORKDIR /opt/flume

COPY flume/pom.xml /opt/flume
RUN mvn dependency:copy-dependencies
RUN mv target/dependency/zookeeper-3.4.5.jar /opt/flume/lib/zookeeper-3.4.5.jar

WORKDIR /opt/app

COPY settings.xml /root/.m2/

COPY pom.xml /opt/app
RUN mvn dependency:copy-dependencies
RUN mvn package

WORKDIR /opt/flume

COPY flume/ /opt/flume

WORKDIR /opt/app

COPY . /opt/app
RUN mvn clean package

RUN mv target/*.jar /opt/flume/lib/

ENV FLUME_CONF_FILE /opt/flume/kafka.conf
ENV FLUME_AGENT_NAME kagent
