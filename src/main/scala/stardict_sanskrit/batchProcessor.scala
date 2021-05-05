package stardict_sanskrit

import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}
import sanskrit_coders.Utils
import sanskritnlp.dictionary.{BabylonDictionary, babylonTools}

import java.io.File
import scala.io.Source

object batchProcessor extends BatchProcessor {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
   *
   * @param dictPattern
   * @param babylonBinary
   * @param tarBaseUrl - example value https://github.com/indic-dict/stardict-marathi/raw/gh-pages/ma-head/other-entries/tars
   * @param githubToken
   */
  def makeIndicStardictTar(dictPattern: String = ".*", babylonBinary: String, tarBaseUrl: String, githubToken: Option[String] = None, overwrite: Boolean = false, baseDir: String = ".") = {
    var dictionaries = this.getMatchingDictionaries(dictPattern, baseDir = baseDir)
    val githubRepo = GithubRepo.fromUrl(url = tarBaseUrl, githubToken = githubToken)
    log info "=======================Full build from source to stardict tar."
    dictionaries.foreach(dictionary => {
      log info (s"Want to make tar file for ${dictionary.name}.")

      if (dictionary.babylonFile.isDefined) {
        val tarFileMatchesBabylon = dictionary.gitDictFileMatchesSource(githubRepo = githubRepo, outputType = "tar")
        if (!tarFileMatchesBabylon || overwrite) {
          if (!dictionary.name.contains("spokensanskrit")) {
            babylonTools.addStandardHeadwords(infileStr = dictionary.babylonFile.get.getAbsolutePath)
          }
          dictionary.makeStardictFromBabylonFile(babylonBinary)
          dictionary.makeTar(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.babylonFile.get.getAbsolutePath))
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "tar").getCanonicalPath, urlBase = tarBaseUrl, ext = "tar.gz")
          dictionary.delete_large_intermediate_files()
        } else {
          log info (s"Tar file for ${dictionary.name} is not outdated. Not overwriting.")
          githubRepo.downloadFileByPrefix(fileName = dictionary.name, dirPath = dictionary.getOutputDirFile("tar").getAbsolutePath)
          // Tarlist may be out of sync and is anyway non-binary (so git git will not be updated in case same content is generated), so will overwrite anyway.
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "tar").getCanonicalPath, urlBase = tarBaseUrl, ext = "tar.gz")
        }
      } else {
        log.info(s"**** No babylon file in ${dictionary.dirName} - skipping.")
        if (dictionary.stardictFolder.ifoFile.isDefined) {
          val tarFileMatchesIfo = dictionary.gitDictFileMatchesSource(sourceFile = dictionary.stardictFolder.ifoFile.get, githubRepo = githubRepo, outputType = "tar")
          if (!tarFileMatchesIfo || overwrite) {
            dictionary.makeTar(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.stardictFolder.ifoFile.get.getAbsolutePath))
          } else {
            log info (s"Tar file for ${dictionary.name} is not outdated. Not overwriting.")
            githubRepo.downloadFileByPrefix(dictionary.name, dirPath = dictionary.getOutputDirFile("tar").getAbsolutePath)
          }
          // Tarlist may be out of sync and is anyway non-binary (so git git will not be updated in case same content is generated), so will overwrite anyway.
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "tar").getCanonicalPath, urlBase = tarBaseUrl, ext = "tar.gz")
        } else {
          log.info(s"**** No babylon or ifo file in ${dictionary.dirName} - skipping.")
        }
      }
    })

  }

  def makePerHeadwordMdFiles(dictPattern: String = ".*", tarBaseUrl: String, githubToken: Option[String] = None, overwrite: Boolean = false, baseDir: String = ".") = {
    var dictionaries = this.getMatchingDictionaries(dictPattern, baseDir = baseDir)
    val githubRepo = GithubRepo.fromUrl(url = tarBaseUrl, githubToken = githubToken)
    log info "=======================Full build from source to md files."
    dictionaries.foreach(dictionary => {
      log info (s"Want to make md files for ${dictionary.name}.")

      if (dictionary.babylonFile.isDefined) {
        val destPath = new File(dictionary.getOutputDirFile("md"), dictionary.name)
        val mdFileMatchesBabylon = dictionary.downstreamFileNewerThanSource(githubRepo = githubRepo, sourceFile = dictionary.babylonFile.get, destFilePath = new File(destPath, "_index.md"))
        if (!mdFileMatchesBabylon || overwrite) {
          log info (s"MD file for ${dictionary.name} is outdated.")
        } else {
          log info (s"MD file for ${dictionary.name} is not outdated. But regenerating anyway.")
        }
        val babylonDictionary = new BabylonDictionary(nameIn = dictionary.name, sourceIn = dictionary.babylonFile.get.toString, headLanguage = "UNK")
        babylonDictionary.dumpPerHeadwordMarkdownFiles(destPath = destPath.toString)
      } else {
        log.info(s"**** No babylon file in ${dictionary.dirName} - skipping.")
      }
    })

  }


  def hasPyglossary: Boolean = {
    try {
      val (status, stdout, stderr) = Utils.runCommandLimitOutput("pyglossary --help")
      if (status != 0) {
        return false
      }
    } catch {
      case _ => return false
    } 
    return true
  }

  /** Create slob files (for use with aard) from stardict and babylon files.
   * 
   * @param dictPattern
   * @param babylonBinary (used to make stardict files if needed).
   * @param baseUrl
   * @param githubToken
   * @param overwrite
   * @param baseDir
   */
  def makeSlobs(dictPattern: String = ".*", babylonBinary: String, baseUrl: String, githubToken: Option[String] = None, overwrite: Boolean = false, baseDir: String = "."): Unit = {
    if (!hasPyglossary) {
      log warn("pyglossary not installed. Returning")
      return 
    }
    val dictionaries = this.getMatchingDictionaries(dictPattern, baseDir = baseDir)
    val githubRepo = GithubRepo.fromUrl(url = baseUrl, githubToken = githubToken)
    log info "=======================Full build from source to slob."
    dictionaries.foreach(dictionary => {
      var makeSlob = true
      log info (s"Want to make slob file for ${dictionary.name}.")
      log debug dictionary.toString
      if (dictionary.babylonFile.isDefined) {
        val slobFileMatchesBabylon = dictionary.gitDictFileMatchesSource(sourceFile = dictionary.babylonFile.get, githubRepo = githubRepo, outputType = "slob")
        if (slobFileMatchesBabylon && !overwrite) {
          makeSlob = false
          log info (s"Slob file for ${dictionary.name} is not outdated. Not overwriting.")
          githubRepo.downloadFileByPrefix(dictionary.name, dirPath = dictionary.getOutputDirFile("slob").getAbsolutePath)
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "slob").getCanonicalPath, urlBase = baseUrl.replaceAll("/tars", "/slobs"), ext = "slob")
        }
      } else {
        if (!dictionary.stardictFolder.ifoFile.isDefined) {
          makeSlob = false
          log info(s"No babylon. No ifo. No slob for ${dictionary.name}.")
        }
      }
      if (!makeSlob) {
        log info(s"Not making a new slob for  ${dictionary.name}.")
      } else{
        if (dictionary.babylonFile.isDefined && dictionary.stardictFolder.ifoFile.isEmpty) {
          dictionary.makeStardictFromBabylonFile(babylon_binary = babylonBinary)
        }
        if (dictionary.stardictFolder.ifoFile.isDefined) {
          log info s"Stardict to slob for  ${dictionary.name}."
          var timestamp: Option[String] = None
          if (dictionary.babylonFile.isDefined) {
            timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.babylonFile.get.getAbsolutePath)
          }
          dictionary.makeSlobFromStardict(timestamp = timestamp)
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "slob").getCanonicalPath, urlBase = baseUrl.replaceAll("/tars", "/slobs"), ext = "slob")
        }
      }
    })
  }


  def main(args: Array[String]): Unit = {
    val configFileContents = Source.fromResource("conf.local").mkString
    var config = ConfigFactory.parseString(configFileContents)
    val dictPattern = ".*"
    def vyAkaraNaTest(): Unit = {
      var workingDir = "/home/vvasuki/indic-dict/stardict-sanskrit-vyAkaraNa"
      makeIndicStardictTar(dictPattern = ".*", babylonBinary = "stardict-babylon", tarBaseUrl = "https://github.com/indic-dict/stardict-sanskrit-vyAkaraNa/raw/gh-pages/si-head/en-entries/tars", githubToken = None, overwrite = false, baseDir = workingDir)
    }
//    vyAkaraNaTest()
    
    def sinhalaTest(): Unit = {
      var workingDir = "/home/vvasuki/indic-dict/stardict-sinhala/si-head/en-entries"
      makeIndicStardictTar(dictPattern = ".*", babylonBinary = "stardict-babylon", tarBaseUrl = "https://github.com/indic-dict/stardict-sinhala/raw/gh-pages/si-head/en-entries/tars", githubToken = None, overwrite = true, baseDir = workingDir)
//      makeSlobs(dictPattern = ".*", "stardict-babylon", baseUrl = "https://github.com/indic-dict/stardict-sinhala/raw/gh-pages/si-head/en-entries/tars", githubToken = Some(config.getString("github_token")), overwrite = false, baseDir = workingDir)
    }
//    sinhalaTest()

    def sanskritTest(from: String="sa", to: String="en"): Unit = {
      var workingDir = s"/home/vvasuki/indic-dict/stardict-sanskrit/${from}-head/${to}-entries/"
//      makeIndicStardictTar(dictPattern = ".*", babylonBinary = "stardict-babylon", tarBaseUrl = s"https://github.com/indic-dict/stardict-sanskrit/raw/gh-pages/${from}-head/${to}-entries/tars", githubToken = None, overwrite = true, baseDir = workingDir)
      makeSlobs(dictPattern = ".*", babylonBinary = "stardict-babylon", baseUrl = s"https://github.com/indic-dict/stardict-sanskrit/raw/gh-pages/${from}-head/${to}-entries/tars", githubToken = None, overwrite = true, baseDir = workingDir)
    }
    sanskritTest(from = "sa", to = "other-indic")
  }
}
