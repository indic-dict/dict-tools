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
  * Processes babylon files in various ways. For example - adds devanagari or opitrans headwords; or produces stardict files.
  * 
  * Major entry points are: fixHeadwordsInFinalFile (and its wrappers like addOptitrans), makeStardict, main.
  */
object babylonProcessor extends BatchProcessor{
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Gets dictionaries matching a certain pattern.
    * 
    * @param file_pattern
    * @param baseDir
    * @return
    */
  override def getMatchingDictionaries(file_pattern: String, baseDir: String = "."): List[DictionaryFolder] = {
    val dictionaries = super.getMatchingDictionaries(file_pattern, baseDir).filter(_.getFinalBabylonFile != null)
    log info (s"Got ${dictionaries.count(_.babylonFinalFile.isDefined)} babylon_final files")
    log info (s"Got ${dictionaries.count(x => x.babylonFile.isDefined && x.babylonFinalFile.isEmpty)}  dicts without babylon_final files but with babylon file.")
    dictionaries
  }

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

  def fixHeadwordsInFinalFile(file_pattern: String = ".*", baseDir: String = ".", headwordTransformer: (Array[String]) => Array[String], finalFileExtension: String = ".babylon_final", sort: Boolean = true, overwrite: Boolean = false) = {
    val files_to_ignore = Set("spokensanskrit.babylon")
    var dictionaries = getMatchingDictionaries(file_pattern, baseDir).filter(_.babylonFile.isDefined)
    log info (s"Got ${dictionaries.length} babylon files")
    var dictsToIgnore = dictionaries.filter(!overwrite && _.babylonFinalFileNewerThanBabylon())
    if (dictsToIgnore.nonEmpty) {
      log warn s"Ignoring these files, whose final babylon files seem updated:" + dictsToIgnore.mkString("\n")
    }
    dictionaries = dictionaries.filterNot(dictsToIgnore.contains(_))
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

  def addOptitrans(dictPattern: String = ".*", baseDir: String = ".", overwrite: Boolean = false) = {
    log info "=======================Adding optitrans headwords, making final babylon file."
    val headwordTransformer = (headwords_original:Array[String]) => (
      headwordTransformers.addOptitransFromDevanaagarii(
        headwordTransformers.addNonAnsusvaaraVariantsFromDevanaagarii(headwordTransformers.addDevanaagariiFromOtherIndic(headwords_original)))
      ).filterNot(_.isEmpty).distinct
    fixHeadwordsInFinalFile(file_pattern=dictPattern, baseDir=baseDir, headwordTransformer=headwordTransformer, sort=false, overwrite=overwrite)
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

  /**
    * Add optitrans and devanAgarI headwords from IAST. 
    * 
    * @param file_pattern
    * @param baseDir
    * @param indicWordSet
    */
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

  /**
    * Makes stardict files from babylon files.
    * 
    * @param dictPattern
    * @param babylonBinary
    */
  def makeStardict(dictPattern: String = ".*", babylonBinary: String, overwrite: Boolean = false) = {
    log info "=======================makeStardict"
    var dictionaries = getMatchingDictionaries(dictPattern)

    var dictsToIgnore = dictionaries.filter(!overwrite && _.ifoFileNewerThanBabylon())
    if (dictsToIgnore.nonEmpty) {
      log warn s"Ignoring these files, whose dict files seem updated: " + dictsToIgnore.mkString("\n")
    }
    dictionaries = dictionaries.filterNot(dictsToIgnore.contains(_))
    dictionaries.foreach(_.makeStardictFromBabylonFile(babylonBinary))
  }

  /**
    * Transliterates all words (mentioned to wordListFilePath) in sourceScheme to devanAgarI.
    * 
    * @param inFilePath
    * @param outFilePath
    * @param sourceScheme
    * @param wordListFilePath
    */
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


  /**
    * 
    * @param dictPattern
    * @param babylonBinary
    * @param tarBaseUrl - example value https://github.com/indic-dict/stardict-marathi/raw/gh-pages/ma-head/other-entries/tars
    * @param githubToken
    */
  def makeIndicStardictTar(dictPattern: String = ".*", babylonBinary: String, tarBaseUrl: String, githubToken: Option[String], overwrite: Boolean = false) = {
    var dictionaries = getMatchingDictionaries(dictPattern)
    val githubRepo=GithubRepo.fromUrl(url=tarBaseUrl, githubToken=githubToken)
    log info "=======================Full build from babylon to stardict tar."
    dictionaries.foreach(dictionary => {
      if (dictionary.babylonFile.isDefined) {
        val tarFileMatchesBabylon = dictionary.tarFileMatchesSource(githubRepo=githubRepo)
        if (!tarFileMatchesBabylon || overwrite) {
          addOptitrans(dictPattern = dictionary.dirName, overwrite = overwrite)
          dictionary.makeStardictFromBabylonFile(babylonBinary)
          dictionary.makeTar()
          tarProcessor.writeTarsList(tarDestination = dictionary.getTarDirFile.getCanonicalPath, urlBase=tarBaseUrl)
        } else {
          log info(s"Tar file for ${dictionary.name} is newer. Not overwriting.")
        }
      } else {
        log.info(s"**** No babylon file in ${dictionary.dirName} - skipping.")
        if (dictionary.ifoFile.isDefined) {
          val tarFileMatchesIfo = dictionary.tarFileMatchesSource(sourceFile = dictionary.ifoFile.get, githubRepo=githubRepo)
          if (!tarFileMatchesIfo || overwrite) {
            dictionary.makeTar()
          }
        } else {
          log.info(s"**** No babylon or ifo file in ${dictionary.dirName} - skipping.")
        }
      }
    })
  }

  
  /**
    * An ad-hoc test/ processing entry point.
    * @param args
    */
  def main(args: Array[String]): Unit = {
    val dictPattern = ".*"
    val workingDirInit = System.getProperty("user.dir")
    var workingDir = "/home/vvasuki/indic-dict/stardict-marathi/ma-head/other-entries"
    System.setProperty("user.dir", workingDir)
//    dumpWordToDictMap(basePaths=List(workingDir), outFilePath=s"${workingDir}wordlists/words_sa_dev.txt")
    // stripNonOptitransHeadwords(dictPattern, workingDir)
    // getDevanagariOptitransFromIast(dictPattern, workingDir)
//    getDevanagariOptitransFromIastIfIndic(dictPattern, workingDir, getWordToDictsMapFromPaths(List("/home/vvasuki/stardict-pali/pali-head/").keys))
//    addOptitrans(dictPattern = "Me.*", baseDir = "/home/vvasuki/indic-dict/stardict-sanskrit/sa-head/en-entries")
    makeIndicStardictTar(dictPattern = ".*", babylonBinary = "stardict-babylon", tarBaseUrl = "https://github.com/indic-dict/stardict-marathi/raw/gh-pages/ma-head/other-entries/tars", githubToken = None, overwrite = false)
    // makeStardict(dir, "/home/vvasuki/stardict/tools/src/babylon")
  }
}
