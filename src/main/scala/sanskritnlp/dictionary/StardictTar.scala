package sanskritnlp.dictionary

import java.io.{BufferedInputStream, File, FileInputStream, FileNotFoundException}
import java.nio.file.Paths

import org.apache.commons.compress.archivers.{ArchiveException, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorException
import org.slf4j.LoggerFactory

class StardictTar(filePath: String) {
  val log = LoggerFactory.getLogger(this.getClass)

  private val archiveStreamFactory = new ArchiveStreamFactory

  import org.apache.commons.compress.compressors.CompressorStreamFactory
  private val compressorStreamFactory = new CompressorStreamFactory(true /*equivalent to setDecompressConcatenated*/)

  @throws[FileNotFoundException]
  @throws[CompressorException]
  @throws[ArchiveException]
  private def inputStreamFromArchive() = { // To handle "IllegalArgumentException: Mark is not supported", we wrap with a BufferedInputStream
    // as suggested in http://apache-commons.680414.n4.nabble.com/Compress-Reading-archives-within-archives-td746866.html
    archiveStreamFactory.createArchiveInputStream(new BufferedInputStream(compressorStreamFactory.createCompressorInputStream(new BufferedInputStream(new FileInputStream(filePath)))))
  }

  def extract(destinationPath: String): Unit = {
    log.info(s"Extracting ${filePath}")
    var archiveInputStream = inputStreamFromArchive()
    import java.io.IOException
    import java.nio.file.Files

    import org.apache.commons.compress.utils.IOUtils
    var entry = archiveInputStream.getNextEntry
    while ( entry != null) {
      if (!archiveInputStream.canReadEntryData(entry)) { // log something?
        log.warn("Cannot read next entry!")
      } else {
        val f = new File(Paths.get(destinationPath, entry.getName).toString)
        if (entry.isDirectory) {
          if (!f.isDirectory && !f.mkdirs) throw new IOException("failed to create directory " + f)
        } else {
          val parent = f.getParentFile
          if (!parent.isDirectory && !parent.mkdirs) throw new IOException("failed to create directory " + parent)
          val o = Files.newOutputStream(f.toPath)
          try
            IOUtils.copy(archiveInputStream, o)
          finally if (o != null) o.close()
        }
      }
      entry = archiveInputStream.getNextEntry
    }
  }

}
