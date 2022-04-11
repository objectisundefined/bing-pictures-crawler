import cats.effect._
import cats.syntax.all._
import java.io.{File, InputStream, OutputStream, FileInputStream, FileOutputStream, ByteArrayInputStream}
import scala.io._

/* Config */
object PullC {
  val target = "./src/main/resources/db/images.yaml"
}

/* Model */
object PullM {
  final case class Image(copyright: String, startdate: String, enddate: String, url: String)

  final case class Res(images: List[Image])
}

/* Fetcher */
object PullF {
  import PullM._

  // https://circe.github.io/circe/
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  // https://sttp.softwaremill.com/en/latest/examples.html
  import sttp.client3._
  import sttp.client3.circe._

  val backend = HttpURLConnectionBackend()

  def fromEither[A](e: Either[Throwable, A]): IO[A] = e.fold(IO.raiseError, IO.pure)

  val fill = (x: Image) => x.copy(url = s"https://cn.bing.com${x.url}")

  val run = (n: Int, size: Int) => IO {
    val response: Identity[Response[Either[ResponseException[String, io.circe.Error], Res]]] =
      basicRequest
        .get(uri"https://cn.bing.com/HPImageArchive.aspx?format=js&idx=${n}&n=${size}&mkt=zh-CN")
        .response(asJson[Res])
        .send(backend)

    response.body.map(_.images.map(fill).reverse)
  } flatMap fromEither
}

/* Reader */
object PullR {
  import java.io._

  val acquire: IO[BufferedReader] = IO {
    new BufferedReader(new FileReader(new File(PullC.target)))
  }

  val release: BufferedReader => IO[Unit] = br => IO { br.close } *> IO(())

  val loop: (BufferedReader, List[String], Int) => IO[List[String]] = (in, acc, range) =>
    IO(in.readLine()).flatMap[List[String]] { line =>
      if (line != null) {
        loop(in, line +: acc.take(range - 1), range)
      } else {
        IO(acc)
      }
    }

  val run = (range: Int) => Resource.make(acquire)(release).use(br => loop(br, List[String](), range))
}

/* Writer */
object PullW {
  import java.io._

  val acquire: IO[BufferedWriter] = IO {
    new BufferedWriter(new FileWriter(new File(PullC.target), true))
  }

  val release: BufferedWriter => IO[Unit] = bw => IO { bw.close } *> IO(())

  val run = (lines: List[String]) => Resource.make(acquire)(release).use(bw => lines.map(line => IO { bw.write(s"${line}\n") }).sequence.void)
}

object PullT extends IOApp {
  override def run(args: List[String]): cats.effect.IO[cats.effect.ExitCode] =
    for {
      tail <- PullR.run(7)
      images <- PullF.run(0, 7)
      _ <- PullW.run(images.map(x => s"- ${x.url}").filter(!tail.contains(_)))
    } yield ExitCode.Success
}
