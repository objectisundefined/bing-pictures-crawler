import cats.Monad
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._

object Main extends IOApp {

  /** Core business logic, fully polymorphic over any monadic effect `F`.
   *
   *  Reads the set of already-stored URLs, fetches a fresh batch from the
   *  Bing API, filters out duplicates, and appends only the new entries.
   *  The function is pure with respect to its effect type and carries no
   *  hard dependency on `IO`, which makes it straightforward to test.
   */
  def program[F[_]: Monad](store: Store[F], client: BingClient[F], config: Config): F[Unit] =
    for {
      existing <- store.readUrls
      images   <- client.fetchImages(config.idx, config.count)
      newUrls   = images.map(_.absoluteUrl).filterNot(existing)
      _        <- store.appendUrls(newUrls)
    } yield ()

  def run(args: List[String]): IO[ExitCode] = {
    val config = Config.default
    BingClient
      .make(config.apiBaseUrl)
      .use { client =>
        program(Store.file(config.dbPath), client, config)
          .as(ExitCode.Success)
          .handleErrorWith { err =>
            IO.println(s"[error] ${err.getMessage}").as(ExitCode.Error)
          }
      }
  }
}
