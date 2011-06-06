#!/bin/sh
cd `dirname $0`

curl --data-binary "<delete><query>*:*</query></delete>" -H 'Content-type:text/xml; charset=utf-8' 'http://localhost:8983/solr/update'


for FILE in `ls data`
do
  curl --data-binary "@data/$FILE" -H 'Content-type:text/csv; charset=utf-8' 'http://localhost:8983/solr/update/csv?separator=%09&escape=\&fieldnames=id,text&header=false'
done
curl "http://localhost:8983/solr/update?commit=true"