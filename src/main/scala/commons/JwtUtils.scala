package commons
import pdi.jwt._
import pdi.jwt.algorithms.JwtHmacAlgorithm
import scala.util.Try
object JwtUtils {
  private val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS256
  def createJwtToken(secretKey: String): String =
    Jwt.encode("""{"user": 1}""", secretKey, algorithm)

  def decodeJwtToken(token: String, secretKey: String): Try[String] =
    Jwt.decodeRaw(token, secretKey, Seq(algorithm))
}
