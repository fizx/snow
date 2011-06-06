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
  
  val debug = System.getenv("SNOW_DEBUG") != null
  def log(s: => String) = if(debug) println("SNOW ------" + System.currentTimeMillis + " - " + s)

  var lastReader: CIR = null
  val termInfosIndexDivisor = 128

  val flushThread = new Runnable {
    def run = {
      log("flushing realtime from timer thread")
      if (numDocsInRAM() > 0) forceRealtimeToDisk()
      log("flushed realtime from timer thread")
    }
  }

  val flushFuture = pool.scheduleWithFixedDelay(flushThread, flushEvery, flushEvery, TimeUnit.MILLISECONDS)

  override def close() = {
    log("FATAL closing IndexWriter")
    flushFuture.cancel(true)
    super.close()
  }

  log("initializing " + getClass)
  super.commit()
  def ramCfg = new IndexWriterConfig(LUCENE_31, analyzer)
  val analyzer = new StandardAnalyzer(LUCENE_31)
  var baseReader: IndexReader = null; reopenDisk()
  var ramDir = new RAMDirectory
  var ramWriter = new IndexWriter(ramDir, ramCfg)
  ramWriter.commit()
  var ramReader = IndexReader.open(ramDir)
  var added = 0
  
  log("initialized " + getClass)

  def reopenRam() = { 
    log("reopening ram")
    ramWriter.commit()
    added = 0
    ramReader = IndexReader.open(ramDir) 
    log("reopened ram")
  }

  /**
   * Tricky code.  This is the NRT reopen, however we open the reader as an unlocked writable reader!!
   * This would be unsafe if we ever flushed it to disk, but we're just using its capabilities of 
   * tracking deletes, which a read-only reader doesn't have.
   */
  def reopenDisk() = synchronized {
    log("reopening disk")
    super.commit()
    baseReader = new DirectoryReader(getDirectory(), segmentInfos, null, false, termInfosIndexDivisor, null) {
      override def acquireWriteLock() = ()
    }
    log("reopened disk")
  }

  override def updateDocument(term: Term, doc: Document) = updateDocument(term, doc, getAnalyzer)
  override def updateDocument(term: Term, doc: Document, analyzer: Analyzer) = {
    deleteDocuments(term)
    addDocument(doc, analyzer)
  }

  override def addDocument(doc: Document) = addDocument(doc, getAnalyzer)
  override def addDocument(doc: Document, analyzer: Analyzer) = {
    if (added >= bufferSize) {
      log("manually flushing")
      forceRealtimeToDisk()
      log("manually flushed")
    }
    log("adding document")
    ramWriter.addDocument(doc, analyzer)
    added += 1
  }

  override def deleteDocuments(term: Term): Unit = {
    log("deleting documents for term")
    ramWriter.deleteDocuments(term)
    baseReader.deleteDocuments(term)
    super.deleteDocuments(term)
    log("deleted documents for term")
  }

  override def deleteDocuments(queries: Query*): Unit = queries.foreach(q => deleteDocuments(q))
  override def deleteDocuments(query: Query): Unit = {
    log("deleting documents for query")
    ramWriter.deleteDocuments(query)
    reopenRam()
    super.deleteDocuments(query)
    reopenDisk()
    log("deleted documents for query")
  }

  override def deleteAll() = synchronized {
    log("deleting all")
    ramWriter.deleteAll()
    reopenRam()
    super.deleteAll()
    reopenDisk()
  }

  class CIR(ir: IndexReader) extends FilterIndexReader(ir) {
    def setInternal(i: IndexReader) = this.in = i
    override def getSequentialSubReaders() = null
  }

  def reopenReader() = {
    lastReader.setInternal(nReader)
  }

  override def getReader() = {
    lastReader = new CIR(nReader())
    lastReader
  }

  def nReader() = {
    reopenRam()
    new MultiReader(ramReader, baseReader) {
      override def directory() = baseReader.directory()
      override def reopen() = throw new RuntimeException("woot")
    }
  }

  def forceRealtimeToDisk() = { addToDiskAndReopenReader(swapRamDirs) }

  def addToDiskAndReopenReader(dir: Directory) {
    addIndexes(dir)
    reopenDisk()
  }

  def swapRamDirs() = synchronized {
    val newRamDir = new RAMDirectory
    val newRamWriter = new IndexWriter(newRamDir, ramCfg)
    newRamWriter.commit()
    val newRamReader = IndexReader.open(newRamDir)
    val oldRamDir = ramDir
    val oldRamWriter = ramWriter
    val oldRamReader = ramReader
    ramDir = newRamDir
    ramWriter = newRamWriter
    ramReader = newRamReader
    oldRamWriter.close
    oldRamDir
  }

  def numDocsOnDisk() = baseReader.numDocs
  def numDocsInRAM() = ramReader.numDocs
}