#!/bin/sh
cd `dirname $0`
set -e

PORT=$1
CORPUS=$2

for FILE in `ls data/$CORPUS | head -100`
do
  curl -f --data-binary "@data/$CORPUS/$FILE" -H 'Content-type:text/csv; charset=utf-8' 'http://localhost:'$PORT'/solr/update/csv?commit=true&separator=%09&escape=\&fieldnames=id,text&header=false'
done