package sanskritnlp.dictionary

import java.io.{File, PrintWriter}
import org.slf4j.LoggerFactory

import java.nio.file.{Files, Paths}
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.io.{BufferedSource, Source}

class BabylonDictionary(nameIn: String, sourceIn: String = "", headLanguage: String) extends Iterator[(Seq[String], String)] {
  var wordToLocations: HashMap[String, ListBuffer[Int]] = new HashMap[String, ListBuffer[Int]]
  var wordToMeanings = new HashMap[String, ListBuffer[String]]
  val log = LoggerFactory.getLogger(this.getClass)

  val dictName = nameIn

  var wordsTaken = 0

  var fileLocation = sourceIn
  var linesIter: Iterator[String] = null
  var src: Source = null

  def readFile(infileStr: String=fileLocation) = {
    // log info s"Reading $infileStr for $dict_name"
    fileLocation = infileStr
    wordsTaken = 0
    src = Source.fromFile(infileStr, "utf8")
    def isHeadLine(x:String) = x.startsWith("#") || x.trim.isEmpty
    linesIter = src.getLines.dropWhile(isHeadLine)
  }

  override def hasNext(): Boolean = {
    return linesIter.hasNext
  }

  //  java.nio.charset.MalformedInputException: Input length = 1 implies bad character in file.
  override def next(): (Seq[String], String) = {
    wordsTaken = wordsTaken + 1
    val headwordsLine = linesIter.next()
    if (headwordsLine.trim.isEmpty) {
      return (Seq(), null)
    }
    val headwords = headwordsLine.split('|').toSeq
//    log debug(headwords.mkString("|"))
//    log debug(linesIter.hasNext.toString)
    if (!linesIter.hasNext) {
      return (headwords, null)
    }
    val entry = linesIter.next
//    log debug(entry)
    val returnTuple = (headwords, entry)
    assert(!linesIter.hasNext || linesIter.next() == "")
    return returnTuple
  }


  def makeWordToLocationMap(headword_pattern: String = ".+") = {
    log info s"Making wordToLocationMap for $dictName"
    readFile()
    while (hasNext()) {
      val (headwords, meaning) = next()
      // log.info(s"word_index : $word_index")
      val filtered_headwords = headwords.filter(_ matches headword_pattern)
      filtered_headwords.foreach(word => {
        val locus_list = wordToLocations.getOrElse(word, ListBuffer[Int]())
        locus_list += wordsTaken
        wordToLocations += (word -> locus_list)
      })
    }
    readFile(fileLocation)
  }

  def getMeaningAtIndex(locus: Int): String = {
//    log info(s"locus: $locus")
//    log info(s"words_taken: $words_taken")
    readFile()
    take(locus - 1)
    val (_, meaning_line) = next()
    return meaning_line
  }

  def getMeanings(word: String): ListBuffer[String] = {
    if (wordToMeanings.size == 0) {
      if (wordToLocations.size == 0) {
        makeWordToLocationMap()
      }
      val definition_locus_list = wordToLocations.getOrElse(word, ListBuffer[Int]())
      return definition_locus_list.map(getMeaningAtIndex(_))
    } else {
      return wordToMeanings.getOrElse(word, null)
    }
  }

  // Assumes that you've called makeWordToMeaningsMap.
  def getWords: List[String] = {
    if (wordToMeanings.size == 0) {
      return wordToLocations.keys.toList.sorted
    } else {
      return wordToMeanings.keys.toList.sorted
    }

  }

  def makeWordToMeaningsMap(headwordPattern: String = ".+"): Unit = {
    readFile()
    if (wordToMeanings.size > 0) {
      log info (s"Not overwriting wordToMeaning map for $dictName")
      return
    }
    log info s"Making wordToMeanings for $dictName"
    import scala.util.control.Breaks._

    breakable {
      while (hasNext()) {
        val (headwords, meaning) = next()
        if (headwords.size == 0 || meaning == null) {
          log warn (s"Found an illegal entry - $headwords and meaning: $meaning")
          break()
        }
        // log.info(s"word_index : $word_index")
        val filteredHeadwords = headwords.filter(_ matches headwordPattern)
        filteredHeadwords.foreach(word => {
          val meaningList = wordToMeanings.getOrElse(word, ListBuffer[String]())
          meaningList += meaning
          wordToMeanings += (word -> meaningList)
        })
      }
    }
  }

  def dumpPerHeadwordMarkdownFiles(headwordPattern: String = ".+", destPath: String, prefixPathDepth: Int = 4, entrySeparator: String ="----------") = {
    log info s"Creating markdown files at $destPath from $fileLocation"
    makeWordToMeaningsMap(headwordPattern = headwordPattern)
    wordToMeanings.foreach {case (word, meaningList) =>
      val filePath = mdTools.getFilePath(destPath=destPath, prefixPathDepth=prefixPathDepth, word=word)
      Files.createDirectories(Paths.get(filePath.toString).getParent)
      val writer = new PrintWriter(filePath)
      writer.println(word)
      writer.println(entrySeparator)
      meaningList.foreach(meaning => {
        writer.println(meaning)
        writer.println(entrySeparator)
      })
      writer.close()
    }
    val indexFile = new File(destPath, "_index.md")
    Files.createDirectories(Paths.get(indexFile.toString).getParent)
    val writer = new PrintWriter(indexFile)
    writer.println(s"dictName=${dictName}\n\nstringsDefined = ${wordToMeanings.size}")
    writer.close()
    
  }
  
  override def toString(): String = s"$nameIn : $fileLocation"
}

object babylonDictTest {
  val log = LoggerFactory.getLogger(this.getClass)

  def kalpadruma_test: Unit = {
    val kalpadruma = new BabylonDictionary(nameIn = "कल्पद्रुमः", sourceIn = "http://www.sanskrit-lexicon.uni-koeln.de/scans/csldoc/contrib/index.html", headLanguage = "sa")
    kalpadruma.readFile(infileStr = "/home/vvasuki/stardict-sanskrit/sa-head/kalpadruma-sa/kalpadruma-sa.babylon_final")
    log info kalpadruma.getMeanings("इ").mkString("\n\n")
    log info kalpadruma.getMeanings("अ").mkString("\n\n")
    log info kalpadruma.getMeanings("उ").mkString("\n\n")
    log info kalpadruma.getMeanings("इ").mkString("\n\n")
    log info kalpadruma.getMeanings("अ").mkString("\n\n")
  }
  def main(args: Array[String]) {

  }
}