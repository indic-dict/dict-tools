package sanskritnlp.dictionary.db

import _root_.java.io.File

import com.couchbase.lite.auth.BasicAuthenticator
import dbSchema.common.ScriptRendering
import dbSchema.dictionary.{DictEntry, DictLocation}
import dbUtils.{collectionUtils, jsonHelper}
import sanskritnlp.dictionary.BabylonDictionary
import stardict_sanskrit.babylonProcessor

import scala.collection.mutable
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
  val replicationUrl = "http://127.0.0.1:5984/"
  var replicationPw = ""

  def openDatabasesLaptop() = {
    dbManager = new Manager(new JavaContext("data") {
      override def getRootDirectory: File = {
        val rootDirectoryPath = "/home/vvasuki/dict-tools"
        new File(rootDirectoryPath)
      }
    }, Manager.DEFAULT_OPTIONS)
    dbManager.setStorageType("ForestDB")
    dictEntriesDb = dbManager.getDatabase(s"dict_entries")
  }

  def replicate(database: Database) = {
    import java.net.URL

    import com.couchbase.lite.replicator.Replication
    val url = new URL(replicationUrl + database.getName)
    log.info("replicating to " + url.toString())
    if (replicationPw.isEmpty) {
      log info "Enter password"
      replicationPw = StdIn.readLine().trim
    }
    val auth = new BasicAuthenticator("vvasuki", replicationPw)

    val push = database.createPushReplication(url)
    push.setAuthenticator(auth)
    push.setContinuous(true)
    push.addChangeListener(new Replication.ChangeListener() {
      override def changed(event: Replication.ChangeEvent): Unit = {
        log.info(event.toString)
      }
    })
    push.start

    val pull = database.createPullReplication(url)
    //    pull.setContinuous(true)
    pull.setAuthenticator(auth)
    pull.addChangeListener(new Replication.ChangeListener() {
      override def changed(event: Replication.ChangeEvent): Unit = {
        log.info(event.toString)
      }
    })
    pull.start
  }

  def replicateAll() = {
    replicate(dictEntriesDb)
  }


  def closeDatabases = {
    dictEntriesDb.close()
  }

  def purgeDatabase(database: Database) = {
    val result = database.createAllDocumentsQuery().run
    val docObjects = result.iterator().asScala.map(_.getDocument).map(_.purge())
  }

  def purgeAll = {
    purgeDatabase(dictEntriesDb)
  }

  def updateDocument(document: Document, jsonMap: Map[String, Object]) = {
    document.update(new Document.DocumentUpdater() {
      override def update(newRevision: UnsavedRevision): Boolean = {
        val properties = newRevision.getUserProperties
        val jsonMapJava = collectionUtils.toJava(jsonMap).asInstanceOf[java.util.Map[String, Object]]
        //        log debug jsonMapJava.getClass.toString
        properties.putAll(jsonMapJava)
        newRevision.setUserProperties(properties)
        true
      }
    })
  }

  def updateDictEntry(dictEntry: DictEntry): Boolean = {
    val jsonMap = jsonHelper.getJsonMap(dictEntry)
    log debug (jsonMap.toString())
    //    sys.exit()
    val document = dictEntriesDb.getDocument(dictEntry.getKey)
    updateDocument(document, jsonMap)
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

  def listCaseClassObjects(query: Query) = {
    val result = query.run
    val docObjects = result.iterator().asScala.map(_.getDocument).map(doc => {
      val jsonMap = collectionUtils.toScala(doc.getUserProperties).asInstanceOf[mutable.Map[String, _]]
      //      val jsonMap = doc.getUserProperties
      jsonHelper.fromJsonMap(jsonMap)
    })
    //    log info s"We have ${quotes.length} quotes."
    docObjects.foreach(quoteText => {
      log info quoteText.toString
      log info jsonHelper.getJsonMap(quoteText).toString()
    })
  }

  def listAllCaseClassObjects = {
    //    listCaseClassObjects(quoteDb.createAllDocumentsQuery)
    listCaseClassObjects(dictEntriesDb.createAllDocumentsQuery)
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
    val dicts = babylonProcessor.getRecursiveListOfBabylonDicts(basePaths = Seq("/home/vvasuki/stardict-sanskrit/sa-head/sa-entries/"))
    dicts.take(5).map(x => {
      log info x.toString()
      dictDb.dumpDictionary(babylonDictionary = x)
    })
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
