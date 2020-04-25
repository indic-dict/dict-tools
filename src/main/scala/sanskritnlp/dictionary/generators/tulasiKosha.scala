package sanskritnlp.dictionary.generators

import java.io.{File, PrintWriter}

import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.{Logger, LoggerFactory}

object tulasiKosha {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  def scsvToJsonBabylon(): Unit = {
    implicit val formats: Formats = DefaultFormats
    val infileStr = "/home/vvasuki/sanskrit/raw_etexts/koshaH/tulasi_shabda_kosha/tulsi_shabda_kosh_part_01_0001-0023.pdf.txt"
    val outfileStr = "/home/vvasuki/stardict-hindi/hi-head/tulsi_shabda_kosh/tulsi_shabda_kosh.babylon"
    val src = utils.tsvToMapIterator(infileStr)
    val outFileObj = new File(outfileStr)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)
    // TODO: Read colon separated value files, create dictioary, dump as babylon.
  }

  def main(args: Array[String]): Unit = {
    scsvToJsonBabylon()
  }
}
