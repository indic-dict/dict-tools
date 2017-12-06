package stardict_sanskrit

import java.io.{File, PrintWriter}

import com.davidthomasbernal.stardict.Dictionary

object stardictProcessor extends BatchProcessor{
  override def getMatchingDictionaries(file_pattern: String, baseDir: String = "."): List[DictionaryFolder] = {
    val dictionaries = super.getMatchingDictionaries(file_pattern, baseDir).filter(_.ifoFile.nonEmpty)
    log info (s"Got ${dictionaries.length} stardict ifo files")
    return dictionaries
  }

  def decompileToBabylon(file_pattern: String, baseDir: String = ".") = {
    val dictionaries = getMatchingDictionaries(file_pattern, baseDir)
    dictionaries.foreach(dict => {
      try {
        val stardict = Dictionary.fromIfo(dict.ifoFile.get.getAbsolutePath, true)
        val stardictIterator = stardict.getIterator
        val babylonFile = new File(dict.dirFile.getAbsolutePath, s"${dict.dirName}.babylon")
        val destination = new PrintWriter(babylonFile)
        while(stardictIterator.hasNext) {
          val dictEntry = stardictIterator.next()
          destination.println(dictEntry.words.toArray().mkString("|"))
          destination.println(dictEntry.definition)
          destination.println()
        }
        destination.close()
      } catch {
        case e: Exception => {
          log.error(e.toString)
          e.printStackTrace()
        }
      }
    })
  }

  def testApi = {
    val ifoFile = "/home/vvasuki/stardict-test/babylon-test/babylon-test.ifo"
    val dict = Dictionary.fromIfo(ifoFile, true)
    log info dict.getWords.toArray().mkString("|")
    log info dict.getDefinitions("babytest_entry2_syn2").toArray().mkString(" ")
  }

  def main(args: Array[String]): Unit = {
    // decompileToBabylon(file_pattern = "Declension.*", baseDir = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa")
  }
}
