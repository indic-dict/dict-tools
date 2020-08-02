package stardict_sanskrit

import java.io.File

import org.slf4j.{Logger, LoggerFactory}
import sanskrit_coders.Utils
import sanskritnlp.dictionary.StardictFolder
import stardict_sanskrit.babylonProcessor.log

import scala.sys.process._

/**
  * Every dictionary has a folder with several files, of which the babylon and stardict files are particularly notable. This class represents such a dictionary folder.
  * 
  * @param name
  */
class DictionaryFolder(val name: String) {

  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  var dirName: String = _
  var dirFile: File = _
  var entryFilesDir: File = _
  var babylonFile: Option[File] = None
  var babylonFinalFile: Option[File] = None
  var stardictFolder: StardictFolder = null
  var tarFile: Option[File] = None

  def this(dirFileIn: java.io.File, entryFilesDirIn: java.io.File = null) = {
    this(dirFileIn.getName)
    dirFile = dirFileIn
    stardictFolder = new StardictFolder(dirFileIn=dirFileIn)
    entryFilesDir = entryFilesDirIn
    dirName = dirFile.getName.replaceAll(".*/", "")
    babylonFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.babylon"))
    babylonFinalFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.babylon_final"))

    if (getTarDirFile.exists) {
      tarFile = getTarDirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.*.tar.gz"))
    }
    log debug toString
  }

  def entryFilesNewerThanBabylon(): Boolean = {
    if (entryFilesDir == null) {
      false
    } else {
      val firstEntryFile = entryFilesDir.listFiles().headOption
      firstEntryFile.isDefined && firstEntryFile.get.lastModified > babylonFile.get.lastModified
    }
  }

  def babylonFinalFileNewerThanBabylon(): Boolean = {
    babylonFinalFile.isDefined && (babylonFinalFile.get.lastModified > babylonFile.get.lastModified)
  }

  def tarFileNewerThanIfo(): Boolean = {
    tarFile.isDefined && (tarFile.get.lastModified > stardictFolder.ifoFile.get.lastModified)
  }


  //noinspection AccessorLikeMethodIsEmptyParen
  def getFinalBabylonFile(): File = {
    babylonFinalFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.babylon_final"))
    if (babylonFinalFile.isDefined) {
      babylonFinalFile.get
    } else if (babylonFile.isDefined) {
      babylonFile.get
    } else {
      null
    }
  }

  def ifoFileNewerThanBabylon(): Boolean = {
    val babFile = getFinalBabylonFile()
    stardictFolder.ifoFile.isDefined && (stardictFolder.ifoFile.get.lastModified > babFile.lastModified)
  }
  

  def tarFileMatchesSource(githubRepo: GithubRepo, sourceFile: File=babylonFile.get,  sourceFileBranch: Option[String]=None, tarFileBranch: Option[String]=None): Boolean = {
    val babylonUpdateTimestamp = githubRepo.getGithubUpdateTime(filePath = sourceFile.getAbsolutePath, branch = sourceFileBranch)
    if (babylonUpdateTimestamp.isDefined) {
      val tarFileTimestamp = githubRepo.getTarFileNameTimestampFromGithub(dictionaryFolder = this)
      log debug(s"babylon: ${babylonUpdateTimestamp}, tar: ${tarFileTimestamp}")
      return babylonUpdateTimestamp == tarFileTimestamp
    } else {
      return false
    }
  }

  //noinspection AccessorLikeMethodIsEmptyParen
  def getLocalBabylonOrIfoTimestampString(): String = {
    // Format: dhAtupATha-sa__2016-02-20_16-15-35.tar.gz
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    if (getFinalBabylonFile != null) {
      format.format(getFinalBabylonFile().lastModified)
    } else {
      format.format(stardictFolder.ifoFile.get.lastModified)
    }
  }

  def makeEntryFilesFromBabylonFile(): AnyVal = {
    val babFile = getFinalBabylonFile
    log info (f"Making entry files from: ${babFile.getCanonicalPath}")
    
  }

  def makeStardictFromBabylonFile(babylon_binary: String): AnyVal = {
    val babFile = getFinalBabylonFile
    log info (f"Making stardict from: ${babFile.getCanonicalPath}")
    val (status, stdout, stderr) = Utils.runCommandLimitOutput(s"$babylon_binary ${babFile.getCanonicalPath}")
    log info ("stdout excerpt: \n" + stdout)
    log info ("stderr excerpt: \n" + stderr)
    stardictFolder.dictFile = dirFile.listFiles.map(_.getCanonicalFile).filter(_.getName.matches(s".*/?${dirName}.dict")).headOption
    if (stardictFolder.dictFile.nonEmpty) {
      s"dictzip ${stardictFolder.dictFile.get.getCanonicalPath}".!
    }
  }


  def delete_large_intermediate_files(): Unit = {
    val babFile = getFinalBabylonFile
    if (babFile.getName.endsWith("final_babylon") && babFile.length() > 99000000) {
      log warn (s"Deleted large final babylon file $babFile of size ${babFile.length()} with result ${babFile.delete()}")
    } else {
      log info (s"Keeping final babylon file $babFile of size ${babFile.length()}")
    }
  }


  def getExpectedTarFileName(sizeMbString: String = "unk", timestamp: Option[String]= None): String = s"${dirName}__${timestamp.getOrElse(getLocalBabylonOrIfoTimestampString)}__${sizeMbString}MB.tar.gz"
  def getTarDirFile = new File(dirFile.getParentFile.getCanonicalPath, "/tars")

  def tarFileMatchesBabylon(): Boolean = {
    tarFile.isDefined && tarFile.get.getName.matches(s".*/?${getExpectedTarFileName(sizeMbString = ".*")}")
  }

  def makeTar(filePatternToTar: String=tarProcessor.filePatternToTar, timestamp: Option[String] = None) = {
    if (getTarDirFile.exists()) {
      getTarDirFile.listFiles.map(_.getCanonicalFile).filter(_.getName.matches(s".*/?${dirName}.*.tar.gz")).foreach(x => {
        log info "Deleting " + x.getAbsolutePath
        tarFile.get.delete()
      })
    }
    val targetTarFile = new File(getTarDirFile.getCanonicalPath, getExpectedTarFileName())
    targetTarFile.getParentFile.mkdirs
    val filesToCompress = dirFile.listFiles.map(_.getCanonicalPath).filter(x => x.matches(filePatternToTar))
    val command = s"tar --transform s/.*\\///g -czf ${targetTarFile.getCanonicalPath} ${filesToCompress.mkString(" ")}"
    log info command
    command.!

    // Add size hint.
    val sizeMbString = (targetTarFile.length()/(1024*1024)).toLong.toString
    val fileWithSize = new File(getTarDirFile.getCanonicalPath, getExpectedTarFileName(sizeMbString = sizeMbString, timestamp=timestamp))
    val renameResult = targetTarFile.renameTo(fileWithSize)
    if (!renameResult) {
      log warn s"Renamed ${targetTarFile} to ${fileWithSize}: $renameResult"
    } else {
      log info s"Renamed ${targetTarFile} to ${fileWithSize}: $renameResult"
    }
  }

  override def toString: String =
    s"${dirFile.getName} with ${babylonFile} babylon, ${babylonFinalFile} babylonFinal, ${stardictFolder.ifoFile} ifo, ${tarFile} tar "
}








