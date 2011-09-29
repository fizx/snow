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

class SnowIndexReaderFactory extends IndexReaderFactory {

  var writer: NRTIndexWriter = null

  override def newReader(dir: Directory, readOnly: Boolean) = {
    writer = new NRTIndexWriter(dir)
    writer.getReader()
  }

  def close() = {
    writer.close()
  }
}