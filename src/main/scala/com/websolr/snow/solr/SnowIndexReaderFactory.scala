package com.websolr.snow.solr

import com.websolr.snow.managed._

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

class SnowIndexReaderFactory extends IndexReaderFactory {
  val analyzer = new StandardAnalyzer(LUCENE_31)
  val cfg = new IndexWriterConfig(LUCENE_31, analyzer)
  val policy = new BalancedSegmentMergePolicy()
  policy.setMergeFactor(3)
  cfg.setMergePolicy(policy)
  var writer: NRTIndexWriter = null

  override def newReader(dir: Directory, readOnly: Boolean) = {
    if (writer == null) {
      writer = new NRTIndexWriter(dir, cfg)
    }
    writer.getReader()
  }

  def close() = {
    writer.close()
  }
}