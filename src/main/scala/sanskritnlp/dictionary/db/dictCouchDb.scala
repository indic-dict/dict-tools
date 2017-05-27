package sanskritnlp.dictionary.db

import com.ibm.couchdb.{CouchDb, TypeMapping}
import dbSchema.common.{ScriptRendering, Source}
import dbSchema.dictionary.{DictEntry, DictLocation}
import org.slf4j.LoggerFactory
import sanskritnlp.dictionary.BabylonDictionary
import stardict_sanskrit.babylonProcessor

object dictCouchDb {
  val log = LoggerFactory.getLogger(getClass.getName)
  var couch = CouchDb("127.0.0.1", 5984)
  val typeMapping = TypeMapping(classOf[Source] -> "Source", classOf[DictEntry] -> "DictEntry")
  var dictEntriesDb = couch.db("dict_entries", typeMapping)
  var bookDetailsDb = couch.db("book_details", typeMapping)

  def initialize = {
    couch  = CouchDb("127.0.0.1", 5984)
    dictEntriesDb = couch.db("dict_entries", typeMapping)
    bookDetailsDb = couch.db("book_details", typeMapping)
  }

  def dumpDictionary(babylonDictionary: BabylonDictionary) = {
    var entryNumber = -1
    while (babylonDictionary.hasNext()) {
      entryNumber = entryNumber + 1
      val (headwords, meaning) = babylonDictionary.next()
      val dictEntry = DictEntry(headwords = headwords.map(ScriptRendering(_)),
        entry = ScriptRendering(text = meaning),
        location = DictLocation(dictionaryId = babylonDictionary.fileLocation.replace("/home/vvasuki/", "").
          replaceAll("\\.babylon.+", "").replaceAll("/", "__"),
        entryNumber = entryNumber))
//      log debug dictEntry.toString
//      log debug jsonHelper.getJsonMap(dictEntry).toString()
      val action = dictEntriesDb.docs.create(obj = dictEntry, id = dictEntry.getKey)
//      action.unsafePerformAsync((t) => {
//        log debug (t.toString)
//      })
      log debug action.unsafePerformSync.toString
    }
  }

  def main(args: Array[String]): Unit = {
    var workingDir = "/home/vvasuki/stardict-sanskrit/"
    val dicts = babylonProcessor.getRecursiveListOfBabylonDicts(basePaths = Seq("/home/vvasuki/stardict-sanskrit/sa-head/sa-entries/"))
    dicts.take(1).map(x => {
      log info x.toString()
      dumpDictionary(babylonDictionary = x)
    })
    couch.client.client.shutdownNow()
  }
}

