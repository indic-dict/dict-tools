package stardict_sanskrit

import java.io.{File, PrintWriter, StringWriter}

import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.dictionary.{BabylonDictionary, babylonTools}
import sanskritnlp.transliteration.{iast, transliterator}
import sanskritnlp.vyAkaraNa.devanAgarI
import stardict_sanskrit.babylonProcessor.{getClass, log}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

object headwordTransformers{
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  def addDevanaagariiFromOtherIndic(headwords_original:Array[String]) = (headwords_original ++ headwords_original.map(
    x => try {
      transliterator.getDevanagariiFromOtherIndicString(x).getOrElse("")
    } catch {
      case ex: Exception => {
        val sw = new StringWriter
        ex.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        log.error(x)
        ""
      }
    }))

  def addOptitransFromDevanaagarii(headwords_original:Array[String]) = (headwords_original ++ headwords_original.map(
    x => try {
      transliterator.transliterate(x, "dev", "optitrans")
    } catch {
      case ex: Exception => {
        val sw = new StringWriter
        ex.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        log.error(x)
        ""
      }
    }))

  def addNonAnsusvaaraVariantsFromDevanaagarii(headwords_original:Array[String]) = (headwords_original ++ headwords_original.map(
    x => try {
      transliterator.getNonAnusvaaraVariant(x)
    } catch {
      case ex: Exception => {
        val sw = new StringWriter
        ex.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        log.error(x)
        ""
      }
    }))
}

object babylonProcessor extends BatchProcessor{
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  override def getMatchingDictionaries(file_pattern: String, baseDir: String = "."): List[DictionaryFolder] = {
    val dictionaries = super.getMatchingDictionaries(file_pattern, baseDir).filter(_.getFinalBabylonFile != null)
    log info (s"Got ${dictionaries.count(_.babylonFinalFile.isDefined)} babylon_final files")
    log info (s"Got ${dictionaries.count(x => x.babylonFile.isDefined && x.babylonFinalFile.isEmpty)}  dicts without babylon_final files but with babylon file.")
    dictionaries
  }

  def getRecursiveListOfFinalBabylonDicts(basePaths: Seq[String]): Seq[BabylonDictionary] = {
    val babylonFiles = getRecursiveSetOfDictDirs(basePaths=basePaths).map(_.getFinalBabylonFile).filterNot(_ == null)
    val babylonDicts = babylonFiles.map(x => {
      val dict = new BabylonDictionary(name_in = x.getName, head_language = "")
      dict.fromFile(x.getCanonicalPath)
      dict
    })
    log info s"Got ${babylonDicts.size} babylon files. And they are: \n${babylonDicts.mkString("\n")}"
    babylonDicts.toList.sortBy(_.fileLocation)
  }

  def fixHeadwordsInFinalFile(file_pattern: String = ".*", baseDir: String = ".", headwordTransformer: (Array[String]) => Array[String],
                              finalFileExtension: String = ".babylon_final", sort: Boolean = true) = {
    val files_to_ignore = Set("spokensanskrit.babylon")
    var dictionaries = getMatchingDictionaries(file_pattern, baseDir).filter(_.babylonFile.isDefined)
    log info (s"Got ${dictionaries.length} babylon files")
    var dictsToIgnore = dictionaries.filter(_.babylonFinalFileNewerThanBabylon())
    if (dictsToIgnore.nonEmpty) {
      log warn s"Ignoring these files, whose final babylon files seem updated:" + dictsToIgnore.mkString("\n")
    }
    dictionaries = dictionaries.filterNot(_.babylonFinalFileNewerThanBabylon)
    log info (s"Got ${dictionaries.length} babylon files")

    val babylon_files = dictionaries.map(_.babylonFile)

    babylon_files.map(_.get.getCanonicalPath).foreach(file => {
      if (files_to_ignore contains file) {
        log info (f"skipping: $file")
      } else {
        log info (f"Fixing headwords in: $file")
        sanskritnlp.dictionary.babylonTools.fixHeadwords(file, finalFileExtension, headwordTransformer, sort=sort)
      }
    })
    // sys.exit()
  }

  def addOptitrans(file_pattern: String = ".*", baseDir: String = ".") = {
    log info "=======================Adding optitrans headwords, making final babylon file."
    val headwordTransformer = (headwords_original:Array[String]) => (
      headwordTransformers.addOptitransFromDevanaagarii(
        headwordTransformers.addNonAnsusvaaraVariantsFromDevanaagarii(headwordTransformers.addDevanaagariiFromOtherIndic(headwords_original)))
      ).filterNot(_.isEmpty).distinct
    fixHeadwordsInFinalFile(file_pattern=file_pattern, baseDir=baseDir, headwordTransformer=headwordTransformer, sort=false)
  }

  def stripNonOptitransHeadwords(file_pattern: String = ".*", baseDir: String = "."): Unit = {
    log info "=======================stripNonOptitransHeadwords, making final babylon file."
    val headwordTransformer = (headwords_original:Array[String]) => headwords_original.filterNot(iast.isEncoding)
    fixHeadwordsInFinalFile(file_pattern=file_pattern, baseDir=baseDir, headwordTransformer=headwordTransformer)
  }

  def getWordToDictsMapFromPaths(basePaths: Seq[String], wordPattern: String = "(\\p{IsDevanagari})+"): mutable.HashMap[String, ListBuffer[BabylonDictionary]] = {
    val babylonDicts = getRecursiveListOfFinalBabylonDicts(basePaths = basePaths)
    val wordToDicts = babylonTools.mapWordToDicts(dictList=babylonDicts, headword_pattern=wordPattern)
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

  def getDevanagariOptitransFromIast(file_pattern: String = ".*", baseDir: String = ".") = {
    log info "=======================Adding optitrans headwords, making final babylon file."
    val toDevanAgarIAndOptitrans = (headwords_original:Array[String]) => headwords_original.map(
      x => transliterator.transliterate(x, "iast", "dev")) ++ headwords_original.map(
      x => transliterator.transliterate(x, "iast", "optitrans"))
    fixHeadwordsInFinalFile(file_pattern=file_pattern, baseDir=baseDir, headwordTransformer=toDevanAgarIAndOptitrans)
  }

  def getDevanagariOptitransFromIastIfIndic(file_pattern: String = ".*", baseDir: String = ".", indicWordSet: mutable.HashSet[String] = mutable.HashSet[String]()): Unit = {
    log info "=======================Adding optitrans headwords, making final babylon file."
    val indicWordSetDev = indicWordSet.filter(devanAgarI.isEncoding)
    def isIndic(word: String) = indicWordSetDev.contains(iast.fromDevanagari(word)) || iast.isEncoding(word)
    def transliterateIfIndic(x: String, destSchema: String) = if(isIndic(x)) {
      transliterator.transliterate(x, "iast", destSchema)
    } else {
      x
    }
    val toDevanAgarIAndOptitrans = (headwords_original:Array[String]) => headwords_original.map(
      x => transliterateIfIndic(x, "dev")) ++ headwords_original.map(x => transliterateIfIndic(x, "optitrans"))
    fixHeadwordsInFinalFile(file_pattern=file_pattern, baseDir=baseDir, headwordTransformer=toDevanAgarIAndOptitrans)
  }

  def makeStardict(file_pattern: String = ".*", babylon_binary: String) = {
    log info "=======================makeStardict"
    var dictionaries = getMatchingDictionaries(file_pattern)

    var dictsToIgnore = dictionaries.filter(_.ifoFileNewerThanBabylon())
    if (dictsToIgnore.nonEmpty) {
      log warn s"Ignoring these files, whose dict files seem updated: " + dictsToIgnore.mkString("\n")
    }
    dictionaries = dictionaries.filterNot(_.ifoFileNewerThanBabylon())
    dictionaries.foreach(_.makeStardictFromBabylonFile(babylon_binary))
  }

  def transliterateAllIndicToDevanagarI(inFilePath: String, outFilePath: String, sourceScheme:String, wordListFilePath: String ="/home/vvasuki/stardict-sanskrit/wordlists/words_sa_dev.txt") = {
    var wordSet = Source.fromFile(wordListFilePath, "utf8").getLines.map(_.split("\t").headOption.getOrElse("न")).map(transliterator.transliterate(_, transliterator.scriptDevanAgarI, sourceScheme)).toSet
    val outFileObj = new File(outFilePath)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)
    val inFile = Source.fromFile(inFilePath, "utf8").getLines().map(line => {
      val outStr = transliterator.transliterateWordsIfIndic(in_str = line, wordSet=wordSet, sourceScheme=sourceScheme, destScheme = transliterator.scriptDevanAgarI)
      destination.println(outStr)
    })
    destination.close()
  }


  def main(args: Array[String]): Unit = {
    val dictPattern = ".*"
    val workingDirInit = System.getProperty("user.dir")
    var workingDir = "/home/vvasuki/stardict-sanskrit/"
    System.setProperty("user.dir", workingDir)
//    dumpWordToDictMap(basePaths=List(workingDir), outFilePath=s"${workingDir}wordlists/words_sa_dev.txt")
    // stripNonOptitransHeadwords(dictPattern, workingDir)
    // getDevanagariOptitransFromIast(dictPattern, workingDir)
//    getDevanagariOptitransFromIastIfIndic(dictPattern, workingDir, getWordToDictsMapFromPaths(List("/home/vvasuki/stardict-pali/pali-head/").keys))
     addOptitrans(file_pattern = "pu.*", baseDir = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa")
    // makeStardict(dir, "/home/vvasuki/stardict/tools/src/babylon")
  }
}
