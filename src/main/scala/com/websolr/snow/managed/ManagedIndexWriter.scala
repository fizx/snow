package org.apache.lucene.index

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

class ManagedIndexWriter(dir: Directory, 
                         cfg: IndexWriterConfig, 
                         pool: ScheduledExecutorService, 
                         flushEvery: Long, bufferSize: Int,
                         val termInfosIndexDivisor: Int = 128) 
                         extends IndexWriter(dir, cfg) {
  
  val debug = System.getenv("SNOW_DEBUG") != null
  def log(s: => String) = if(debug) println("SNOW ------" + System.currentTimeMillis + " - " + s)
  def ramCfg = new IndexWriterConfig(LUCENE_31, analyzer)
  
  
  class RefreshableIndexHolder(ir: IndexReader) extends FilterIndexReader(ir) {
    def setInternal(i: IndexReader) = this.in = i
    override def getSequentialSubReaders() = null
  }
  
  class FlushThead(name: String) extends Runnable {
    def run = {
      log("flushing from " + name)
      added.set(0)
      flush()
      added.set(0)
      log("flushed from " + name)
    }  
  }
  
  log("initializing " + getClass)
  super.commit()
  val analyzer = new StandardAnalyzer(LUCENE_31)
  val manualThread = new FlushThead("manual")
  val flushThread = new FlushThead("timer")
  val flushFuture = pool.scheduleWithFixedDelay(
                    flushThread, flushEvery, flushEvery, TimeUnit.MILLISECONDS)

  var added = new AtomicLong
  
  var baseReader: IndexReader = null; reopenDisk()
  var ramWriter: IndexWriter = null
  val ramReaders = new ListBuffer[IndexReader]; flush()
  val deletions = new ListBuffer[Term]

  val holder = new RefreshableIndexHolder(newMultiReader())

  log("initialized " + getClass)
  
  def reopenReader() = {
    holder.setInternal(newMultiReader())
  }

  override def getReader() = {
    holder.setInternal(newMultiReader())
    holder
  }

  def newMultiReader() = new Object().synchronized {
    reopenRam()
    new MultiReader((ramReaders ++ Seq(baseReader)): _*) {
      override def directory() = baseReader.directory()
      override def reopen() = throw new RuntimeException("woot")
    }
  }

  def flush() = new Object().synchronized {
    log("flush phase 1")

    val newRamDir = new RAMDirectory
    val newRamWriter = new IndexWriter(newRamDir, ramCfg)
    newRamWriter.commit()
    val newRamReader = IndexReader.open(newRamDir)
    val oldRamWriter = ramWriter
    ramWriter = newRamWriter
    ramReaders.prepend(newRamReader)
    if(oldRamWriter !=null) oldRamWriter.close
    
    log("flush phase 1")
    
    if(ramReaders.size > 1) {
      val toMerge = ramReaders.tail
      
      if (deletions.size > 0) {
        val toDelete = deletions.clone()
        super.deleteDocuments(toDelete: _*)
        deletions.trimEnd(toDelete.size)
        log("flushed deletions")
      }

      addIndexes(toMerge.map(_.directory): _*)
      log("addedIndexes")
      
      reopenDisk()
      ramReaders.trimEnd(toMerge.size)
    }
    log("flushed")
  }
  
  override def updateDocument(term: Term, doc: Document) = updateDocument(term, doc, getAnalyzer)
  override def updateDocument(term: Term, doc: Document, analyzer: Analyzer) = {
    log("predelete")
    deleteDocuments(term)
    log("preadd")
    addDocument(doc, analyzer)
    log("postadd")
  }

  override def addDocument(doc: Document) = addDocument(doc, getAnalyzer)
  override def addDocument(doc: Document, analyzer: Analyzer) = {
    if (added.incrementAndGet > bufferSize) {
      added.set(0)
      pool.submit(manualThread)
    }
    ramWriter.addDocument(doc, analyzer)
  }

  override def deleteDocuments(term: Term): Unit = {
    log("deleting documents for term")
    ramWriter.deleteDocuments(term)
    log("from ram")
    baseReader.deleteDocuments(term)
    log("from diskreader")
    deletions.prepend(term)
    // // super.deleteDocuments(term)
    // log("from diskwriter")
    // log("deleted documents for term")
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

  override def deleteAll() = {//synchronized {
    log("deleting all")
    ramWriter.deleteAll()
    reopenRam()
    super.deleteAll()
    reopenDisk()
  }
  
  def reopenRam(): IndexReader = new Object().synchronized { 
    log("reopening ram")
    if(ramWriter != null) ramWriter.commit()
    ramReaders(0) = IndexReader.open(ramWriter.getDirectory)
    log("reopened ram")
    ramReaders(0)
  }

  /**
   * Tricky code.  This is the NRT reopen, however we open the reader as an unlocked writable reader!!
   * This would be unsafe if we ever flushed it to disk, but we're just using its capabilities of 
   * tracking deletes, which a read-only reader doesn't have.
   */
  def reopenDisk(): IndexReader = new Object().synchronized {
    log("reopening disk")
    super.commit()
    baseReader = new DirectoryReader(getDirectory(), segmentInfos, null, false, termInfosIndexDivisor, null) {
      override def acquireWriteLock() = ()
    }
    log("reopened disk")
    baseReader
  }

  def numDocsOnDisk() = baseReader.numDocs
  def numDocsInRAM(): Int = ramReaders.foldLeft(0)(_+_.numDocs)
  
  override def close() = {
    flushFuture.cancel(true)
    super.close()
  }
}