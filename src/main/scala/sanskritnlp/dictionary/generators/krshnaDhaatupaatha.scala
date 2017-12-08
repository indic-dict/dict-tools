package sanskritnlp.dictionary.generators

import java.io.{File, PrintWriter}

import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.{Logger, LoggerFactory}

object krshnaDhaatupaatha {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  def tsvToJsonBabylon(): Unit = {
    implicit val formats: Formats = DefaultFormats
    val infileStr = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/dhAtu-pATha-kRShNamAchArya/mUlam/dhAtu-pATha-kRShNamAchArya.tsv"
    val outfileStr = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/dhAtu-pATha-kRShNamAchArya/pdhAtu-pATha-kRShNamAchArya.babylon"
    val src = utils.tsvToMapIterator(infileStr)
    val outFileObj = new File(outfileStr)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)

    src.map(valueMap=> valueMap.mapValues(_.split("/").map(_.trim)))
      .foreach(valueMap => {
      val headers = valueMap.filterKeys(key => Set("धातुः", "मूलधातुः", "धात्वर्थः", "रूपम्").contains(key)).values.flatten.filter(!_.isEmpty).toList.distinct
      val headersLine = headers.mkString("|")
      val meaningLine =  Serialization.writePretty(valueMap).replace("\n", "<BR>")
      destination.println(headersLine)
      destination.println(meaningLine)
      destination.println("")
      // println(line)
      println(headersLine)
      println(meaningLine)
    })
    destination.close()
    println("")
  }

  def main(args: Array[String]): Unit = {
    tsvToJsonBabylon()
  }
}
