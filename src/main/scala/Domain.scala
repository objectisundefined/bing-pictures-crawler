import io.circe.{Decoder, HCursor}

/** A single image entry returned by the Bing daily-image API. */
final case class BingImage(
  copyright: String,
  startdate: String,
  enddate: String,
  url: String
) {
  /** Prepend the Bing hostname to the relative URL path. */
  def absoluteUrl: String = s"https://cn.bing.com$url"
}

object BingImage {
  implicit val decoder: Decoder[BingImage] = (c: HCursor) =>
    for {
      copyright <- c.downField("copyright").as[String]
      startdate <- c.downField("startdate").as[String]
      enddate   <- c.downField("enddate").as[String]
      url       <- c.downField("url").as[String]
    } yield BingImage(copyright, startdate, enddate, url)
}

/** Top-level API response envelope. */
final case class BingResponse(images: List[BingImage])

object BingResponse {
  implicit val decoder: Decoder[BingResponse] = (c: HCursor) =>
    c.downField("images").as[List[BingImage]].map(BingResponse(_))
}

/** Typed error hierarchy for the whole application. */
sealed abstract class AppError(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

object AppError {
  final case class ApiError(statusCode: Int, body: String)
      extends AppError(s"Bing API returned $statusCode: $body")

  final case class ParseError(message: String)
      extends AppError(s"JSON parse failure: $message")

  final case class ReadError(path: String, cause: Throwable)
      extends AppError(s"Cannot read $path: ${cause.getMessage}", Some(cause))

  final case class WriteError(path: String, cause: Throwable)
      extends AppError(s"Cannot write $path: ${cause.getMessage}", Some(cause))
}
