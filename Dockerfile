FROM anapsix/alpine-java:8_jdk

ARG MAVEN_VERSION=3.3.9
ARG USER_HOME_DIR="/root"

RUN mkdir -p /opt/flume

RUN apk add --no-cache wget \
  && apk --update add tar \

  && wget -qO- http://archive.apache.org/dist/flume/1.6.0/apache-flume-1.6.0-bin.tar.gz | tar -xzC /opt/flume --strip-components=1 \
  && apk del wget \
  && rm -rf /var/cache/apk/*

ENV PATH /opt/flume/bin:$PATH

RUN mkdir -p /opt/app

WORKDIR /opt/app

COPY . /opt/app

RUN apk add --no-cache curl \

  && mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn \

  && cd /opt/app/flume \
  && mvn dependency:copy-dependencies \
  && mv target/dependency/zookeeper-3.4.5.jar /opt/flume/lib/zookeeper-3.4.5.jar \

  && cd /opt/app \
  && mv settings.xml /root/.m2 \
  && mv /opt/app/flume/kafka.conf /opt/flume/kafka.conf \
  && mvn package \
  && mv target/*jar-with-dependencies.jar /opt/flume/lib/ \

  && rm -rf /opt/app \
  && rm -rf /root/.m2 \
  && rm -rf /usr/shar/maven \
  && apk del curl \
  && rm -rf /var/cache/apk/*

ENV ZOOKEEPER_URL zookeeper

ENV FILE_PREFIX flume-s3

ENV AWS_ACCESS_KEY_ID accesskey
ENV AWS_SECRET_ACCESS_KEY secretkey
ENV AWS_BUCKET netuitive-bucket
ENV AWS_BUCKET_FORMAT /%Y/%m/%d/%H

CMD sed -i -e "s~kagent.sinks.s3.hdfs.path\ =\ .*~kagent.sinks.s3.hdfs.path\ =\ s3n://${AWS_ACCESS_KEY_ID}:${AWS_SECRET_ACCESS_KEY}@${AWS_BUCKET}${AWS_BUCKET_FORMAT}~g" /opt/flume/kafka.conf \
  && sed -i -e "s/kagent.sinks.s3.hdfs.filePrefix\ =\ .*/kagent.sinks.s3.hdfs.filePrefix\ =\ ${FILE_PREFIX}/g" /opt/flume/kafka.conf \
  && sed -i -e "s~kagent.sources.source1.zookeeperConnect\ =\ .*~kagent.sources.source1.zookeeperConnect\ =\ ${ZOOKEEPER_URL}~g" /opt/flume/kafka.conf \
  && java -Xmx200m -Dflume.root.logger=INFO,console -cp '/opt/flume/conf:/opt/flume/lib/*:/lib/*' -Djava.library.path= org.apache.flume.node.Application -f /opt/flume/kafka.conf -n kagent
