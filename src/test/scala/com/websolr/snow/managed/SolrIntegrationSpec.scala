package com.websolr.snow.managed

import com.websolr.snow.solr._
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import org.apache.solr.common.params.CommonParams
import org.apache.solr.request._
import java.io.File
import org.apache.solr.update._
import org.apache.commons.io._
import org.apache.lucene.document.Field._
import org.apache.lucene.document._
import org.apache.lucene.analysis.miscellaneous._
import org.apache.lucene.analysis._
import com.twitter.conversions.time._
import org.apache.solr.search._
import com.twitter.util._
import com.websolr.snow._
import org.apache.lucene.search._
import org.apache.lucene.index._
import org.apache.solr.util.TestHarness

class SolrIntegrationSpec extends AbstractSpec {

  val solrConfigFile = new File("src/test/resources/solrconfig.xml")
  val schemaFile = new File("src/test/resources/schema.xml")
  val dataDir = new File("/tmp/test-index")
  FileUtils.deleteDirectory(dataDir)

  val solrConfig = TestHarness.createConfig(solrConfigFile.getAbsolutePath)
  val h = new TestHarness(dataDir.getAbsolutePath(),
    solrConfig,
    schemaFile.getAbsolutePath);

  describe("Solr Integration") {
    it("should show adds immediately") {
      val updater = h.getCore.getUpdateHandler().asInstanceOf[RealtimeUpdateHandler]
      val cmd = new AddUpdateCommand()
      cmd.overwriteCommitted = true
      cmd.overwritePending = true
      cmd.allowDups = false

      // Add a valid document
      cmd.doc = new Document()
      cmd.doc.add(new Field("id", "AAA", Store.YES, Index.ANALYZED))
      cmd.doc.add(new Field("text", "xxxxx", Store.YES, Index.ANALYZED))
      updater.addDoc(cmd)
      
      val cmt = new CommitUpdateCommand(false)
      updater.commit(cmt)
      
      val foo = h.getCore.getSearcher(false, true, null).get()
      
      val searcher = new IndexSearcher(updater.writer.holder)
      val q = QueryParsing.parseQuery("*:*", h.getCore.getSchema)
      searcher.search(q, 10).totalHits should equal(1)

      val args = new java.util.HashMap[String, String]
      args.put(CommonParams.Q, "*:*")
      val req = new LocalSolrQueryRequest(h.getCore(), new MapSolrParams(args));

      val response = h.query(req)
      println(response)
      response.contains("numFound=\"1") should equal(true)
    }
  }
}
