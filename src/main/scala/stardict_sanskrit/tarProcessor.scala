package stardict_sanskrit

import java.io.{File, PrintWriter}
import java.nio.file.{Path, Paths}

import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source
import scala.sys.process._


object tarProcessor extends BatchProcessor {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val filePatternToTar = ".*\\.ifo|.*\\.idx|.*\\.dz|.*\\.ifo|.*\\.syn|.*LICENSE.*"

  def writeFilesListMd(mdPath: String, urlBase: String, githubRepoOpt: Option[GithubRepo] = None, ext: String): Unit = {
    log.info(s"======================= Updating files list at ${urlBase}")
    val outFileObj = new File(mdPath)
    outFileObj.getParentFile.mkdirs
    val destination = new PrintWriter(outFileObj)
    val urlBaseFinal = urlBase.replaceAll("/$", "")
    val localArchives = outFileObj.getParentFile.listFiles().map(_.getCanonicalFile).filter(_.getName.endsWith(ext))
    val localDictionaryNames = localArchives.map(x=>getDictIdFromName(x.getName))
    var uploadedArchives = List[String]()
    if (githubRepoOpt.isDefined) {
      uploadedArchives = githubRepoOpt.get.getContentList(tarDirFilePath=outFileObj.getParentFile.getAbsolutePath).filter(x => !localDictionaryNames.contains(getDictIdFromName(x.name))).map(_.download_url.get)
    }
    val localArchiveUrls = localArchives.map(x => s"${urlBaseFinal}/${x.getName.replaceAll(".*/", "")}")
    ( localArchiveUrls.toList ::: uploadedArchives).sorted.foreach(x => {
      destination.println(x + "  ")
    })
    destination.close()
  }

  def getTimestampFromName(fileName: String): Option[String] = fileName.split("/").last.split("\\.")(0).split("__").toList.tail.headOption

  def getDictIdFromName(fileName: String): String = fileName.split("/").last.split("\\.")(0).split("__")(0)
  

  //noinspection AccessorLikeMethodIsUnit
  def getStats(): Unit = {
    val indexIndexorum = "https://raw.githubusercontent.com/indic-dict/stardict-index/master/dictionaryIndices.md"
    //noinspection SourceNotClosed
    val indexes = Source.fromURL(indexIndexorum).mkString.replaceAll("<|>","").split("\n")
    val counts = indexes.map(index => {
      // Example: https://raw.githubusercontent.com/sanskrit-coders/stardict-sanskrit/master/sa-head/en-entries/tars/tars.MD
      val indexShortName = index.replaceAll("https://raw.githubusercontent.com/sanskrit-coders/|master/|tars/tars.MD", "")
      //noinspection SourceNotClosed
      val indexCount = Source.fromURL(index).mkString.replaceAll("<|>","").split("\n").length
      indexShortName -> indexCount
    })
    counts.sortBy(_._1).foreach(x => {
      println(f"${x._1}%-50s : ${x._2}")
    })
    println(f"${"Total"}%-50s : ${counts.toMap.values.sum}")
  }

  def main(args: Array[String]): Unit = {
    var workingDir = "/home/vvasuki/stardict-marathi/ma-head/other-entries/tars"
//    compressAllDicts(List(workingDir), workingDir + "all_dicts.tar.gz")
    //    getStats
  }
}
