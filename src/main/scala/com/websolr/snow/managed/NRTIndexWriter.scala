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

class NRTIndexWriter(dir: Directory) 
                     extends IndexWriter(
                       new NRTCachingDirectory(
                       dir,
                       NRTIndexWriter.maxMergeSizeMB,
                       NRTIndexWriter.maxCachedMB)
                     , new StandardAnalyzer()) {
                       
                       // val analyzer = new StandardAnalyzer(LUCENE_31)
                       // val cfg = new IndexWriterConfig(LUCENE_31, analyzer)
                       // val policy = new BalancedSegmentMergePolicy()
                       // policy.setMergeFactor(3)
                       // cfg.setMergePolicy(policy)

  val nrtDir = getDirectory.asInstanceOf[NRTCachingDirectory]
  setMergeScheduler(nrtDir.getMergeScheduler)

  val holder = new RefreshableIndexHolder(super.getReader)

  override def getReader() = {
    holder.setInternal(super.getReader())
    println(holder.numDocs)
    holder
  }
}