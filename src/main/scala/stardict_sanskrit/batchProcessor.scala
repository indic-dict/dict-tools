package stardict_sanskrit

import org.slf4j.{Logger, LoggerFactory}

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
    var dictionaries = this.getMatchingDictionaries(dictPattern)
    val githubRepo=GithubRepo.fromUrl(url=tarBaseUrl, githubToken=githubToken)
    log info "=======================Full build from source to stardict tar."
    dictionaries.foreach(dictionary => {
      if (dictionary.babylonFile.isDefined) {
        val tarFileMatchesBabylon = dictionary.tarFileMatchesSource(githubRepo=githubRepo)
        if (!tarFileMatchesBabylon || overwrite) {
          babylonProcessor.addOptitrans(dictPattern = dictionary.dirName, overwrite = overwrite)
          dictionary.makeStardictFromBabylonFile(babylonBinary)
          dictionary.makeTar(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.babylonFile.get.getAbsolutePath))
          tarProcessor.writeTarsList(tarDestination = dictionary.getTarDirFile.getCanonicalPath, urlBase=tarBaseUrl)
        } else {
          log info(s"Tar file for ${dictionary.name} is not outdated. Not overwriting.")
          githubRepo.downloadTarFile(dictionary)
        }
      } else {
        log.info(s"**** No babylon file in ${dictionary.dirName} - skipping.")
        if (dictionary.ifoFile.isDefined) {
          val tarFileMatchesIfo = dictionary.tarFileMatchesSource(sourceFile = dictionary.ifoFile.get, githubRepo=githubRepo)
          if (!tarFileMatchesIfo || overwrite) {
            dictionary.makeTar(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.ifoFile.get.getAbsolutePath))
            tarProcessor.writeTarsList(tarDestination = dictionary.getTarDirFile.getCanonicalPath, urlBase=tarBaseUrl)
          } else {
            log info(s"Tar file for ${dictionary.name} is not outdated. Not overwriting.")
            githubRepo.downloadTarFile(dictionary)
          }
        } else {
          log.info(s"**** No babylon or ifo file in ${dictionary.dirName} - skipping.")
        }
      }
    })

  }

  def main(args: Array[String]): Unit = {
    val dictPattern = ".*"
    var workingDir = "/home/vvasuki/indic-dict/stardict-pali/pali-en-head"
    makeIndicStardictTar(dictPattern = ".*", babylonBinary = "stardict-babylon", tarBaseUrl = "https://github.com/indic-dict/stardict-pali/raw/gh-pages/pali-en-head/tars", githubToken = None, overwrite = false, baseDir = workingDir)
    
  }
}
