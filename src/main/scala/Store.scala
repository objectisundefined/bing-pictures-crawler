import cats.effect.{IO, Resource}
import cats.syntax.all._
import java.io.{BufferedReader, BufferedWriter, FileReader, FileWriter, FileNotFoundException}
import java.nio.file.Path

/** Algebra for persisting and retrieving image URLs. */
trait Store[F[_]] {
  def readUrls: F[Set[String]]
  def appendUrls(urls: List[String]): F[Unit]
}

object Store {

  /** File-backed interpreter that reads / appends YAML list entries (`- <url>`). */
  def file(path: Path): Store[IO] = new Store[IO] {

    def readUrls: IO[Set[String]] =
      Resource
        .make(IO.blocking(new BufferedReader(new FileReader(path.toFile))))(r =>
          IO.blocking(r.close())
        )
        .use { reader =>
          IO.blocking(
            Iterator
              .continually(reader.readLine())
              .takeWhile(_ != null)
              .collect { case line if line.startsWith("- ") => line.drop(2) }
              .toSet
          )
        }
        .handleErrorWith {
          case _: FileNotFoundException => IO.pure(Set.empty[String])
          case e                        => IO.raiseError(AppError.ReadError(path.toString, e))
        }

    def appendUrls(urls: List[String]): IO[Unit] =
      if (urls.isEmpty) IO.unit
      else
        Resource
          .make(IO.blocking(new BufferedWriter(new FileWriter(path.toFile, true))))(w =>
            IO.blocking(w.close())
          )
          .use { writer =>
            urls.traverse_(url => IO.blocking(writer.write(s"- $url\n")))
          }
          .adaptError { case e => AppError.WriteError(path.toString, e) }
  }
}
