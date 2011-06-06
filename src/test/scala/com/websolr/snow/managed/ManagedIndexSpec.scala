package com.websolr.snow.managed

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import java.io.File
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
import org.apache.lucene.index._
import org.apache.lucene.util.Version._

class ManagedIndexSpec extends AbstractSpec {
  val tmp = new File("/tmp/snow")
  var factory: ManagedIndex = null
  var writer: ManagedIndexWriter = null
  var i = 0

  def addAndRead(string: String = "hello") = {
    writer.addDocument(doc(string))
    i += 1
    factory.getReader.numDocs() should equal(i)
  }

  override def beforeEach {
    FileUtils.deleteQuietly(tmp)
    tmp.mkdirs()
    factory = ManagedIndex(tmp)
    factory.open()
    writer = factory.getWriter
    i = 0
  }

  override def afterEach {
    factory.close()
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
        factory.getReader.numDocs() should equal(1)
        writer.updateDocument(new Term("default", "hello"), doc("hello"))
        factory.getReader.numDocs() should equal(1)
      }

      it("should be able to update disk doc") {
        writer.updateDocument(new Term("default", "hello"), doc("hello"))
        factory.getReader.numDocs() should equal(1)
        writer.forceRealtimeToDisk()
        writer.updateDocument(new Term("default", "hello"), doc("hello"))
        factory.getReader.numDocs() should equal(1)
      }
      // 
      it("should take much less than 1ms to get the reader") {
        val before = Time.now
        for (i <- 1 to 10) { factory.getReader }
        (Time.now - before).inMillis.toInt should (be < 3)
      }

      it("should be able to merge back to the main reader") {
        for (i <- 1 to 10) { addAndRead() }
        writer.forceRealtimeToDisk()
        writer.numDocsOnDisk should equal(i)
        writer.numDocsInRAM should equal(0)
      }

      it("should be able to quickly delete document in RAM") {
        for (i <- 1 to 10) { addAndRead(i.toString) }
        writer.numDocsInRAM should equal(10)

        writer.deleteDocuments(new Term("default", "1"))
        writer.numDocsInRAM should equal(9)

        inLessThan(10.millis) {
          for (i <- 2 to 9) {
            writer.deleteDocuments(new Term("default", i.toString))
          }
        }
        writer.numDocsInRAM should equal(1)

        writer.forceRealtimeToDisk()
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
        writer.forceRealtimeToDisk()
        writer.numDocsOnDisk should equal(10)
        writer.deleteDocuments(new Term("default", "1"))
        writer.numDocsOnDisk should equal(9)
      }
    }
  }
}
