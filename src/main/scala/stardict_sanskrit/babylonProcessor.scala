package stardict_sanskrit

import java.io.{File, PrintWriter}

import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.dictionary.{BabylonDictionary, babylonTools}
import sanskritnlp.transliteration.roman.iast
import sanskritnlp.transliteration.transliterator
import sanskritnlp.vyAkaraNa.devanAgarI

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Processes babylon files en masse in various ways.
  * 
  * Also see babylonTools object.
  */
object babylonProcessor extends BatchProcessor{
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)


  private def getRecursiveListOfFinalBabylonDicts(basePaths: Seq[String]): Seq[BabylonDictionary] = {
    val babylonFiles = getRecursiveSetOfDictDirs(basePaths=basePaths).map(_.getFinalBabylonFile).filterNot(_ == null)
    val babylonDicts = babylonFiles.map(x => {
      val dict = new BabylonDictionary(name_in = x.getName, head_language = "")
      dict.fromFile(x.getCanonicalPath)
      dict
    })
    log info s"Got ${babylonDicts.size} babylon files. And they are: \n${babylonDicts.mkString("\n")}"
    babylonDicts.toList.sortBy(_.fileLocation)
  }

  def mapWordToDicts(dictList: Seq[BabylonDictionary], headword_pattern: String): mutable.HashMap[String, ListBuffer[BabylonDictionary]] = {
    val wordToDicts = new mutable.HashMap[String, ListBuffer[BabylonDictionary]]()
    dictList.foreach(dictionary => {
      // dictionary.makeWordToLocationMap(headword_pattern = "\\p{IsDevanagari}+")
      dictionary.makeWordToMeaningsMap(headword_pattern)
      dictionary.getWords.foreach(word => {
        var dictList = wordToDicts.getOrElse(word, ListBuffer[BabylonDictionary]())
        dictList += dictionary
        wordToDicts += (word -> dictList)
      })
    })
    wordToDicts
  }

  def getWordToDictsMapFromPaths(basePaths: Seq[String], wordPattern: String = "(\\p{IsDevanagari})+"): mutable.HashMap[String, ListBuffer[BabylonDictionary]] = {
    val babylonDicts = getRecursiveListOfFinalBabylonDicts(basePaths = basePaths)
    val wordToDicts = mapWordToDicts(dictList=babylonDicts, headword_pattern=wordPattern)
    log info s"Got ${wordToDicts.size} words"
    return wordToDicts
  }

  def dumpWordToDictMap(basePaths: Seq[String], wordPattern: String = "(\\p{IsDevanagari})+", outFilePath: String): Unit = {
    val words = getWordToDictsMapFromPaths(basePaths, wordPattern)
    log info s"Got ${words.size} words"
    log info s"Dumping to ${outFilePath} "
    val outFileObj = new File(outFilePath)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)
    words.keys.toList.sorted.foreach(word => {
      val dictNames = words(word).map(_.dict_name)
      destination.println(s"$word\t${dictNames.mkString(",")}")
    })
    destination.close()
  }
  
  
  /**
    * An ad-hoc test/ processing entry point.
    * @param args
    */
  def main(args: Array[String]): Unit = {
    val dictPattern = ".*"
    val workingDirInit = System.getProperty("user.dir")
    var workingDir = "/home/vvasuki/indic-dict/stardict-pali/pali-en-head"
    System.setProperty("user.dir", workingDir)
//    dumpWordToDictMap(basePaths=List(workingDir), outFilePath=s"${workingDir}wordlists/words_sa_dev.txt")
    // stripNonOptitransHeadwords(dictPattern, workingDir)
    // getDevanagariOptitransFromIast(dictPattern, workingDir)
//    getDevanagariOptitransFromIastIfIndic(dictPattern, workingDir, getWordToDictsMapFromPaths(List("/home/vvasuki/stardict-pali/pali-head/").keys))
//    addOptitrans(dictPattern = "Me.*", baseDir = "/home/vvasuki/indic-dict/stardict-sanskrit/sa-head/en-entries")
    // makeStardict(dir, "/home/vvasuki/stardict/tools/src/babylon")
  }
}
