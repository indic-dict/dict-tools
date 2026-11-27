package sanskritnlp.dictionary

import java.io.{BufferedInputStream, File, FileInputStream, FileNotFoundException}
import java.nio.file.{AtomicMoveNotSupportedException, Files, Path, Paths, StandardCopyOption}
import java.util.Comparator

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

  private def deleteRecursively(path: Path): Unit = {
    if (Files.exists(path)) {
      val stream = Files.walk(path)
      try {
        val it = stream.sorted(Comparator.reverseOrder()).iterator()
        while (it.hasNext) {
          Files.delete(it.next())
        }
      } finally {
        if (stream != null) stream.close()
      }
    }
  }

  def extract(destinationPath: String): Unit = {
    log.info(s"Extracting ${filePath} to ${destinationPath}")
    var archiveInputStream = inputStreamFromArchive()
    import java.io.IOException

    import org.apache.commons.compress.utils.IOUtils
    val destDir = Paths.get(destinationPath).toFile.getCanonicalFile
    try {
      var entry = archiveInputStream.getNextEntry
      while (entry != null) {
        if (!archiveInputStream.canReadEntryData(entry)) { // log something?
          log.warn("Cannot read next entry!")
        } else {
          val f = new File(destDir, entry.getName)
          val destCanonicalPath = destDir.getCanonicalPath
          val fileCanonicalPath = f.getCanonicalPath
          if (!fileCanonicalPath.startsWith(destCanonicalPath + File.separator) && fileCanonicalPath != destCanonicalPath) {
            throw new IOException("Entry is outside of the target dir: " + entry.getName)
          }
          if (entry.isDirectory) {
            if (Files.exists(f.toPath) && !Files.isDirectory(f.toPath)) {
              Files.delete(f.toPath)
            }
            if (!f.isDirectory && !f.mkdirs) throw new IOException("failed to create directory " + f)
          } else {
            val parent = f.getParentFile
            if (parent != null) {
              if (Files.exists(parent.toPath) && !Files.isDirectory(parent.toPath)) {
                Files.delete(parent.toPath)
              }
              if (!parent.isDirectory && !parent.mkdirs) throw new IOException("failed to create directory " + parent)
            }
            if (Files.exists(f.toPath) && Files.isDirectory(f.toPath)) {
              deleteRecursively(f.toPath)
            }
            val targetParent = if (parent != null) parent.toPath else destDir.toPath
            val tmpFile = Files.createTempFile(targetParent, "." + f.getName + ".", ".tmp")
            try {
              val o = Files.newOutputStream(tmpFile)
              try
                IOUtils.copy(archiveInputStream, o)
              finally if (o != null) o.close()
              try {
                Files.move(tmpFile, f.toPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
              } catch {
                case _: AtomicMoveNotSupportedException =>
                  Files.move(tmpFile, f.toPath, StandardCopyOption.REPLACE_EXISTING)
              }
            } finally {
              Files.deleteIfExists(tmpFile)
            }
          }
        }
        entry = archiveInputStream.getNextEntry
      }
    } finally {
      if (archiveInputStream != null) archiveInputStream.close()
    }
  }

}
