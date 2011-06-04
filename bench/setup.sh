#!/bin/sh
cd `dirname $0`
mkdir -p data
wget -O data/names.bz2 http://download.freebase.com/wex/2011-05-13/freebase-wex-2011-05-13-freebase_names.tsv.bz2
cd data
bunzip2 names.bz2
split -l 100000 names
rm names
cd -
mkdir -p dist
cd dist
wget -O solr.tgz http://mirrors.kahuki.com/apache//lucene/solr/3.2.0/apache-solr-3.2.0.tgz
tar -xzf solr.tgz
cp -R apache-solr-3.2.0 apache-solr-3.2.0-snow
