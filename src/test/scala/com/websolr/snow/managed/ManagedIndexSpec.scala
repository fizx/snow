package com.websolr.snow.managed

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import java.io.File
import java.util.concurrent._
import org.apache.commons.io._
import org.apache.lucene.document._
import org.apache.lucene.analysis.miscellaneous._
import org.apache.lucene.analysis._
import com.twitter.conversions.time._
import org.apache.lucene.queryParser._
import com.twitter.util._
import org.apache.lucene.analysis.standard._
import com.websolr.snow._
import org.apache.lucene.search._
import org.apache.lucene.store._
import org.apache.lucene.index._
import org.apache.lucene.util.Version._

class ManagedIndexSpec extends AbstractSpec {
  val tmp = new File("/tmp/snow")
  val pool = Executors.newScheduledThreadPool(8)
  var writer: ManagedIndexWriter = null
  var reader: IndexReader = null
  var i = 0

  def addAndRead(string: String = "hello") = {
    writer.addDocument(doc(string))
    i += 1
    reader = writer.getReader
    reader.numDocs() should equal(i)
  }

  override def beforeEach {
    FileUtils.deleteQuietly(tmp)
    tmp.mkdirs()
    val dir = FSDirectory.open(tmp)
    writer = new ManagedIndexWriter(
         dir, 
         new IndexWriterConfig(LUCENE_31, new StandardAnalyzer(LUCENE_31)), 
         pool, 
         10000, 10000)
    reader = writer.getReader
    i = 0
  }

  override def afterEach {
    writer.close()
  }

  describe("A ManagedIndexReader") {
    describe("recurring reads and writes") {
      it("should be able to index and then be updated immediately") {
        addAndRead()
      }

      it("should take less than 5ms per cycle") {
        for (i <- 1 to 100) { inLessThan(1.second) { addAndRead() } } // warm up
        inLessThan(40.millis) {
          for (i <- 1 to 10) { addAndRead() }
        }
      }
      
      it("should be able to update ram doc") {
        writer.updateDocument(new Term("default", "hello"), doc("hello"))
        writer.getReader.numDocs() should equal(1)
        writer.updateDocument(new Term("default", "hello"), doc("hello"))
        writer.getReader.numDocs() should equal(1)
      }

      it("should be able to update disk doc") {
        writer.updateDocument(new Term("default", "hello"), doc("hello"))
        writer.getReader.numDocs() should equal(1)
        writer.numDocsInRAM should equal(1)
        writer.numDocsOnDisk should equal(0)
        
        writer.flush()
        writer.numDocsInRAM should equal(0)
        writer.numDocsOnDisk should equal(1)

        writer.updateDocument(new Term("default", "hello"), doc("hello"))
        writer.getReader.numDocs() should equal(1)
        writer.numDocsInRAM should equal(1)
        writer.numDocsOnDisk should equal(0)        
      }

      it("should take much less than 1ms to get the reader") {
        for (i <- 1 to 100) { writer.getReader } // warmup JIT
        val before = Time.now
        for (i <- 1 to 10) { writer.getReader }
        (Time.now - before).inMillis.toInt should (be < 3)
      }

      it("should be able to quickly delete document in RAM") {
        for (i <- 1 to 10) { addAndRead(i.toString) }
        writer.numDocsInRAM should equal(10)
      
        writer.deleteDocuments(new Term("default", "1"))
        writer.reopenReader
        writer.numDocsInRAM should equal(9)
      
        inLessThan(10.millis) {
          for (i <- 2 to 9) {
            writer.deleteDocuments(new Term("default", i.toString))
          }
        }
        writer.reopenReader
        writer.numDocsInRAM should equal(1)
      
        writer.flush()
        writer.numDocsInRAM should equal(0)
        writer.numDocsOnDisk should equal(1)
      }

      it("should be searchable") {
        for (i <- 1 to 10) { addAndRead(i.toString) }
        writer.numDocsInRAM should equal(10)
      
        writer.deleteDocuments(new Term("default", "1"))
        val searcher = new IndexSearcher(writer.getReader)
        val qp = new QueryParser(LUCENE_31, "default", new StandardAnalyzer(LUCENE_31))
      
        searcher.search(qp.parse("1"), 100).totalHits should equal(0)
      
        searcher.search(qp.parse("2"), 100).totalHits should equal(1)
      }

      it("should be able to delete document from disk") {
        for (i <- 1 to 10) { addAndRead(i.toString) }
        writer.flush()
        writer.numDocsOnDisk should equal(10)
        writer.deleteDocuments(new Term("default", "1"))
        writer.numDocsOnDisk should equal(9)
        writer.deletions.size should equal(1)
        writer.flush()
        writer.numDocsOnDisk should equal(9)
      }
    }
  }
}
