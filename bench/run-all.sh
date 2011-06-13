#!/bin/sh
cd `dirname $0`
sh ./run-nrt.sh &
sh run-solr.sh