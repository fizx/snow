Abandoned/deprecated
========

Solr4 includes this functionality.

Overview
========

Snow provides a simple, fast, tiny drop-in plugin for realtime search in Solr.  

Why?
====

Solr is the leading open-source search engine, but it's not great at realtime updates.  Your options used to be:

1. Batch your changes every few (possibly tens of) seconds.
2. Patch Solr to use Lucene NRT "Near real time" capabilities.  
3. Use Solr with Zoie, a plugin for realtime search developed by LinkedIn.
4. Use Solandra, Solr backed by Cassandra.

Unfortunately NRT isn't that fast, and Zoie, while fast, is a 10,000LOC project with Solr support as a second-class citizen.  Solandra was 10-40x slower than solr in benchmarks, probably because Cassandra's read performance is still slower than other general purpose databases, and the data structures it uses are less efficient for this use case than the Lucene index structure.

How it works
============

Snow uses NRT, but bundles in a slightly modified version of Lucene's contrib/NRTCachingDirectory.  Effectively, this creates a small RAM disk for the most rapidly changing parts of your index.

Building and installing
=======================

We aren't distributing binaries yet, but Snow is easy to build from source.  Snow is compatible with Solr 3.2.  To build, you can run:

     # depends on unix, wget, java
    :$ ./build-war.sh 

Configuration
=============

There is an example configuration in ./bench/misc/solrconfig-nrt.xml.  If you'd prefer to use your existing configuration, you need to make the following changes to your solrconfig.xml.

1. Replace "solr.DirectUpdateHandler2" with "com.websolr.snow.solr.SnowUpdateHandler"
2. Add or replace the following chunk of xml in solrconfg.xml. The indexReaderFactory is commented out in the example `solrconfig.xml` that ships with Solr.

        <indexReaderFactory name="IndexReaderFactory" class="com.websolr.snow.solr.SnowIndexReaderFactory" />
    
3. Disable all Solr caches.  You can grep for FastLRUCache, and comment out the xml tags that contain it.

Usage
=====

The usage of Solr is the same as before.  You still need to commit, however your commits will take around a millisecond, instead of around a second.  Rollback is unsupported, and some of the advanced commit flags are no-ops.

In general, it's feasible to commit every add, unless you're bulk re-indexing.  YMMV, and check out the benchmarks below.

Benchmarks
==========

We want to test three cases:

1. Batch indexing
2. Indexing while searching
3. Searching a static index

The goal is to have 1 & 3 be only slightly slower than stock Solr, while 2 is *much* faster.  See the bench/ folder for most of the setup.

Batch indexing
--------------

For this, I used curl to upload 1MM tiny documents in 10,000 document chunks, single-threaded.  Tiny documents are the worst-case scenario for possible slowdowns in indexing, because the indexing time will not be dominated by tokenization and linguistic features.  This setup replicates a reasonable batch-indexing scenario.

Best of five runs of `time ./bench/batch.sh`:

* Stock, committing every 10k docs: 0m42.837s (23,300 docs/second)
* SNOW, committing every 10k docs, flushing to disk every 10s: 0m56.135s (17,800 docs/second)
* SNOW, committing every 10k docs, never super-flushing: 0m36.232s 

* Stock, never flushing: 0m36.575s


From this, we can conclude, that SNOW flushes are more expen



