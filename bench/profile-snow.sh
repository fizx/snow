#!/bin/sh
cd `dirname $0`
cd solr/apache-solr-3.2.0-snow/example
rm -rf solr/data
echo starting...
java -agentpath:/Applications/YourKit_Java_Profiler_9.0.8.app/bin/mac/libyjpagent.jnilib -Xmx1g -jar start.jar