import java.nio.file.{Path, Paths}

final case class Config(
  apiBaseUrl: String,
  idx: Int,
  count: Int,
  dbPath: Path
)

object Config {
  val default: Config = Config(
    apiBaseUrl = "https://cn.bing.com/HPImageArchive.aspx",
    idx = 0,
    count = 8,
    dbPath = Paths.get("./src/main/resources/db/images.yaml")
  )
}
