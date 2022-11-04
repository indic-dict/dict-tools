package stardict_sanskrit

import org.slf4j.LoggerFactory

/**
  * Example invocation:
  * java -jar bin/artifacts/dict-tools.jar install --destinationPath=/opt/dicts/indic-dict/stardict/ --dictRepoIndexUrl=https://raw.githubusercontent.com/indic-dict/stardict-index/master/dictionaryIndices.md
  */
case class CommandConfig(mode: Option[String]=None,
                         destinationPath: Option[String]=None, 
                         dictRepoIndexUrl: Option[String]=None,
                         dictPattern: Option[String]=None,
                         urlBase: Option[String]=None,
                         babylonBinary: Option[String]=None,
                         sourcePath: Option[String]=None,
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
          opt[String]("dictPattern")
            .action((x, c) => c.copy(dictPattern = Some(x))).required().withFallback(() => ".*"),
          opt[Boolean]("overwrite")
            .action((x, c) => c.copy(overwrite = Some(x))),
        )
      cmd("makeIndicStardictTar")
        .action((_, c) => c.copy(mode = Some("makeIndicStardictTar")))
        .text("makeTars.")
        .children(
          opt[String]("urlBase")
            .action((x, c) => c.copy(urlBase = Some(x)))
            .required(),
          opt[String]("dictPattern")
            .action((x, c) => c.copy(dictPattern = Some(x)))
            .required(),
          opt[String]("babylonBinary")
            .action((x, c) => c.copy(babylonBinary = Some(x)))
            .required(),
          opt[String]("githubToken")
            .action((x, c) => c.copy(githubToken = if (x != "NONE") Some(x) else None)),
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
      cmd("makePerHeadwordMdFiles")
        .action((_, c) => c.copy(mode = Some("makePerHeadwordMdFiles")))
        .text("Make per-entry files.")
        .children(
          opt[String]("destinationPath")
            .action((x, c) => c.copy(destinationPath = Some(x)))
            .required(),
          opt[String]("sourcePath")
            .action((x, c) => c.copy(sourcePath = Some(x)))
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
    log.info(args.mkString(" "))
    parser.parse(args, CommandConfig()) match {
      case Some(commandConfig) => 
        log.debug(commandConfig.toString)
        commandConfig.mode.get match {
          case "install" => installer.install(destination = commandConfig.destinationPath.get, indexOfIndicesUrl = commandConfig.dictRepoIndexUrl.get, dictFilterRegex=commandConfig.dictPattern.getOrElse(".*"), overwrite = commandConfig.overwrite.getOrElse(false))
          case "makeIndicStardictTar" => batchProcessor.makeIndicStardictTar(dictPattern = commandConfig.dictPattern.get, babylonBinary =  commandConfig.babylonBinary.get, overwrite = commandConfig.overwrite.getOrElse(false), tarBaseUrl =  commandConfig.urlBase.get, githubToken = commandConfig.githubToken)
            batchProcessor.makeSlobs(dictPattern = commandConfig.dictPattern.get, overwrite = commandConfig.overwrite.getOrElse(false), baseUrl =  commandConfig.urlBase.get, githubToken = commandConfig.githubToken, babylonBinary = commandConfig.babylonBinary.get)
            // The below result in too slow pushes leading to failed builds like https://github.com/indic-dict/stardict-sanskrit/runs/2511181643.
//            batchProcessor.makePerHeadwordMdFiles(dictPattern = commandConfig.dictPattern.get, tarBaseUrl =  commandConfig.urlBase.get, githubToken = commandConfig.githubToken)
          case "makePerHeadwordMdFiles" => batchProcessor.makePerHeadwordMdFiles(sourceDir = commandConfig.sourcePath.get, destDir = commandConfig.destinationPath.get)
          case unknownCommand => log.error(s"Do not recognize $unknownCommand")
        } // successful parse case ends
      case _ =>
        log.error("Failed to parse args")
        throw new IllegalArgumentException
    }
    log.info("Done with : " + args.mkString(" "))
  }
}
