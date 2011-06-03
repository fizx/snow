package com.km.snow.managed

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
import com.twitter.util._
import com.km.snow._
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
      val updater = h.getCore.getUpdateHandler()
      val cmd = new AddUpdateCommand()
      cmd.overwriteCommitted = true
      cmd.overwritePending = true
      cmd.allowDups = false

      // Add a valid document
      cmd.doc = new Document()
      cmd.doc.add(new Field("id", "AAA", Store.YES, Index.NOT_ANALYZED))
      cmd.doc.add(new Field("subject", "xxxxx", Store.YES, Index.NOT_ANALYZED))
      updater.addDoc(cmd)

      val args = new java.util.HashMap[String, String]
      args.put(CommonParams.Q, "id:AAA")
      val req = new LocalSolrQueryRequest(h.getCore(), new MapSolrParams(args));

      val response = h.query(req)
      val results = h.validateXPath(response, "//*[@numFound='1']")
      results should equal("//*[@numFound='1']")
    }
  }
}
