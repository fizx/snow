import sbt._
import com.github.olim7t.sbtscalariform._

class SnowProject(info: ProjectInfo) extends DefaultProject(info) with ScalariformPlugin 
  {
  
  val solr = "org.apache.solr" % "solr-core" % "3.1.0"
    
  val util = "com.twitter" % "util-core" % "1.8.9" % "test"
  val scalaTest = "org.scalatest" % "scalatest" % "1.3" % "test"
  val mockito = "org.mockito" % "mockito-all" % "1.8.5" % "test"

  val twitter = "Twitter" at
      "http://maven.twttr.com"
  val scalaToolsSnapshots = "Scala Tools Repository" at
      "http://nexus.scala-tools.org/content/repositories/snapshots/"
  // val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at
  //     "https://oss.sonatype.org/content/repositories/snapshots"
  // val sonatypeNexusReleases = "Sonatype Nexus Releases" at 
  //     "https://oss.sonatype.org/content/repositories/releases"
  // val fuseSourceSnapshots = "FuseSource Snapshot Repository" at 
  //     "http://repo.fusesource.com/nexus/content/repositories/snapshots"
  // val clojars = "Clojars" at 
  //     "http://clojars.org/repo/"
      
  // def thriftSources = (mainSourcePath / "thrift" ##) ** "*.thrift"
  // def thriftBin = "thrift"
  // 
  // def compileThriftAction(lang: String) = task {
  //   import Process._
  //   outputPath.asFile.mkdirs()
  //   val tasks = thriftSources.getPaths.map { path =>
  //     execTask { "%s --gen %s -o %s %s".format(thriftBin, lang, outputPath.absolutePath, path) }
  //   }
  //   if (tasks.isEmpty) None else tasks.reduceLeft { _ && _ }.run
  // } describedAs("Compile thrift into %s".format(lang))
  // 
  // lazy val compileThriftJava = compileThriftAction("java")
  // 
  // override def compileAction = super.compileAction dependsOn(compileThriftJava)
  // 
  // def generatedJavaDirectoryName   = "gen-java"
  // def generatedJavaPath   = outputPath / generatedJavaDirectoryName
  // 
  // override def mainSourceRoots = super.mainSourceRoots +++ (outputPath / generatedJavaDirectoryName ##)
  // 
  // lazy val cleanGenerated = cleanTask(generatedJavaPath) describedAs "Clean generated source folders"
  // 
  // override def cleanAction = super.cleanAction dependsOn(cleanGenerated)
}