#!/bin/sh
cd `dirname $0`
cd solr/apache-solr-3.2.0/example
rm -rf solr/data
echo starting...
java -Xmx1g -jar start.jar