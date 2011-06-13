#!/bin/sh
cd `dirname $0`
# mkdir -p results
# sh batch.sh 8983 > results/batch-solr 2>/dev/null
# sh batch.sh 8984 > results/batch-nrt 2>/dev/null

sh slow-post.sh 8983 > results/single-solr 2>/dev/null &
PID=$$
sleep 3
sh search.sh 8983 | tee results/query-solr 2>/dev/null
wait $PID

sh slow-post.sh 8984 | tee results/single-nrt &
PID=$$
sleep 3
sh search.sh 8983 | tee results/query-nrt 2>/dev/null
wait $PID
echo done
