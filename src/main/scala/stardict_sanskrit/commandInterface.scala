package stardict_sanskrit

import org.slf4j.LoggerFactory

/**
  * Example invocation:
  * java -jar bin/artifacts/dict-tools.jar install /home/vvasuki/sanskrit-coders/stardict-dicts-installed/ https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md
  */
object commandInterface {
  private val log = LoggerFactory.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    assert(args.length > 0)
    val command = args(0)
    log.info(args.mkString(" "))
    command match {
      case "install" => installer.install(destination = args(1), indexOfIndicesUrl = args(2))
      case "addOptitrans" => babylonProcessor.addOptitrans(file_pattern = args(1).replace("DICTS=", ""))
      case "makeTars" => tarProcessor.makeTars(urlBase = args(1), file_pattern = args(2).replace("DICTS=", ""))
      case "compressAllDicts" => tarProcessor.compressAllDicts(basePaths = Seq(args(1)), tarFilePath = args(2))
      case "makeStardict" => babylonProcessor.makeStardict(file_pattern = args(1).replace("DICTS=", ""), babylon_binary = args(2))
      case "writeTarsList" => tarProcessor.writeTarsList(tarDestination = args(1), urlBase = args(2))
      case unknownCommand => log.error(s"Do not recognize $unknownCommand")
    }
  }
}
