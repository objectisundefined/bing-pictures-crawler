import cats.effect.{IO, Resource}
import sttp.client3._
import sttp.client3.circe._

/** Algebra for fetching images from the Bing daily-image API. */
trait BingClient[F[_]] {
  def fetchImages(idx: Int, count: Int): F[List[BingImage]]
}

object BingClient {

  /** sttp interpreter backed by a synchronous `HttpURLConnectionBackend`.
   *
   *  The backend is allocated once per `Resource` lifetime and properly
   *  closed on release, ensuring no connections are leaked.
   */
  def make(baseUrl: String): Resource[IO, BingClient[IO]] =
    Resource
      .make(IO.blocking(HttpURLConnectionBackend()))(b => IO.blocking(b.close()))
      .map { backend =>
        new BingClient[IO] {
          def fetchImages(idx: Int, count: Int): IO[List[BingImage]] =
            IO.blocking(
              basicRequest
                .get(uri"$baseUrl?format=js&idx=$idx&n=$count&mkt=zh-CN")
                .response(asJson[BingResponse])
                .send(backend)
                .body
            ).flatMap {
              case Right(resp)                              => IO.pure(resp.images)
              case Left(HttpError(body, status))            => IO.raiseError(AppError.ApiError(status.code, body))
              case Left(DeserializationException(_, error)) => IO.raiseError(AppError.ParseError(error.getMessage))
              case Left(other)                              => IO.raiseError(AppError.ParseError(other.getMessage))
            }
        }
      }
}
