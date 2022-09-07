#!/bin/bash
cd `dirname $0`
java -jar target/enlinkd-link-descriptions-1.0-SNAPSHOT-jar-with-dependencies.jar "$@"
