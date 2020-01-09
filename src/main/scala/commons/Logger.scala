package commons
import org.log4s.getLogger
object Logger {
  implicit final val logger: org.log4s.Logger = getLogger("volskayaLogger")
}
