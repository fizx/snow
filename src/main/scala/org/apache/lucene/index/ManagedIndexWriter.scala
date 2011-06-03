package org.apache.lucene.index

import org.apache.lucene.store._
import org.apache.lucene.analysis._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.document._
import org.apache.lucene.search._
import org.apache.lucene.index._
import java.util.concurrent._
import org.apache.lucene.util.Version._

class ManagedIndexWriter(dir: Directory, cfg: IndexWriterConfig, pool: ScheduledExecutorService, flushEvery: Long, bufferSize: Int) extends IndexWriter(dir, cfg) {

  val termInfosIndexDivisor = 128

  val flushThread = new Runnable {
    def run = {
      if (numDocsInRAM() > 0) forceRealtimeToDisk()
    }
  }

  val flushFuture = pool.scheduleWithFixedDelay(flushThread, flushEvery, flushEvery, TimeUnit.MILLISECONDS)

  override def close() = {
    flushFuture.cancel(true)
    super.close()
  }

  super.commit()
  def ramCfg = new IndexWriterConfig(LUCENE_31, analyzer)
  val analyzer = new StandardAnalyzer(LUCENE_31)
  var baseReader: IndexReader = null; reopenDisk()
  var ramDir = new RAMDirectory
  var ramWriter = new IndexWriter(ramDir, ramCfg)
  ramWriter.commit()
  var ramReader = IndexReader.open(ramDir)
  var added = 0

  def reopenRam() = { ramWriter.commit(); added = 0; ramReader = IndexReader.open(ramDir) }

  /**
   * Tricky code.  This is the NRT reopen, however we open the reader as an unlocked writable reader!!
   * This would be unsafe if we ever flushed it to disk, but we're just using its capabilities of 
   * tracking deletes, which a read-only reader doesn't have.
   */
  def reopenDisk() = {
    flush(false, true)
    baseReader = new DirectoryReader(getDirectory(), segmentInfos, null, false, termInfosIndexDivisor, null) {
      override def acquireWriteLock() = ()
    }
  }

  override def updateDocument(term: Term, doc: Document) = updateDocument(term, doc, getAnalyzer)
  override def updateDocument(term: Term, doc: Document, analyzer: Analyzer) = {
    deleteDocuments(term)
    addDocument(doc, analyzer)
  }

  override def addDocument(doc: Document) = addDocument(doc, getAnalyzer)
  override def addDocument(doc: Document, analyzer: Analyzer) = {
    if (added >= bufferSize) forceRealtimeToDisk()
    ramWriter.addDocument(doc, analyzer)
    reopenRam()
    added += 1
  }

  override def deleteDocuments(term: Term): Unit = {
    ramWriter.deleteDocuments(term)
    reopenRam()
    baseReader.deleteDocuments(term)
    super.deleteDocuments(term)
  }

  override def deleteDocuments(queries: Query*): Unit = queries.foreach(q => deleteDocuments(q))
  override def deleteDocuments(query: Query): Unit = {
    ramWriter.deleteDocuments(query)
    reopenRam()
    super.deleteDocuments(query)
    reopenDisk()
  }

  override def deleteAll() = synchronized {
    ramWriter.deleteAll()
    reopenRam()
    super.deleteAll()
    reopenDisk()
  }

  override def getReader() = {
    new MultiReader(ramReader, baseReader) {
      override def directory() = baseReader.directory()
    }
  }

  def forceRealtimeToDisk() = synchronized { addToDiskAndReopenReader(swapRamDirs) }

  def addToDiskAndReopenReader(dir: Directory) {
    addIndexes(dir)
    reopenDisk()
  }

  def swapRamDirs() = {
    val newRamDir = new RAMDirectory
    val newRamWriter = new IndexWriter(newRamDir, ramCfg)
    newRamWriter.commit()
    val newRamReader = IndexReader.open(newRamDir)
    val oldRamDir = ramDir
    val oldRamWriter = ramWriter
    val oldRamReader = ramReader
    oldRamWriter.close
    oldRamReader.close
    synchronized {
      ramDir = newRamDir
      ramWriter = newRamWriter
      ramReader = newRamReader
    }
    oldRamDir
  }

  def numDocsOnDisk() = baseReader.numDocs
  def numDocsInRAM() = ramReader.numDocs
}