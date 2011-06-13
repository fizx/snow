#!/bin/sh
cd `dirname $0`/bench
if [ ! -d data/names ]
then
  mkdir -p data/names
  wget -O data/names/names.bz2 http://download.freebase.com/wex/2011-05-13/freebase-wex-2011-05-13-freebase_names.tsv.bz2
  cd data/names
  bunzip2 names.bz2
  split -a 6 -l 10000 names
  rm names
  cd -
fi

if [ ! -d data/articles ]
then
  mkdir -p data/articles
  cd data/articles
  curl http://download.freebase.com/wex/latest/freebase-wex-2011-05-27-articles.tsv.bz2 | bzcat | cut -f 1 -f 4 | sed -E 's/<[^>]*>/ /g' | sed 's/\\n/ /g' | head -500000 > articles
  split -a 6 -l 1000 articles
  rm articles
  cd -
fi

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
rm -rf WEB-INF
cd bench
cp misc/solrconfig-nocache.xml solr/apache-solr-3.2.0/example/solr/conf/solrconfig.xml
cp misc/solrconfig-nrt.xml solr/apache-solr-3.2.0-nrt/example/solr/conf/solrconfig.xml
