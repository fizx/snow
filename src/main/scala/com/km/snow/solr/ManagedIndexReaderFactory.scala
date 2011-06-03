package com.km.snow.solr

import com.km.snow.managed._

import org.apache.solr.core.IndexReaderFactory
import org.apache.solr.common.util._

import java.io._
import org.apache.lucene.index._
import org.apache.lucene.store._
import org.apache.lucene.search._
import java.util.concurrent._
import org.apache.lucene.util.Version._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.index.IndexWriterConfig.OpenMode._

object ManagedIndexReaderFactory {
  var pool = Executors.newScheduledThreadPool(10)
}

class ManagedIndexReaderFactory extends IndexReaderFactory {

  val analyzer = new StandardAnalyzer(LUCENE_31)
  val cfg = new IndexWriterConfig(LUCENE_31, analyzer)
  var writer: ManagedIndexWriter = null

  override def newReader(dir: Directory, readOnly: Boolean) = {
    if (writer == null) {
      writer = new ManagedIndexWriter(dir, cfg, ManagedIndexReaderFactory.pool, 10000, 10000)
    }
    writer.getReader()
  }
}