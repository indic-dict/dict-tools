//package stardict_sanskrit
//
//import com.github.sqlite4s.SQLiteConnection
//
//import java.io.File
//import java.sql.DriverManager
//

//class SQLiteDB(filePath: String) {
//  val db = DriverManager.getConnection("jdbc:sqlite:" + new File(filePath).toString).asInstanceOf[SQLiteConnection]
//  
//  def setup() = {
//    db.open(allowCreate=true)
//    try {
//      var st = db.prepare(sql="CREATE TABLE IF NOT EXISTS definitions(headword TEXT PRIMARY KEY, definitions TEXT NOT NULL) WITHOUT ROWID")
//      st.step()
//      st = db.prepare(sql="CREATE TABLE IF NOT EXISTS synonyms(synonym TEXT PRIMARY KEY, headword TEXT) WITHOUT ROWID")
//      st.step()
//    } finally {
//      db.dispose()
//    }
//  }
//  def addDefinition(definition: String, headwords: Seq[String], primaryHeadword: String=null) = {
//    try {
//      val primaryHeadwordFinal = if (primaryHeadword == null) primaryHeadword else headwords(0)
//      var st = db.prepare(sql=s"INSERT INTO definitions(headword,definition) VALUES(${primaryHeadword}, ${definition})\n  ON CONFLICT(headword) DO UPDATE SET definition=excluded.definition")
//      st.step()
//      headwords.foreach(headword => {
//        var st = db.prepare(sql=s"INSERT INTO synonyms(synonym,headword) VALUES(${headword}, ${primaryHeadword})\n  ON CONFLICT(synonym) DO UPDATE SET headword=excluded.headword")
//        st.step()
//      })
//    } finally {
//      db.dispose()
//    }
//  }
//}
