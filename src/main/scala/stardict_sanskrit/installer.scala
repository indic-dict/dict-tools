package stardict_sanskrit

import akka.pattern.ask
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Status}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, IOResult}
import org.slf4j.{Logger, LoggerFactory}
import sanskrit_coders.{RichHttpAkkaClient, Utils}
import akka.http.scaladsl.model._
import akka.util.Timeout
import de.heikoseeberger.akkahttpjson4s.Json4sSupport.ShouldWritePretty.False
import sys.process._
import java.net.URL
import java.io.File

import scala.concurrent.Future
import scala.util.{Failure, Success}


case class DictInfo(dictTarUrl: String, destinationFolder: String, var dictName: String = null, var tarFilename : String = null, var timestamp: Long = 0) {
  tarFilename = dictTarUrl.split("/").last
  val tarFilenameParts = tarFilename.split("__")
  dictName = tarFilenameParts.head
  if (tarFilenameParts.length > 1) {
    // example: 2020-05-20_15-13-48Z
    val timestampStr = tarFilenameParts(1).replace("Z", "")
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    timestamp = format.parse(timestampStr).getTime()
  }
}


case class DictIndex(indexUrl: String, downloadPathPrefix: String, var downloadPath: String = "", var dictTarUrls: List[String] = List()) {
  downloadPath = Paths.get(downloadPathPrefix, indexUrl.replaceAllLiterally("https://raw.githubusercontent.com/", "").replaceAllLiterally("master/", "").replaceAllLiterally("tars/tars.MD", "")).toString
  dictTarUrls = DictIndex.getUrlsFromIndexMd(url=indexUrl).toList

  val dictionaries: List[DictInfo] = dictTarUrls.map(dictTarUrl => DictInfo(dictTarUrl=dictTarUrl, destinationFolder = downloadPath))
//
//  override def toString: String = s"indexUrl: ${indexUrl}\ndownloadPath: ${downloadPath}\ndictTarUrls: ${dictTarUrls.mkString("\n")}"
}

object DictIndex {
  def getUrlsFromIndexMd(url: String): Array[String] = {
    import scala.io.Source
    Source.fromURL(url).mkString.split("\n").map(_.replaceAll("[<>]", ""))
  }
}


class InstallerActor extends Actor with ActorLogging {
  import context.dispatcher // Provides ExecutionContext - required below.
  import akka.pattern.pipe // For pipeTo() below.

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private val simpleClient: HttpRequest => Future[HttpResponse] = Http(context.system).singleRequest(_: HttpRequest)
  private val redirectingClient: HttpRequest => Future[HttpResponse] = RichHttpAkkaClient.httpClientWithRedirect(simpleClient)(context.system)

  def receive: PartialFunction[Any, Unit] = {
    case (dict: DictInfo, overwrite: Boolean) => {
      log.info(dict.toString)
      val destinationTarPath = Paths.get(dict.destinationFolder, dict.dictName, dict.tarFilename)
      val dictionaryFolder = new java.io.File(destinationTarPath.getParent.toString)
      val doesDictExist = dictionaryFolder.exists() && dictionaryFolder.listFiles().length > 3 && dictionaryFolder.listFiles().find(_.getName.endsWith("ifo")) != None
      val localDictFileNewer = doesDictExist && dictionaryFolder.listFiles().filter(_.getName.endsWith("ifo")).head.lastModified() > dict.timestamp
      if (!overwrite && !localDictFileNewer) {
        log.warning(s"Skipping pre-existing seemingly newer $dict")
        Future.fromTry(Success(s"Dict already exists: $dict")).pipeTo(sender())
      } else {
        // Download the file.
        new java.io.File(destinationTarPath.getParent.toString).mkdirs()
        log.info(s"Downloading ${dict.dictTarUrl}")
        val downloadAndExtractFuture = RichHttpAkkaClient.dumpToFile(dict.dictTarUrl, destinationTarPath.toString)(context.system).map(result => {
          if (result.wasSuccessful) {
            tarProcessor.extractFile(archiveFileName = destinationTarPath.toString, destinationPath = destinationTarPath.getParent.toString)
            new java.io.File(destinationTarPath.toString).delete()
            Success(s"Done with $dict")
          } else {
            throw result.getError
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

  def install(destination: String, indexOfIndicesUrl: String, overwrite:Boolean=false): Unit = {
    val indices = DictIndex.getUrlsFromIndexMd(indexOfIndicesUrl)
    val dictionaries = indices.map(new DictIndex(_, downloadPathPrefix = destination)).flatMap(index => index.dictionaries)

    val installerActorRef = system.actorOf(Props[InstallerActor], "installerActor")
    // Actor ask timeout
    implicit val timeout: Timeout = Timeout(10, TimeUnit.MINUTES)
    import scala.concurrent.ExecutionContext.Implicits.global

    //    log.debug(indices.mkString(","))
    val resultFutureList = dictionaries.map( dictionary =>  ask(installerActorRef, (dictionary, overwrite)) )
    val futureOfResults: Unit = Utils.getFutureOfTrys(resultFutureList). onComplete {
      case Success(resultList) =>
        log.info(resultList.mkString("\n"))
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
    install(destination = "/home/vvasuki/indic-dict/stardict-dicts-installed/", indexOfIndicesUrl = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md", overwrite=false)
  }
}
