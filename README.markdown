Pre-release code, still under active development

Overview
========

Solr is the leading open-source search engine, but it's not great at realtime updates.  Your options used to be:

1. Batch your changes every few (possibly tens of) seconds.
2. Patch Solr to use Lucene NRT "Near real time" capabilities.  In my benchmarks, NRT rarely helps
3. Use Solr with Zoie, a plugin for realtime search developed by LinkedIn.
4. Use Solandra, Solr backed by Cassandra.

Unfortunately NRT isn't that fast, and Zoie, while fast, is a 10,000LOC project with Solr support as a second-class citizen.  I'm skeptical of Solandra.  Cassandra's read performance is still slow, and the data structures it uses are much less efficient for this use case than the Lucene index structure.

Snow provides a simple, fast, drop-in, plugin for realtime search in Solr.  Inspired by Zoie, it's much times faster than Lucene NRT.  It's a couple hundred lines of straightforward Scala, so you and I have a hope of maintaining it (or porting it to Java, if that's your thing).

How it works
============

Snow sends new documents to an extra index in RAM. Snow flushes the RAM index to the primary on-disk index whenever the buffer gets large, or a configurable amount of time has elapsed.  Reads, updates, and deletes are transparently routed to both indexes.

Building and installing
=======================

Snow is compatible with Solr 3.1.

    :$ ./sbt update package
    :$ mkdir -p WEB-INF/lib
    :$ cp ./project/boot/scala-2.8.1/lib/scala-library.jar WEB-INF/lib
    :$ cp ./target/scala_2.8.1/snow_2.8.1-0.1.jar WEB-INF/lib
    :$ zip bench/solr/apache-solr-3.2.0-snow/example/webapps/solr.war WEB-INF/lib/*
    :$ rm -rf WEB-INF

Edit your solrconfig.xml, replacing "solr.DirectUpdateHandler2" with "com.websolr.snow.solr.RealtimeUpdateHandler".

Finally, add or replace the following chunk of xml in solrconfg.xml. The indexReaderFactory is commented out in the example `solrconfig.xml` that ships with Solr.

     <indexReaderFactory name="IndexReaderFactory" class="com.websolr.snow.solr.ManagedIndexReaderFactory" />