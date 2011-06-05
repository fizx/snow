package com.km.snow.solr

import com.km.snow.managed._
import org.apache.solr.search._

import org.apache.solr.core._
import org.apache.solr.common.util._
import org.apache.solr.update._

import org.apache.solr.core.SolrInfoMBean._
import java.io._
import org.apache.lucene.search.BooleanClause._
import org.apache.lucene.index._
import org.apache.lucene.store._
import org.apache.lucene.search._
import java.util.concurrent._
import java.util.concurrent.atomic._
import org.apache.lucene.util.Version._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.index.IndexWriterConfig.OpenMode._

class RealtimeUpdateHandler(core: SolrCore) extends UpdateHandler(core) {

  val addCommandsCumulative = new AtomicLong()
  val deleteByIdCommandsCumulative = new AtomicLong()
  val deleteByQueryCommandsCumulative = new AtomicLong()
  val mergeIndexesCommands = new AtomicLong()
  val optimizeCommands = new AtomicLong()
  val numErrorsCumulative = new AtomicLong()

  val irf = core.getIndexReaderFactory().asInstanceOf[ManagedIndexReaderFactory]
  val writer = irf.writer

  def close() = {
    irf.close()
  }

  def rollback(cmd: RollbackUpdateCommand) = throw new UnsupportedOperationException

  def commit(cmd: CommitUpdateCommand) = {
    if (cmd.optimize) {
      optimizeCommands.incrementAndGet
      writer.optimize(cmd.maxOptimizeSegments)
      writer.reopenReader()
    }
  }

  def mergeIndexes(cmd: MergeIndexesCommand) = {
    val dirs = cmd.dirs
    if (dirs != null && dirs.length > 0) {
      mergeIndexesCommands.incrementAndGet
      writer.addIndexes(dirs: _*)
    }
    writer.reopenReader()
    1
  }

  def deleteByQuery(cmd: DeleteUpdateCommand) = {
    val q = QueryParsing.parseQuery(cmd.query, schema)
    val delAll = classOf[MatchAllDocsQuery] == q.getClass()

    if (delAll) {
      deleteAll()
    } else {
      writer.deleteDocuments(q)
    }
    writer.reopenReader()
    deleteByQueryCommandsCumulative.incrementAndGet()
  }

  def deleteAll() = writer.deleteAll()

  def delete(cmd: DeleteUpdateCommand) = {
    deleteByIdCommandsCumulative.incrementAndGet()
    writer.deleteDocuments(idTerm.createTerm(idFieldType.toInternal(cmd.id)))
    writer.reopenReader()
  }

  def addDoc(cmd: AddUpdateCommand) = {
    var rc = 0
    try {
      if (cmd.indexedId == null) {
        cmd.indexedId = getIndexedId(cmd.doc)
      }

      if (cmd.overwriteCommitted || cmd.overwritePending) {
        val idTerm = this.idTerm.createTerm(cmd.indexedId)

        var del = false
        val updateTerm = if (cmd.updateTerm == null) {
          idTerm
        } else {
          del = true
          cmd.updateTerm
        }

        if (del) { // ensure id remains unique
          val bq = new BooleanQuery()
          bq.add(new BooleanClause(new TermQuery(updateTerm), Occur.MUST_NOT))
          bq.add(new BooleanClause(new TermQuery(idTerm), Occur.MUST))
          writer.deleteDocuments(bq)
        }

        writer.updateDocument(updateTerm, cmd.getLuceneDocument(schema))
      } else {
        writer.addDocument(cmd.getLuceneDocument(schema))
      }
      rc = 1
    } finally {
      if (rc != 1) {
        numErrorsCumulative.incrementAndGet();
      } else {
        addCommandsCumulative.incrementAndGet();
      }
    }
    writer.reopenReader()
    rc
  }

  def getStatistics() = {
    val lst = new org.apache.solr.common.util.SimpleOrderedMap[Long]()
    lst.add("optimizes", optimizeCommands.get())
    lst.add("cumulative_adds", addCommandsCumulative.get())
    lst.add("cumulative_deletesById", deleteByIdCommandsCumulative.get())
    lst.add("cumulative_deletesByQuery", deleteByQueryCommandsCumulative.get())
    lst.add("cumulative_errors", numErrorsCumulative.get())
    lst
  }

  override def toString() = "RealtimeUpdateHandler" + getStatistics()
  def getName = getClass.getName
  def getVersion() = "0.1"
  def getDescription() = "Realtime update handler"
  def getCategory() = Category.UPDATEHANDLER
  def getSourceId() = "N/A"
  def getSource() = "SNOW"
  def getDocs() = null
}