#!/bin/sh
cd `dirname $0`/bench

if [ ! -d solr ]
then
  mkdir -p solr
  cd solr
  wget -O solr.tgz http://mirrors.kahuki.com/apache//lucene/solr/3.2.0/apache-solr-3.2.0.tgz
  tar -xzf solr.tgz
  cp -R apache-solr-3.2.0 apache-solr-3.2.0-nrt
  rm solr.tgz
  cd -
fi
cd ..
./sbt update package
mkdir -p WEB-INF/lib
cp ./project/boot/scala-2.8.1/lib/scala-library.jar WEB-INF/lib
cp ./target/scala_2.8.1/snow_2.8.1-0.1.jar WEB-INF/lib
zip bench/solr/apache-solr-3.2.0-nrt/example/webapps/solr.war WEB-INF/lib/*
cp bench/solr/apache-solr-3.2.0-nrt/example/webapps/solr.war .
rm -rf WEB-INF
cd bench
cp misc/solrconfig-nocache.xml solr/apache-solr-3.2.0/example/solr/conf/solrconfig.xml
cp misc/solrconfig-nrt.xml solr/apache-solr-3.2.0-nrt/example/solr/conf/solrconfig.xml
