Pre-release code, still under active development

Overview
========

Solr is the leading open-source search engine, but it's not great at realtime updates.  Your options used to be:

1. Batch your changes every few tens of seconds.
2. Patch Solr to use Lucene NRT "Near real time" capabilities.
3. Use Solr with Zoie, a plugin for realtime search developed by LinkedIn.

Unfortunately NRT isn't that fast, and Zoie, while fast, is a 10,000LOC project with Solr support as a second-class citizen.

Snow provides a simple, fast, drop-in plugin for realtime search in solr.  Inspired by Zoie, it's several times faster than Lucene NRT.  It's a couple hundred lines of straightforward Scala, so you and I have a hope of maintaining it (or porting it to Java, if that's your thing).

How it works
============

Snow sends new documents to an extra index in RAM. Snow flushes the RAM index to the primary on-disk index whenever the buffer gets large, or a configurable amount of time has elapsed.  The flush uses NRT, which turns out to be fast enough to use occasionally.  Reads, updates, and deletes are transparently routed to both indexes.

Building and installing
=======================

Snow is compatible with Solr 3.1.

    :$ ./sbt package
    :$ cp ./project/boot/scala-2.8.1/lib/scala-library.jar YOUR_SOLR_HOME/lib
    :$ cp ./target/scala-2.8.1/snow_2.8.1-0.1.jar YOUR_SOLR_HOME/lib

Edit your solrconfig.xml, replacing "solr.DirectUpdateHandler2" with "com.km.snow.solr.RealtimeUpdateHandler".

Finally, add or replace the following chunk of xml in solrconfg.xml. The indexReaderFactory is commented out in the example solrconfig.xml's that ship with Solr.

     <indexReaderFactory name="IndexReaderFactory" class="com.km.snow.solr.ManagedIndexReaderFactory" />