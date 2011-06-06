#!/bin/sh
cd `dirname $0`
find solr -name data | xargs rm -rf

set -e

for FILE in `ls data`
do
  curl -f --data-binary "@data/$FILE" -H 'Content-type:text/csv; charset=utf-8' 'http://localhost:8983/solr/update/csv?commit=true&separator=%09&escape=\&fieldnames=id,text&header=false'
done