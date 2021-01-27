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
  var slobFile: Option[File] = None

  def this(dirFileIn: java.io.File, entryFilesDirIn: java.io.File = null) = {
    this(dirFileIn.getName)
    dirFile = dirFileIn
    stardictFolder = new StardictFolder(dirFileIn=dirFileIn)
    entryFilesDir = entryFilesDirIn
    dirName = dirFile.getName.replaceAll(".*/", "")
    babylonFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.babylon"))
    babylonFinalFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.babylon_final"))

    if (getOutputDirFile("tar").exists) {
      tarFile = getOutputDirFile("tar").listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.*.tar.gz"))
    }
    if (getOutputDirFile("slob").exists) {
      tarFile = getOutputDirFile("slob").listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*/?${dirName}.*.slob"))
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
  

  def gitDictFileMatchesSource(githubRepo: GithubRepo, sourceFile: File=babylonFile.get, sourceFileBranch: Option[String]=None, outputType: String): Boolean = {
    val babylonUpdateTimestamp = githubRepo.getGithubUpdateTime(filePath = sourceFile.getAbsolutePath, branch = sourceFileBranch)
    if (babylonUpdateTimestamp.isDefined) {
      val gitFileTimestamp = githubRepo.getFileNameTimestampFromGithub(fileName = this.name, dirPath = this.getOutputDirFile(outputType = outputType).getAbsolutePath)
      log debug(s"babylon: ${babylonUpdateTimestamp}, git file: ${gitFileTimestamp}")
      return babylonUpdateTimestamp == gitFileTimestamp
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

  def makeSlobFromBabylonFile(timestamp: Option[String] = None): AnyVal = {
    val babFile = getFinalBabylonFile
    val slobFile = new File(getOutputDirFile("slob").getCanonicalPath, getExpectedFinalFileName(ext = "slob"))
    log info (f"Making slob from: ${babFile.getCanonicalPath} to ${slobFile.getCanonicalPath}")
//    TODO: this command is not working. No output. No error
//    val commandSeq = Seq("python", "-c", s"""'from dict_curation import babylon; babylon.to_slob("${babFile.getCanonicalPath}", "${slobFile.getCanonicalPath}")'""")

    // The below fails with RuntimeError: iterating over a reader while it's not open
    // pyglossary  --read-format BabylonBgl --utf8-check  /home/vvasuki/indic-dict/stardict-sinhala/si-head/en-entries/carter/carter.babylon /home/vvasuki/indic-dict/stardict-sinhala/si-head/en-entries/slobs/carter__2018-03-22_03-14-55__unkMB.slob   
    val commandSeq = Seq("pyglossary", "--read-format", "bgl", babFile.getCanonicalPath, slobFile.getCanonicalPath)
    log debug(commandSeq.toString())
    val (status, stdout, stderr) = Utils.runCommandSeqLimitOutput(commandSeq)
    log info ("command status: \n" + status)
    log info ("stdout excerpt: \n" + stdout)
    log info ("stderr excerpt: \n" + stderr)
    setSizeHint(fileObj = slobFile, timestamp = timestamp)
  }


  def makeSlobFromStardict(timestamp: Option[String] = None): AnyVal = {
    val slobFile = new File(getOutputDirFile("slob").getCanonicalPath, getExpectedFinalFileName(ext = "slob"))
    log info (f"Making slob from: ${stardictFolder.ifoFile.get.getCanonicalPath} to ${slobFile.getCanonicalPath}")
    val commandSeq = Seq("pyglossary", stardictFolder.ifoFile.get.getCanonicalPath, slobFile.getCanonicalPath)
    log debug(commandSeq.toString())
    val (status, stdout, stderr) = Utils.runCommandSeqLimitOutput(commandSeq)
    log info ("command status: \n" + status)
    log info ("stdout excerpt: \n" + stdout)
    log info ("stderr excerpt: \n" + stderr)
    setSizeHint(fileObj = slobFile, timestamp = timestamp)
  }

  def delete_large_intermediate_files(): Unit = {
    val babFile = getFinalBabylonFile
    if (babFile.getName.endsWith("babylon_final") && babFile.length() > 99000000) {
      log warn (s"Deleted large final babylon file $babFile of size ${babFile.length()} with result ${babFile.delete()}")
    } else {
      log info (s"Keeping final babylon file $babFile of size ${babFile.length()}")
    }
  }


  def getExpectedFinalFileName(sizeMbString: String = "unk", timestamp: Option[String]= None, ext: String): String = s"${dirName}__${timestamp.getOrElse(getLocalBabylonOrIfoTimestampString)}__${sizeMbString}MB.${ext}"

  def getTarDirFile = getOutputDirFile(outputType = "tar")

  def getOutputDirFile(outputType: String) = outputType match {
    case _ => new File(dirFile.getParentFile.getCanonicalPath, s"/${outputType}s")
  }

  def getOutputListFile(outputType: String) = outputType match {
    case _ => new File(this.getOutputDirFile(outputType = outputType).getCanonicalPath, s"/${outputType}s.MD")
  }

  def tarFileMatchesBabylon(): Boolean = {
    tarFile.isDefined && tarFile.get.getName.matches(s".*/?${getExpectedFinalFileName(sizeMbString = ".*", ext = "tar.gz")}")
  }

  def makeTar(filePatternToTar: String=tarProcessor.filePatternToTar, timestamp: Option[String] = None) = {
    if (getTarDirFile.exists()) {
      getTarDirFile.listFiles.map(_.getCanonicalFile).filter(_.getName.matches(s".*/?${dirName}.*.tar.gz")).foreach(x => {
        log info "Deleting " + x.getAbsolutePath
        tarFile.get.delete()
      })
    }
    val targetTarFile = new File(getTarDirFile.getCanonicalPath, getExpectedFinalFileName(ext = "tar.gz"))
    targetTarFile.getParentFile.mkdirs
    val filesToCompress = dirFile.listFiles.map(_.getCanonicalPath).filter(x => x.matches(filePatternToTar))
    val command = s"tar --transform s/.*\\///g -czf ${targetTarFile.getCanonicalPath} ${filesToCompress.mkString(" ")}"
    log info command
    command.!
    setSizeHint(fileObj = targetTarFile, timestamp = timestamp)

  }

  def setSizeHint(fileObj: File, timestamp: Option[String] = None): Unit = {
    if (!fileObj.exists()) {
      return 
    }
    // Add size hint.
    val sizeMbString = (fileObj.length()/(1024*1024)).toLong.toString
    var ext = fileObj.getName.split("\\.").last
    if (fileObj.getName.contains(".tar.gz")) {
      ext = "tar.gz"
    }
    val fileWithSize = new File(fileObj.getParentFile.getCanonicalPath, getExpectedFinalFileName(sizeMbString = sizeMbString, timestamp=timestamp, ext = ext))
    val renameResult = fileObj.renameTo(fileWithSize)
    if (!renameResult) {
      log warn s"Renamed ${fileObj} to ${fileWithSize}: $renameResult"
    } else {
      log info s"Renamed ${fileObj} to ${fileWithSize}: $renameResult"
    }  
  }
  
  override def toString: String =
    s"${dirFile.getName} with ${babylonFile} babylon, ${babylonFinalFile} babylonFinal, ${stardictFolder.ifoFile} ifo, ${tarFile} tar, ${slobFile} slob "
}
