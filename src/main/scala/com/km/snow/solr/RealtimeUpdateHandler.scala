package com.km.snow.solr

import com.km.snow.managed._

import org.apache.solr.core._
import org.apache.solr.common.util._
import org.apache.solr.update._

import java.io._
import org.apache.log4j.Logger
import org.apache.lucene.index._
import org.apache.lucene.store._
import org.apache.lucene.search._
import java.util.concurrent._
import org.apache.lucene.util.Version._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.index.IndexWriterConfig.OpenMode._

class RealtimeUpdateHandler(core: SolrCore) extends DirectUpdateHandler2(core) {
  val log = Logger.getLogger(classOf[RealtimeUpdateHandler])
  val irf = core.getIndexReaderFactory().asInstanceOf[ManagedIndexReaderFactory]
  writer = irf.writer

  override def openWriter() = {}
  override def closeWriter() = {}

  override def close() = {
    super.close()
    irf.writer.close()
  }

}