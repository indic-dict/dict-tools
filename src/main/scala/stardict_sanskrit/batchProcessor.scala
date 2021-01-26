package stardict_sanskrit

import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.dictionary.babylonTools

object batchProcessor extends BatchProcessor {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
    *
    * @param dictPattern
    * @param babylonBinary
    * @param tarBaseUrl - example value https://github.com/indic-dict/stardict-marathi/raw/gh-pages/ma-head/other-entries/tars
    * @param githubToken
    */
  def makeIndicStardictTar(dictPattern: String = ".*", babylonBinary: String, tarBaseUrl: String, githubToken: Option[String]=None, overwrite: Boolean = false, baseDir: String = ".") = {
    var dictionaries = this.getMatchingDictionaries(dictPattern, baseDir = baseDir)
    val githubRepo=GithubRepo.fromUrl(url=tarBaseUrl, githubToken=githubToken)
    log info "=======================Full build from source to stardict tar."
    dictionaries.foreach(dictionary => {
      if (dictionary.babylonFile.isDefined) {
        val tarFileMatchesBabylon = dictionary.gitDictFileMatchesSource(githubRepo=githubRepo, outputType="tar")
        if (!tarFileMatchesBabylon || overwrite) {
          if (!dictionary.name.contains("spokensanskrit")) {
            babylonTools.addStandardHeadwords(infileStr = dictionary.babylonFile.get.getAbsolutePath)
          }
          dictionary.makeStardictFromBabylonFile(babylonBinary)
          dictionary.makeTar(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.babylonFile.get.getAbsolutePath))
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "tar").getCanonicalPath, urlBase=tarBaseUrl, ext = "tar.gz")
          dictionary.delete_large_intermediate_files()
        } else {
          log info(s"Tar file for ${dictionary.name} is not outdated. Not overwriting.")
          githubRepo.downloadFileByPrefix(fileName = dictionary.name, dirPath = dictionary.getOutputDirFile("tar").getAbsolutePath)
          // Tarlist may be out of sync and is anyway non-binary (so git git will not be updated in case same content is generated), so will overwrite anyway.
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "tar").getCanonicalPath, urlBase=tarBaseUrl, ext = "tar.gz")
        }
      } else {
        log.info(s"**** No babylon file in ${dictionary.dirName} - skipping.")
        if (dictionary.stardictFolder.ifoFile.isDefined) {
          val tarFileMatchesIfo = dictionary.gitDictFileMatchesSource(sourceFile = dictionary.stardictFolder.ifoFile.get, githubRepo=githubRepo, outputType = "tar")
          if (!tarFileMatchesIfo || overwrite) {
            dictionary.makeTar(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.stardictFolder.ifoFile.get.getAbsolutePath))
          } else {
            log info(s"Tar file for ${dictionary.name} is not outdated. Not overwriting.")
            githubRepo.downloadFileByPrefix(dictionary.name, dirPath = dictionary.getOutputDirFile("tar").getAbsolutePath)
          }
          // Tarlist may be out of sync and is anyway non-binary (so git git will not be updated in case same content is generated), so will overwrite anyway.
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "tar").getCanonicalPath, urlBase=tarBaseUrl, ext = "tar.gz")
        } else {
          log.info(s"**** No babylon or ifo file in ${dictionary.dirName} - skipping.")
        }
      }
    })

  }

  def makeSlobs(dictPattern: String = ".*", baseUrl: String, githubToken: Option[String]=None, overwrite: Boolean = false, baseDir: String = "."): Unit = {
    val dictionaries = this.getMatchingDictionaries(dictPattern, baseDir = baseDir)
    val githubRepo=GithubRepo.fromUrl(url=baseUrl, githubToken=githubToken)
    log info "=======================Full build from source to stardict tar."
    dictionaries.foreach(dictionary => {
      if (dictionary.babylonFile.isDefined) {
        val slobFileMatchesBabylon = dictionary.gitDictFileMatchesSource(githubRepo = githubRepo, outputType = "slob")
        if (!slobFileMatchesBabylon || overwrite) {
          if (!dictionary.name.contains("spokensanskrit") && !dictionary.babylonFinalFileNewerThanBabylon()) {
//            babylonTools.addStandardHeadwords(infileStr = dictionary.babylonFile.get.getAbsolutePath)
          }
          dictionary.makeSlobFromBabylonFile(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.babylonFile.get.getAbsolutePath))
        } else {
          log info (s"Slob file for ${dictionary.name} is not outdated. Not overwriting.")
          githubRepo.downloadFileByPrefix(dictionary.name, dirPath = dictionary.getOutputDirFile("slob").getAbsolutePath)
          tarProcessor.writeFilesListMd(mdPath = dictionary.getOutputListFile(outputType = "slob").getCanonicalPath, urlBase = baseUrl, ext = "slob")
        }
      }
    })
  }


  
  def main(args: Array[String]): Unit = {
    val dictPattern = ".*"
    var workingDir = "/home/vvasuki/indic-dict/stardict-sinhala/si-head/en-entries"
//    makeIndicStardictTar(dictPattern = ".*", babylonBinary = "stardict-babylon", tarBaseUrl = "https://github.com/indic-dict/stardict-sinhala/raw/gh-pages/si-head/en-entries/tars", githubToken = None, overwrite = false, baseDir = workingDir)
    makeSlobs(dictPattern = ".*", baseUrl = "https://github.com/indic-dict/stardict-sinhala/raw/gh-pages/si-head/en-entries/tars", githubToken = None, overwrite = false, baseDir = workingDir)
    
  }
}
