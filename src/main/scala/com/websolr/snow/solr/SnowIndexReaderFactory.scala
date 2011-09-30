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
  private var _writer: NRTIndexWriter = null
  private var _dir: Directory = null
  private var _holder: RefreshableIndexHolder = null

  lazy val writer = {
    if (_dir == null || _holder == null) throw new RuntimeException("WTF")
    IndexWriter.unlock(_dir)
    if (_writer == null) _writer = new NRTIndexWriter(_dir, cfg, _holder)
    _writer
  }

  override def newReader(dir: Directory, readOnly: Boolean) = {
    _dir = dir
    if (_writer == null) {
      _holder = new RefreshableIndexHolder(IndexReader.open(dir, readOnly))
      _holder
    } else {
      writer.getReader()
    }
  }

  def close() = {
    writer.close()
  }
}