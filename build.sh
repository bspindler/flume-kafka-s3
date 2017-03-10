#!/bin/bash

mvn dependency:copy-dependencies
docker build -t flume:kafka . 
