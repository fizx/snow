#!/bin/sh
cd `dirname $0`
mkdir -p results

CORPUS=$1

sh batch.sh 8983 $CORPUS > results/batch-solr-$CORPUS 2>/dev/null
sh batch.sh 8984 $CORPUS > results/batch-nrt-$CORPUS 2>/dev/null

sh slow-post.sh 8983 $CORPUS > results/single-solr-$CORPUS 2>/dev/null &
PID=$$
sleep 3
sh search.sh 8983 $CORPUS | tee results/query-solr-$CORPUS 2>/dev/null
wait $PID

sh slow-post.sh 8984 $CORPUS | tee results/single-nrt-$CORPUS 2>/dev/null &
PID=$$
sleep 3
sh search.sh 8983 $CORPUS | tee results/query-nrt-$CORPUS 2>/dev/null
wait $PID

echo done