package stardict_sanskrit

import java.io.File

import org.slf4j.{Logger, LoggerFactory}

import scala.sys.process._

class DictionaryFolder(val name: String) {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  var dirName: String = _
  var dirFile: File = _
  var babylonFile: Option[File] = None
  var babylonFinalFile: Option[File] = None
  var ifoFile: Option[File] = None
  var tarFile: Option[File] = None
  var dictFile: Option[File] = None
  var dictdzFile: Option[File] = None

  def this(dirFileIn: java.io.File ) = {
    this(dirFileIn.getName)
    dirFile = dirFileIn
    dirName = dirFile.getName.replaceAll(".*/", "")
    babylonFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.babylon"))
    babylonFinalFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.babylon_final"))

    if (getTarDirFile.exists) {
      tarFile = getTarDirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.*.tar.gz"))
    }
    ifoFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.ifo"))
    dictFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.dict"))
    dictdzFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.dict.dz"))
    log debug toString
  }


  def babylonFinalFileNewerThanBabylon(): Boolean = {
    babylonFinalFile.isDefined && (babylonFinalFile.get.lastModified > babylonFile.get.lastModified)
  }

  def tarFileNewerThanIfo(): Boolean = {
    tarFile.isDefined && (tarFile.get.lastModified > ifoFile.get.lastModified)
  }


  //noinspection AccessorLikeMethodIsEmptyParen
  def getFinalBabylonFile(): File = {
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
    ifoFile.isDefined && (ifoFile.get.lastModified > babFile.lastModified)
  }

  //noinspection AccessorLikeMethodIsEmptyParen
  def getBabylonOrIfoTimestampString(): String = {
    // Format: dhAtupATha-sa__2016-02-20_16-15-35.tar.gz
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    if (getFinalBabylonFile != null) {
      format.format(getFinalBabylonFile().lastModified)
    } else {
      format.format(ifoFile.get.lastModified)
    }
  }

  def makeStardictFromBabylonFile(babylon_binary: String): AnyVal = {
    val babFile = getFinalBabylonFile
    log info (f"Making stardict from: ${babFile.getCanonicalPath}")
    s"$babylon_binary ${babFile.getCanonicalPath}".!
    dictFile = dirFile.listFiles.map(_.getCanonicalFile).filter(_.getName.matches(s".*/?${dirName}.dict")).headOption
    if (dictFile.nonEmpty) {
      s"dictzip ${dictFile.get.getCanonicalPath}".!
    }
  }

  def getExpectedTarFileName(sizeMbString: String = "unk"): String = s"${dirName}__${getBabylonOrIfoTimestampString}__${sizeMbString}MB.tar.gz"
  def getTarDirFile = new File(dirFile.getParentFile.getCanonicalPath, "/tars")

  def tarFileMatchesBabylon(): Boolean = {
    tarFile.isDefined && tarFile.get.getName.matches(s".*/?${getExpectedTarFileName(sizeMbString = ".*")}")
  }

  def makeTar(filePatternToTar: String) = {
    if (tarFile.isDefined) {
      log info "Deleting " + tarFile.get.getAbsolutePath
      tarFile.get.delete()
    }
    val targetTarFile = new File(getTarDirFile.getCanonicalPath, getExpectedTarFileName())
    targetTarFile.getParentFile.mkdirs
    val filesToCompress = dirFile.listFiles.map(_.getCanonicalPath).filter(x => x.matches(filePatternToTar))
    val command = s"tar --transform s/.*\\///g -czf ${targetTarFile.getCanonicalPath} ${filesToCompress.mkString(" ")}"
    log info command
    command.!

    // Add size hint.
    val sizeMbString = (targetTarFile.length()/(1024*1024)).toLong.toString
    val fileWithSize = new File(getTarDirFile.getCanonicalPath, getExpectedTarFileName(sizeMbString = sizeMbString))
    val renameResult = targetTarFile.renameTo(fileWithSize)
    if (!renameResult) {
      log warn s"Renamed ${targetTarFile} to ${fileWithSize}: $renameResult"
    } else {
      log info s"Renamed ${targetTarFile} to ${fileWithSize}: $renameResult"
    }
  }

  override def toString: String =
    s"${dirFile.getName} with ${babylonFile} babylon, ${babylonFinalFile} babylonFinal, ${ifoFile} ifo, ${tarFile} tar "
}








