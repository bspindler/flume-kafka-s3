FROM probablyfine/flume

# Must run mvn dependency:copy-dependencies to ensure these files are there
ADD target/dependency/zookeeper-3.4.5.jar /opt/flume/lib/zookeeper-3.4.5.jar
ADD kafka.conf /var/tmp/flume.conf

