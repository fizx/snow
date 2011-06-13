#!/bin/sh
cd `dirname $0`
set -e

PORT=$1

for LINE in `head -1000 data/xaaaaaa | sed 's/'$'\t''/\|/g' | sed 's/ /\./g'`
do
  curl -f --data-binary "$LINE" -H 'Content-type:text/csv; charset=utf-8' 'http://localhost:'$PORT'/solr/update/csv?commit=true&separator=%7C&escape=\&fieldnames=id,text&header=false'
done