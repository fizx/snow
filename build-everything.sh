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

sh ../build-war.sh