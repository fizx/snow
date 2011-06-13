#!/bin/sh
cd `dirname $0`
set -e

PORT=$1
CORPUS=$2

for FILE in `ls data/$CORPUS | head -500`
do
  curl 'http://localhost:'$PORT'/solr/select?q=money'
done