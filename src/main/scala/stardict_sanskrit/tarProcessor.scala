package stardict_sanskrit

import java.io.{File, PrintWriter}

import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source
import scala.sys.process._

object tarProcessor extends BatchProcessor {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val filePatternToTar = ".*\\.ifo|.*\\.idx|.*\\.dz|.*\\.ifo|.*\\.syn|.*LICENSE.*"
  def writeTarsList(tarDestination: String, urlBase: String) = {
    val outFileObj = new File(tarDestination + "/tars.MD")
    outFileObj.getParentFile.mkdirs
    val destination = new PrintWriter(outFileObj)
    val urlBaseFinal = urlBase.replaceAll("/$", "")
    outFileObj.getParentFile.listFiles().map(_.getCanonicalFile).filter(_.getName.endsWith("tar.gz")).toList.sorted.foreach(x => {
      destination.println(s"${urlBaseFinal}/${x.getName.replaceAll(".*/", "")}")
    })
    destination.close()
  }

  def makeTars(urlBase: String, file_pattern: String = ".*") = {
    log info "=======================makeTars"
    // Get timestamp.
    var dictionaries = getMatchingDictionaries(file_pattern).filter(_.ifoFile.isDefined)
    log info (s"Got ${dictionaries.filter(_.tarFile.isDefined).length} tar files")
    log info (s"Got ${dictionaries.filter(x => x.ifoFile.isDefined && !x.tarFile.isDefined).length}  dicts without tarFile files but with ifo file.")
    if (file_pattern == ".*" && dictionaries.nonEmpty) {
      val tarDirFile = dictionaries.head.getTarDirFile
      val excessTarFiles = tarDirFile.listFiles.filterNot(x => {
        val name = x.getName
        name != "tars.MD" && dictionaries.filter(_.tarFile.isDefined).map(_.tarFile.get.getCanonicalPath).contains(x.getCanonicalPath)
      })
      log warn s"Removing ${excessTarFiles.length} excessTarFiles"
      excessTarFiles.foreach(_.delete())
      writeTarsList(dictionaries.head.getTarDirFile.getCanonicalPath, urlBase)
    }
    var dictsToIgnore = dictionaries.filter(_.tarFileNewerThanIfo())
    if (dictsToIgnore.nonEmpty) {
      log warn s"Ignoring these files, whose dict files seem updated: " + dictsToIgnore.mkString("\n")
    }
    dictionaries = dictionaries.filterNot(_.tarFileNewerThanIfo())


    log info(s"got ${dictionaries.length} dictionaries which need to be updated.")
    dictionaries.foreach(_.makeTar(filePatternToTar))

    if (dictionaries.nonEmpty) {
      writeTarsList(dictionaries.head.getTarDirFile.getCanonicalPath, urlBase)
    }
  }

  def compressAllDicts(basePaths: Seq[String], tarFilePath: String) = {
    val dictDirFiles = basePaths.flatMap(basePath => getRecursiveListOfFiles(new File(basePath))).
      filter(_.getName.matches(".*\\.ifo")).map(_.getParentFile)
    val targetTarFile = new File(tarFilePath)
    targetTarFile.getParentFile.mkdirs
    //
    val filesToCompress = dictDirFiles.flatMap(_.listFiles.map(_.getCanonicalPath).filter(x => x.matches(filePatternToTar)))
    val command = s"tar --transform s,${basePaths.map(_.replaceFirst("/", "")).mkString("|")},,g -czf ${targetTarFile.getCanonicalPath} ${filesToCompress.mkString(" ")}"
    log info command
    command.!
  }


  def getStats() = {
    val indexIndexorum = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md"
    val indexes = Source.fromURL(indexIndexorum).mkString.replaceAll("<|>","").split("\n")
    val counts = indexes.map(index => {
      // Example: https://raw.githubusercontent.com/sanskrit-coders/stardict-sanskrit/master/sa-head/en-entries/tars/tars.MD
      val indexShortName = index.replaceAll("https://raw.githubusercontent.com/sanskrit-coders/|master/|tars/tars.MD", "")
      val indexCount = Source.fromURL(index).mkString.replaceAll("<|>","").split("\n").length
      indexShortName -> indexCount
    })
    counts.sortBy(_._1).foreach(x => {
      println(f"${x._1}%-50s : ${x._2}")
    })
    println(f"${"Total"}%-50s : ${counts.toMap.values.sum}")
  }

  def main(args: Array[String]): Unit = {
    var workingDir = "/home/vvasuki/stardict-sanskrit/"
    compressAllDicts(List(workingDir), workingDir + "all_dicts.tar.gz")
    //    makeTars("https://github.com/sanskrit-coders/stardict-telugu/raw/master/en-head/tars", dir)
    //    getStats
  }
}
