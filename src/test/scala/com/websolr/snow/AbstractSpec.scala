 package com.websolr.snow

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import java.io._
import java.nio._
import org.apache.commons.io._
import org.apache.lucene.document._
import org.apache.lucene.analysis.miscellaneous._
import org.apache.lucene.analysis._
import com.twitter.conversions.time._
import com.twitter.util._
import org.apache.lucene.search._
import org.apache.lucene.index._

abstract class AbstractSpec extends Spec with ShouldMatchers with BeforeAndAfterEach {
  def doc(s: String) = {
    val d = new Document()
    val t = new Token(s, 0, s.length)
    val f = new Field("default", s, Field.Store.YES, Field.Index.ANALYZED)
    d.add(f)
    d
  }

  def s2buf(s: String) = ByteBuffer.wrap(s.getBytes)
  def buf2s(b: ByteBuffer) = {
    val a = new Array[Byte](b.remaining)
    b.get(a)
    new String(a)
  }

  def inLessThan(d: Duration)(block: => Any) {
    val before = Time.now
    block
    (Time.now - before) should (be < d)
  }
}
