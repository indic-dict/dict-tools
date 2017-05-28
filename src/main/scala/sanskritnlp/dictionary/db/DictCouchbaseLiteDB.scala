package sanskritnlp.dictionary.db

import _root_.java.io.File

import com.couchbase.lite.auth.BasicAuthenticator
import dbSchema.common.ScriptRendering
import dbSchema.dictionary.{DictEntry, DictLocation}
import dbUtils.{collectionUtils, jsonHelper}
import sanskrit_coders.db.couchbaseLite.couchbaseLiteUtils
import sanskritnlp.dictionary.BabylonDictionary
import stardict_sanskrit.babylonProcessor

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.StdIn

//import com.couchbase.lite.{Database, Manager, JavaContext, Document, UnsavedRevision, Query, ManagerOptions}
import com.couchbase.lite.util.Log
import com.couchbase.lite.{Database, Manager, JavaContext, Document, UnsavedRevision, Query, ManagerOptions}
//import org.json4s.jackson.Serialization
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

// This version of the database uses Java (rather than Android) API.
class DictCouchbaseLiteDB() {
  val log = LoggerFactory.getLogger(getClass.getName)
  var dictEntriesDb: Database = null
  var dbManager: Manager = null

  def openDatabasesLaptop() = {
    dbManager = new Manager(new JavaContext("data") {
      override def getRootDirectory: File = {
        val rootDirectoryPath = "/home/vvasuki/dict-tools"
        new File(rootDirectoryPath)
      }
    }, Manager.DEFAULT_OPTIONS)
    dbManager.setStorageType("ForestDB")
    dictEntriesDb = dbManager.getDatabase("dict_entries")
  }

  def replicateAll() = {
    couchbaseLiteUtils.replicate(dictEntriesDb)
  }


  def closeDatabases = {
    dictEntriesDb.close()
  }

  def purgeAll = {
    couchbaseLiteUtils.purgeDatabase(dictEntriesDb)
  }

  def updateDictEntry(dictEntry: DictEntry): Boolean = {
    val jsonMap = jsonHelper.getJsonMap(dictEntry)
    log debug (jsonMap.toString())
    //    sys.exit()
    val document = dictEntriesDb.getDocument(dictEntry.getKey)
    couchbaseLiteUtils.updateDocument(document, jsonMap)
    return true
  }


  def checkConflicts = {
    val query = dictEntriesDb.createAllDocumentsQuery
    query.setAllDocsMode(Query.AllDocsMode.ONLY_CONFLICTS)
    val result = query.run
    result.iterator().asScala.foreach(row => {
      if (row.getConflictingRevisions.size > 0) {
        Log.w("checkConflicts", "Conflict in document: %s", row.getDocumentId)
      }
    })
  }

  def listAllCaseClassObjects = {
    //    listCaseClassObjects(quoteDb.createAllDocumentsQuery)
    couchbaseLiteUtils.listCaseClassObjects(dictEntriesDb.createAllDocumentsQuery)
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
      updateDictEntry(dictEntry)
    }
  }
}


object dbMakerStardictSanskrit {
  val log = LoggerFactory.getLogger(getClass.getName)
  val dictDb = new DictCouchbaseLiteDB()

  def updateDb = {
    var workingDir = "/home/vvasuki/stardict-sanskrit/"
    val dicts = babylonProcessor.getRecursiveListOfBabylonDicts(basePaths = Seq("/home/vvasuki/stardict-sanskrit/sa-head/"))
    var exceptions = ListBuffer[String]()
    var failedDicts = ListBuffer[BabylonDictionary]()
    dicts.map(x => {
      log info x.toString()
      try {
        dictDb.dumpDictionary(babylonDictionary = x)
      } catch {
        case e => {
          exceptions.append(e.toString)
          failedDicts.append(x)
        }
      }
    })
    exceptions zip failedDicts foreach( x => log error x.toString())
  }

  def main(args: Array[String]): Unit = {
    dictDb.openDatabasesLaptop()
        dictDb.replicateAll()
    // dictDb.checkConflicts
    updateDb
    //    dictDb.listAllCaseClassObjects
    //    dictDb.purgeAll
  }
}
