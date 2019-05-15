package stardict_sanskrit

import org.slf4j.LoggerFactory

/**
  * Example invocation:
  * java -jar bin/artifacts/dict-tools.jar install --destinationPath=/home/vvasuki/sanskrit-coders/stardict-dicts-installed/ --dictRepoIndexUrl=https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md
  */
case class InstallAruments()
case class CommandConfig(mode: Option[String]=None,
                         destinationPath: Option[String]=None, 
                         dictRepoIndexUrl: Option[String]=None,
                         dictPattern: Option[String]=None,
                         urlBase: Option[String]=None,
                         babylonBinary: Option[String]=None,
                         inputPaths: Option[String]=None,
                         githubToken: Option[String]=None,
                         overwrite: Option[Boolean]=None,
                        )

object commandInterface {
  private val log = LoggerFactory.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[CommandConfig]("cli") {
      cmd("install")
        .action((_, c) => c.copy(mode = Some("install")))
        .text("Install all dicts.")
        .children(
          opt[String]("destinationPath")
            .action((x, c) => c.copy(destinationPath = Some(x)))
            .required(),
          opt[String]("dictRepoIndexUrl")
            .action((x, c) => c.copy(dictRepoIndexUrl = Some(x)))
            .required(),
          opt[Boolean]("overwrite")
            .action((x, c) => c.copy(overwrite = Some(x))),
        )
      cmd("addStandardHeadwords")
        .action((_, c) => c.copy(mode = Some("addStandardHeadwords")))
        .text("Add optitrans and devanAgarI headwords to babylon dictionaries.")
        .children(
          opt[String]("dictPattern")
            .action((x, c) => c.copy(dictPattern = Some(x)))
            .required(),
          opt[Boolean]("overwrite")
            .action((x, c) => c.copy(overwrite = Some(x))),
        )
      cmd("makeTars")
        .action((_, c) => c.copy(mode = Some("makeTars")))
        .text("makeTars.")
        .children(
          opt[String]("urlBase")
            .action((x, c) => c.copy(urlBase = Some(x)))
            .required(),
          opt[String]("dictPattern")
            .action((x, c) => c.copy(dictPattern = Some(x)))
            .required(),
          opt[Boolean]("overwrite")
            .action((x, c) => c.copy(overwrite = Some(x))),
        )
      cmd("makeStardict")
        .action((_, c) => c.copy(mode = Some("makeStardict")))
        .text("makeStardict.")
        .children(
          opt[String]("babylonBinary")
            .action((x, c) => c.copy(babylonBinary = Some(x)))
            .required(),
          opt[String]("dictPattern")
            .action((x, c) => c.copy(dictPattern = Some(x)))
            .required(),
          opt[Boolean]("overwrite")
            .action((x, c) => c.copy(overwrite = Some(x))),
        )
      cmd("writeTarsList")
        .action((_, c) => c.copy(mode = Some("writeTarsList")))
        .text("writeTarsList.")
        .children(
          opt[String]("destinationPath")
            .action((x, c) => c.copy(destinationPath = Some(x)))
            .required(),
          opt[String]("urlBase")
            .action((x, c) => c.copy(urlBase = Some(x)))
            .required(),
          opt[Boolean]("overwrite")
            .action((x, c) => c.copy(overwrite = Some(x))),
        )
      cmd("compressAllDicts")
        .action((_, c) => c.copy(mode = Some("compressAllDicts")))
        .text("writeTarsList.")
        .children(
          opt[String]("destinationPath")
            .action((x, c) => c.copy(destinationPath = Some(x)))
            .required(),
          opt[String]("inputPaths")
            .action((x, c) => c.copy(inputPaths = Some(x)))
            .required(),
          opt[Boolean]("overwrite")
            .action((x, c) => c.copy(overwrite = Some(x))),
        )
    }
    parser.parse(args, CommandConfig()) match {
      case Some(commandConfig) => {
        log.debug(commandConfig.toString)
        commandConfig.mode.get match {
          case "install" => installer.install(destination = commandConfig.destinationPath.get, indexOfIndicesUrl = commandConfig.dictRepoIndexUrl.get, overwrite = commandConfig.overwrite.getOrElse(false))
          case "addStandardHeadwords" => babylonProcessor.addOptitrans(dictPattern = commandConfig.dictPattern.get)
          case "makeTars" => tarProcessor.makeTars(urlBase =commandConfig.urlBase.get, dictPattern = commandConfig.dictPattern.get)
          case "compressAllDicts" => tarProcessor.compressAllDicts(basePaths = commandConfig.inputPaths.get.split(","), tarFilePath =  commandConfig.destinationPath.get)
          case "makeStardict" => babylonProcessor.makeStardict(dictPattern = commandConfig.dictPattern.get, babylonBinary =  commandConfig.babylonBinary.get)
          case "writeTarsList" => tarProcessor.writeTarsList(tarDestination =  commandConfig.destinationPath.get, urlBase =  commandConfig.urlBase.get)
          case unknownCommand => log.error(s"Do not recognize $unknownCommand")
        }
      }
      case None =>
        log.error("Failed to parse args")
    }
    log.info(args.mkString(" "))
  }
}
