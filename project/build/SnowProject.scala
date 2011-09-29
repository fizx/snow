import sbt._
import com.github.olim7t.sbtscalariform._

class SnowProject(info: ProjectInfo) extends DefaultProject(info) 
    // with ScalariformPlugin 
  {
  
  override val compileOrder = CompileOrder.JavaThenScala
  
  val solr = "org.apache.solr" % "solr-core" % "1.4.1"
  
  // val solrTest = "org.apache.solr" % "solr-test-framework" % "1.4.2" % "test"
  
  val slf4j     = "org.slf4j" % "slf4j-jdk14" % "1.5.2"
  val slf4jApi  = "org.slf4j" % "slf4j-api" % "1.5.2"
    
  val util = "com.twitter" % "util-core" % "1.8.9" % "test"
  val scalaTest = "org.scalatest" % "scalatest" % "1.3" % "test"
  val mockito = "org.mockito" % "mockito-all" % "1.8.5" % "test"

  val twitter = "Twitter" at
      "http://maven.twttr.com"
  val scalaToolsSnapshots = "Scala Tools Repository" at
      "http://nexus.scala-tools.org/content/repositories/snapshots/"

}