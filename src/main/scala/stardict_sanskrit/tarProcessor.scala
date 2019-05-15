package stardict_sanskrit

import java.io.{File, PrintWriter}
import java.nio.file.{Path, Paths}

import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source
import scala.sys.process._


object tarProcessor extends BatchProcessor {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val filePatternToTar = ".*\\.ifo|.*\\.idx|.*\\.dz|.*\\.ifo|.*\\.syn|.*LICENSE.*"

  def writeTarsList(tarDestination: String, urlBase: String): Unit = {
    val outFileObj = new File(tarDestination + "/tars.MD")
    outFileObj.getParentFile.mkdirs
    val destination = new PrintWriter(outFileObj)
    val urlBaseFinal = urlBase.replaceAll("/$", "")
    outFileObj.getParentFile.listFiles().map(_.getCanonicalFile).filter(_.getName.endsWith("tar.gz")).toList.sorted.foreach(x => {
      destination.println(s"${urlBaseFinal}/${x.getName.replaceAll(".*/", "")}")
    })
    destination.close()
  }

  def makeTars(urlBase: String, dictPattern: String = ".*"): Unit = {
    log info "=======================makeTars"
    // Get timestamp.
    var dictionaries = getMatchingDictionaries(dictPattern).filter(_.ifoFile.isDefined)
    log info s"Got ${dictionaries.count(_.tarFile.isDefined)} tar files"
    log info s"Got ${dictionaries.count(x => x.ifoFile.isDefined && x.tarFile.isEmpty)}  dicts without tarFile files but with ifo file."

    // Remove excess and outdated tar files.
    if (dictPattern == ".*" && dictionaries.nonEmpty) {
      val tarDirFile = dictionaries.head.getTarDirFile
      if (tarDirFile.exists()) {
        val excessTarFiles = tarDirFile.listFiles.filterNot(x => {
          val name = x.getName
          name != "tars.MD" && dictionaries.filter(_.tarFile.isDefined).map(_.tarFile.get.getCanonicalPath).contains(x.getCanonicalPath)
        })
        log warn s"Removing ${excessTarFiles.length} excessTarFiles"
        excessTarFiles.foreach(_.delete())
        writeTarsList(dictionaries.head.getTarDirFile.getCanonicalPath, urlBase)
      }
    }
    var dictsToIgnore = dictionaries.filter(_.tarFileNewerThanIfo())
    if (dictsToIgnore.nonEmpty) {
      log warn s"Ignoring these files, whose dict files seem updated: " + dictsToIgnore.mkString("\n")
    }
    dictionaries = dictionaries.filterNot(_.tarFileNewerThanIfo())


    log info s"got ${dictionaries.length} dictionaries which need to be updated."
    dictionaries.foreach(_.makeTar(filePatternToTar))

    if (dictionaries.nonEmpty) {
      writeTarsList(dictionaries.head.getTarDirFile.getCanonicalPath, urlBase)
    }
  }

  def compressAllDicts(basePaths: Seq[String], tarFilePath: String): Int = {
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

  import org.apache.commons.compress.archivers.ArchiveException
  import org.apache.commons.compress.archivers.ArchiveInputStream
  import org.apache.commons.compress.compressors.CompressorException
  import java.io.BufferedInputStream
  import java.io.FileInputStream
  import java.io.FileNotFoundException
  import org.apache.commons.compress.archivers.ArchiveStreamFactory

  private val archiveStreamFactory = new ArchiveStreamFactory

  import org.apache.commons.compress.compressors.CompressorStreamFactory
  private val compressorStreamFactory = new CompressorStreamFactory(true /*equivalent to setDecompressConcatenated*/)

  @throws[FileNotFoundException]
  @throws[CompressorException]
  @throws[ArchiveException]
  private def inputStreamFromArchive(sourceFile: String) = { // To handle "IllegalArgumentException: Mark is not supported", we wrap with a BufferedInputStream
    // as suggested in http://apache-commons.680414.n4.nabble.com/Compress-Reading-archives-within-archives-td746866.html
    archiveStreamFactory.createArchiveInputStream(new BufferedInputStream(compressorStreamFactory.createCompressorInputStream(new BufferedInputStream(new FileInputStream(sourceFile)))))
  }

  def extractFile(archiveFileName: String, destinationPath: String): Unit = {
    import org.apache.commons.compress.archivers.ArchiveInputStream
    var archiveInputStream = inputStreamFromArchive(archiveFileName)
    import org.apache.commons.compress.archivers.ArchiveEntry
    import org.apache.commons.compress.archivers.ArchiveEntry
    import org.apache.commons.compress.utils.IOUtils
    import java.io.IOException
    import java.io.OutputStream
    import java.nio.file.Files
    var entry = archiveInputStream.getNextEntry
    while ( entry != null) {
      if (!archiveInputStream.canReadEntryData(entry)) { // log something?
        log.warn("Cannot read next entry!")
      } else {
        val f = new File(Paths.get(destinationPath, entry.getName).toString)
        if (entry.isDirectory) {
          if (!f.isDirectory && !f.mkdirs) throw new IOException("failed to create directory " + f)
        } else {
            val parent = f.getParentFile
            if (!parent.isDirectory && !parent.mkdirs) throw new IOException("failed to create directory " + parent)
            val o = Files.newOutputStream(f.toPath)
            try
              IOUtils.copy(archiveInputStream, o)
            finally if (o != null) o.close()
          }
        }
      entry = archiveInputStream.getNextEntry
      }
    }


  //noinspection AccessorLikeMethodIsUnit
  def getStats(): Unit = {
    val indexIndexorum = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md"
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
    var workingDir = "/home/vvasuki/stardict-sanskrit/"
    compressAllDicts(List(workingDir), workingDir + "all_dicts.tar.gz")
    //    makeTars("https://github.com/sanskrit-coders/stardict-telugu/raw/master/en-head/tars", dir)
    //    getStats
  }
}
