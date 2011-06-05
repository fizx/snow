#!/bin/sh
cd `dirname $0`
if [ ! -d data ]
then
  mkdir -p data
  wget -O data/names.bz2 http://download.freebase.com/wex/2011-05-13/freebase-wex-2011-05-13-freebase_names.tsv.bz2
  cd data
  bunzip2 names.bz2
  split -a 6 -l 10000 names
  rm names
  cd -
fi
if [ ! -d solr ]
then
  mkdir -p solr
  cd solr
  wget -O solr.tgz http://mirrors.kahuki.com/apache//lucene/solr/3.2.0/apache-solr-3.2.0.tgz
  tar -xzf solr.tgz
  cp -R apache-solr-3.2.0 apache-solr-3.2.0-snow
  rm solr.tgz
  cd -
fi
cd ..
./sbt update package
mkdir -p WEB-INF/lib
cp ./project/boot/scala-2.8.1/lib/scala-library.jar WEB-INF/lib
cp ./target/scala_2.8.1/snow_2.8.1-0.1.jar WEB-INF/lib
zip bench/solr/apache-solr-3.2.0-snow/example/webapps/solr.war WEB-INF/lib/*
rm -rf WEB-INF/lib
cd bench
cp misc/solrconfig.xml solr/apache-solr-3.2.0-snow/example/solr/conf