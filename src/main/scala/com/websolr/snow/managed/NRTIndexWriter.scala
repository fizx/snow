package com.websolr.snow.managed

import org.apache.lucene.store._
import org.apache.lucene.analysis._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.document._
import org.apache.lucene.search._
import org.apache.lucene.index._
import java.util.concurrent._
import scala.collection.mutable.ListBuffer
import java.util.concurrent.atomic._
import org.apache.lucene.util.Version._

object NRTIndexWriter {
  val maxMergeSizeMB = 5
  val maxCachedMB = 60
}

class NRTIndexWriter(dir: Directory, cfg: IndexWriterConfig) 
                     extends IndexWriter(
                       new NRTCachingDirectory2(
                       dir,
                       NRTIndexWriter.maxMergeSizeMB,
                       NRTIndexWriter.maxCachedMB)
                     , cfg) {

  val nrtDir = getDirectory.asInstanceOf[NRTCachingDirectory2]
  setMergeScheduler(nrtDir.getMergeScheduler)

  val holder = new RefreshableIndexHolder(super.getReader)

  override def getReader() = {
    holder.setInternal(super.getReader())
    holder
  }
}