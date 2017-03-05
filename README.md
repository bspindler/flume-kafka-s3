## docker-flume

  Docker image containing [Apache Flume](https://flume.apache.org/)

## Build Instructions

    docker build -t flume .

## Available environment variables

 * `FLUME_AGENT_NAME` - name of flume agent to run. **Required**
 * `FLUME_CONF_FILE` - location of flume configuration file. **Required**
 * `FLUME_CONF_DIR` - directory for flume environment/configuration files. Defaults to `/opt/flume/conf`

## Example usage

`FROM probablyfine/flume
 ADD example.conf /var/tmp/flume.conf
 EXPOSE 44444`

### and to run

   ` docker run -d \
      -e FLUME_AGENT_NAME=a1 \
      -e FLUME_CONF_FILE=/var/tmp/flume.conf \
      -p 44444:44444 \
      flume:example`
