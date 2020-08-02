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
        val tarFileMatchesBabylon = dictionary.tarFileMatchesSource(githubRepo=githubRepo)
        if (!tarFileMatchesBabylon || overwrite) {
          babylonTools.addStandardHeadwords(infileStr = dictionary.babylonFile.get.getAbsolutePath)
          dictionary.makeStardictFromBabylonFile(babylonBinary)
          dictionary.makeTar(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.babylonFile.get.getAbsolutePath))
          tarProcessor.writeTarsList(tarDestination = dictionary.getTarDirFile.getCanonicalPath, urlBase=tarBaseUrl)
          dictionary.delete_large_intermediate_files()
        } else {
          log info(s"Tar file for ${dictionary.name} is not outdated. Not overwriting.")
          githubRepo.downloadTarFile(dictionary)
          // Tarlist may be out of sync and is anyway non-binary (so git git will not be updated in case same content is generated), so will overwrite anyway.
          tarProcessor.writeTarsList(tarDestination = dictionary.getTarDirFile.getCanonicalPath, urlBase=tarBaseUrl)
        }
      } else {
        log.info(s"**** No babylon file in ${dictionary.dirName} - skipping.")
        if (dictionary.stardictFolder.ifoFile.isDefined) {
          val tarFileMatchesIfo = dictionary.tarFileMatchesSource(sourceFile = dictionary.stardictFolder.ifoFile.get, githubRepo=githubRepo)
          if (!tarFileMatchesIfo || overwrite) {
            dictionary.makeTar(timestamp = githubRepo.getGithubUpdateTime(filePath = dictionary.stardictFolder.ifoFile.get.getAbsolutePath))
          } else {
            log info(s"Tar file for ${dictionary.name} is not outdated. Not overwriting.")
            githubRepo.downloadTarFile(dictionary)
          }
          // Tarlist may be out of sync and is anyway non-binary (so git git will not be updated in case same content is generated), so will overwrite anyway.
          tarProcessor.writeTarsList(tarDestination = dictionary.getTarDirFile.getCanonicalPath, urlBase=tarBaseUrl)
        } else {
          log.info(s"**** No babylon or ifo file in ${dictionary.dirName} - skipping.")
        }
      }
    })

  }

  def main(args: Array[String]): Unit = {
    val dictPattern = ".*"
    var workingDir = "/home/vvasuki/indic-dict//stardict-sinhala/si-head/en-entries"
    makeIndicStardictTar(dictPattern = ".*", babylonBinary = "stardict-babylon", tarBaseUrl = "https://github.com/indic-dict/stardict-sinhala/raw/gh-pages/si-head/en-entries/tars", githubToken = None, overwrite = false, baseDir = workingDir)
    
  }
}
