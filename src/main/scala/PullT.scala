import cats.effect._
import cats.syntax.all._
import java.io.{File, InputStream, OutputStream, FileInputStream, FileOutputStream, ByteArrayInputStream}
import scala.io._

object PullM {
  final case class Image( copyright: String, startdate: String, enddate: String, url: String)

  final case class Res(images: List[Image])
}

object PullR {
  import PullM._

  // https://circe.github.io/circe/
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  // https://sttp.softwaremill.com/en/latest/examples.html
  import sttp.client3._
  import sttp.client3.circe._

  val backend = HttpURLConnectionBackend()

  def run = IO {
    val response: Identity[Response[Either[ResponseException[String, io.circe.Error], Res]]] =
      basicRequest
        .get(uri"http://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=100&mkt=zh-CN")
        .response(asJson[Res])
        .send(backend)

    response.body.map(_.images)
  }
}

object PullW {
  import scala.concurrent.ExecutionContext
  import scala.concurrent.duration._

  // https://sttp.softwaremill.com/en/latest/examples.html
  import sttp.client3._
  import sttp.model._

  def arrayToTuple[T] = (l: Array[T]) => (l(0), l(1))

  val backend = HttpURLConnectionBackend()

  def run(url: String) = IO {
    // eg: /th?id=OHR.FortedeSao_ZH-CN0093358703_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp
    val segments = url.split("\\?")

    val path = segments(0)
    val params = segments(1).split("&").map(s => arrayToTuple[String](s.split("="))).toMap

    val name = params.get("id").map(_.substring(4)).getOrElse("default")

    val uri = Uri(host = "cn.bing.com").withWholePath(path).withParams(params)

    basicRequest
      .get(uri)
      .response(asFile(new File(s"./src/main/resources/db/${name}")))
      .send(backend)
  }
}

object PullT extends IOApp {
  def fromEither[A](e: Either[Throwable, A]): IO[A] = e.fold(IO.raiseError, IO.pure)

  override def run(args: List[String]): cats.effect.IO[cats.effect.ExitCode] =
    for {
      imagesE <- PullR.run
      images <- fromEither(imagesE)
      _ <- images.map(i => PullW.run(i.url)).parSequence.void
    } yield ExitCode.Success
}
