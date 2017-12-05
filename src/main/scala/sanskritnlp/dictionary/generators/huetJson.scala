package sanskritnlp.dictionary.generators

import java.io.{File, PrintWriter}

import com.davidthomasbernal.stardict.Dictionary
import stardict_sanskrit.stardictProcessor.log

object huetJson {
  def makeKrdantaDict(): Unit = {
    val ifoFile = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/grammar-heritage_du_sanskrit_san-san/grammar-heritage_du_sanskrit_san-san.ifo"
    val babylonFile = new File("/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/huetKrdanta/huetKrdantaRoots.txt")
    try {
      val stardict = Dictionary.fromIfo(ifoFile, true)
      val stardictIterator = stardict.getIterator
      babylonFile.getParentFile.mkdirs()
      val destination = new PrintWriter(babylonFile)
      while(stardictIterator.hasNext) {
        val dictEntry = stardictIterator.next()
        destination.println(dictEntry.words.toArray().mkString("|"))
      }
      destination.close()
    } catch {
      case e: Exception => //noinspection RedundantBlock
      {
        log.error(e.toString)
        e.printStackTrace()
      }
    }
  }

  def main(args: Array[String]): Unit = {
    makeKrdantaDict()
  }
}
