package stardict_sanskrit

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.dictionary.StardictTar
import sanskrit_coders.RichHttpAkkaClient
import sanskrit_coders.Utils
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import java.io.{File, FileNotFoundException}
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class DictInfo(dictTarUrl: String, destinationFolder: String, var dictName: String = null, var tarFilename : String = null, var timestamp: Long = 0) {
  tarFilename = dictTarUrl.split("/").last
  val tarFilenameParts = tarFilename.split("__")
  if(tarFilenameParts.length == 1) {
    dictName = tarFilename.split("\\.").head
  } else {
    dictName = tarFilenameParts.head
  }
  if (tarFilenameParts.length > 1) {
    // example: 2020-05-20_15-13-48Z
    val timestampStr = tarFilenameParts(1).replace("Z", "")
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    timestamp = format.parse(timestampStr).getTime()
  }

}


case class DictIndex(indexUrl: String, downloadPathPrefix: String, var downloadPath: String = "", var dictTarUrls: List[String] = List()) {
  downloadPath = Paths.get(downloadPathPrefix, indexUrl.trim.replace("https://raw.githubusercontent.com/", "").replace("master/", "").replace("gh-pages/", "").replaceAll("tars/tars.*\\.MD", "")).toString
  dictTarUrls = DictIndex.getUrlsFromIndexMd(url=indexUrl).toList

  val dictionaries: List[DictInfo] = dictTarUrls.map(dictTarUrl => DictInfo(dictTarUrl=dictTarUrl.trim, destinationFolder = downloadPath))
//
//  override def toString: String = s"indexUrl: ${indexUrl}\ndownloadPath: ${downloadPath}\ndictTarUrls: ${dictTarUrls.mkString("\n")}"
}

object DictIndex {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  def getUrlsFromIndexMd(url: String): Array[String] = {
    import scala.io.Source
    try {
      Source.fromURL(url).mkString.split("\n").filter(x => x.startsWith("<") || x.startsWith("http") || x.startsWith("ftp")).map(_.replaceAll("[<>]", "").trim())
    } catch {
      case e: FileNotFoundException => log.error(s"Could not get ${url}")
        Array()
    }
  }

  def recursiveListFiles(f: File): Array[File] = {
    if(f.exists()) {
      val these = f.listFiles
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    } else {
      Array[File]()
    }
  }
}


class InstallerActor extends Actor with ActorLogging {
  import akka.pattern.pipe
  import context.dispatcher // For pipeTo() below.

  private val simpleClient: HttpRequest => Future[HttpResponse] = Http(context.system).singleRequest(_: HttpRequest)
  private val redirectingClient: HttpRequest => Future[HttpResponse] = RichHttpAkkaClient.httpClientWithRedirect(simpleClient)(context.system)

  def receive: PartialFunction[Any, Unit] = {
    case (dict: DictInfo, dictFilterRegex: String, overwrite: Boolean) => {
      log.info(dict.toString)
      val destinationTarPath = Paths.get(dict.destinationFolder, dict.dictName, dict.tarFilename)
      val dictionaryFolder = new java.io.File(destinationTarPath.getParent.toString)
      val dictFilesLocal = DictIndex.recursiveListFiles(dictionaryFolder)
      val dictIfo = DictIndex.recursiveListFiles(dictionaryFolder).find(_.getName.endsWith("ifo"))
      val doesDictExist = dictionaryFolder.exists() && dictFilesLocal.length >= 3 && dictIfo != None
      val localDictFileNewer = doesDictExist && (dict.timestamp == 0 || dictIfo.head.lastModified() > dict.timestamp)
      if (!overwrite && (localDictFileNewer)) {
        log.warning(s"Skipping pre-existing seemingly newer $dict")
        Future.fromTry(Success(s"Dict already exists: $dict")).pipeTo(sender())
      } else {
        // Download the file.
        new java.io.File(destinationTarPath.getParent.toString).mkdirs()
        log.info(s"Downloading ${dict.dictTarUrl}")
        val downloadAndExtractFuture = RichHttpAkkaClient.dumpToFile(dict.dictTarUrl, destinationTarPath.toString)(context.system).map(result => {
          if (result.wasSuccessful) {
            new StardictTar(filePath=destinationTarPath.toString).extract(destinationPath = destinationTarPath.getParent.toString)
            new java.io.File(destinationTarPath.toString).delete()
            Success(s"Done with $dict")
          } else {
            // In case of download failure, we want to proceed to the next dict.
//            throw result.getError
            Failure(result.getError)
          }
        })
        downloadAndExtractFuture.pipeTo(sender())
      }
    }
  }
}

object installer {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val system = ActorSystem("installerActorSystem")

  def install(destination: String, indexOfIndicesUrl: String, dictFilterRegex:String=".*", overwrite:Boolean=false): Unit = {
    val indices = DictIndex.getUrlsFromIndexMd(indexOfIndicesUrl)
    val dictionaries = indices.map(new DictIndex(_, downloadPathPrefix = destination)).flatMap(index => index.dictionaries).filter(_.dictName.matches(dictFilterRegex))
    val installerActorRef = system.actorOf(Props[InstallerActor], "installerActor")
    // Actor ask timeout
    implicit val timeout: Timeout = Timeout(10, TimeUnit.MINUTES)
    import scala.concurrent.ExecutionContext.Implicits.global

    //    log.debug(indices.mkString(","))
    val resultFutureList = dictionaries.map( dictionary =>  ask(installerActorRef, (dictionary, dictFilterRegex, overwrite)) )
    val futureOfResults: Unit = Utils.getFutureOfTrys(resultFutureList). onComplete {
      case Success(resultList) =>
        log.debug(resultList.mkString("\n"))
        val resultListOverwritten = resultList.filterNot(_.toString.contains("Dict already exists"))
        log.info(resultListOverwritten.mkString("\n"))
        system.terminate()
      case _ => log.error("This branch should not be reached!")
    }
/*
.foreach (someTry => someTry match {
        case Success(value) => log.info("All done!")
        case Failure(exception) => log.error(s"An error occurred: ${exception.getMessage}, with stacktrace:\n${exception.getStackTrace.mkString("\n")}")
      }
 */

  }

  def main(args: Array[String]): Unit = {
    install(destination = "/home/vvasuki/indic-dict/stardict-dicts-installed/", indexOfIndicesUrl = "https://raw.githubusercontent.com/indic-dict/stardict-index/master/dictionaryIndices.md", overwrite=false)
  }
}
