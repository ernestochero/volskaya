package commons

import models.Coordinate

object PolygonUtils {

  val polygon = Array(
    Coordinate(-8.05168, -79.05373),
    Coordinate(-8.05203, -79.0549),
    Coordinate(-8.05276, -79.05525),
    Coordinate(-8.05478, -79.05453),
    Coordinate(-8.05644, -79.0539),
    Coordinate(-8.05757, -79.05377),
    Coordinate(-8.06004, -79.05603),
    Coordinate(-8.06538, -79.05813),
    Coordinate(-8.06873, -79.06107),
    Coordinate(-8.0716, -79.06391),
    Coordinate(-8.07354, -79.0643),
    Coordinate(-8.085, -79.05388),
    Coordinate(-8.08723, -79.05448),
    Coordinate(-8.08895, -79.05354),
    Coordinate(-8.09941, -79.05173),
    Coordinate(-8.10361, -79.04903),
    Coordinate(-8.10546, -79.04845),
    Coordinate(-8.11106, -79.05031),
    Coordinate(-8.11437, -79.05029),
    Coordinate(-8.11517, -79.0496),
    Coordinate(-8.12209, -79.0483),
    Coordinate(-8.12592, -79.05075),
    Coordinate(-8.13012, -79.05329),
    Coordinate(-8.13586, -79.05652),
    Coordinate(-8.136, -79.0586),
    Coordinate(-8.1352, -79.06034),
    Coordinate(-8.13454, -79.06228),
    Coordinate(-8.13692, -79.06537),
    Coordinate(-8.13839, -79.06395),
    Coordinate(-8.13977, -79.06237),
    Coordinate(-8.14259, -79.05907),
    Coordinate(-8.14537, -79.05577),
    Coordinate(-8.14718, -79.05358),
    Coordinate(-8.14898, -79.05148),
    Coordinate(-8.15024, -79.0499),
    Coordinate(-8.15161, -79.04801),
    Coordinate(-8.15368, -79.04558),
    Coordinate(-8.15404, -79.04261),
    Coordinate(-8.15351, -79.04164),
    Coordinate(-8.15261, -79.04159),
    Coordinate(-8.15126, -79.04048),
    Coordinate(-8.15268, -79.03893),
    Coordinate(-8.15334, -79.03695),
    Coordinate(-8.1527, -79.03554),
    Coordinate(-8.15075, -79.0335),
    Coordinate(-8.14578, -79.02905),
    Coordinate(-8.14352, -79.02718),
    Coordinate(-8.14253, -79.02462),
    Coordinate(-8.1403, -79.0239),
    Coordinate(-8.13865, -79.02316),
    Coordinate(-8.13698, -79.0229),
    Coordinate(-8.13649, -79.02139),
    Coordinate(-8.13701, -79.02061),
    Coordinate(-8.1385, -79.02028),
    Coordinate(-8.13995, -79.01944),
    Coordinate(-8.14111, -79.01796),
    Coordinate(-8.1403, -79.01526),
    Coordinate(-8.13842, -79.01309),
    Coordinate(-8.13666, -79.01422),
    Coordinate(-8.13471, -79.01587),
    Coordinate(-8.13404, -79.01829),
    Coordinate(-8.13253, -79.01997),
    Coordinate(-8.13222, -79.01992),
    Coordinate(-8.1318, -79.01983),
    Coordinate(-8.13072, -79.01955),
    Coordinate(-8.12791, -79.01745),
    Coordinate(-8.12672, -79.01668),
    Coordinate(-8.12562, -79.01575),
    Coordinate(-8.12513, -79.0128),
    Coordinate(-8.12396, -79.01011),
    Coordinate(-8.12302, -79.00579),
    Coordinate(-8.1223, -79.00286),
    Coordinate(-8.12047, -79.00079),
    Coordinate(-8.11638, -79.00264),
    Coordinate(-8.10676, -78.995),
    Coordinate(-8.10198, -78.99872),
    Coordinate(-8.09933, -78.99592),
    Coordinate(-8.09685, -78.99523),
    Coordinate(-8.09441, -78.99454),
    Coordinate(-8.09072, -78.99273),
    Coordinate(-8.08758, -78.99119),
    Coordinate(-8.08427, -78.99034),
    Coordinate(-8.07555, -78.99136),
    Coordinate(-8.07337, -78.99095),
    Coordinate(-8.07217, -78.99195),
    Coordinate(-8.07264, -78.99994),
    Coordinate(-8.07235, -79.00249),
    Coordinate(-8.07138, -79.00461),
    Coordinate(-8.07095, -79.0056),
    Coordinate(-8.07016, -79.00657),
    Coordinate(-8.0698, -79.00978),
    Coordinate(-8.06986, -79.01117),
    Coordinate(-8.07013, -79.01265),
    Coordinate(-8.07036, -79.01379),
    Coordinate(-8.07048, -79.01485),
    Coordinate(-8.07122, -79.01803),
    Coordinate(-8.07145, -79.01913),
    Coordinate(-8.07174, -79.02033),
    Coordinate(-8.07183, -79.02136),
    Coordinate(-8.07239, -79.02195),
    Coordinate(-8.0722, -79.02372),
    Coordinate(-8.07194, -79.02547),
    Coordinate(-8.07207, -79.03054),
    Coordinate(-8.07075, -79.03508),
    Coordinate(-8.06863, -79.03813),
    Coordinate(-8.06639, -79.04017),
    Coordinate(-8.06407, -79.04222),
    Coordinate(-8.06241, -79.04393),
    Coordinate(-8.05715, -79.04719),
    Coordinate(-8.05334, -79.05009),
    Coordinate(-8.05234, -79.05104),
    Coordinate(-8.05151, -79.05211)
  )

  def inside(point: Coordinate): Boolean = {
    // ray-casting algorithm based on
    // http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
    val (x, y) = point.getCoordinate

    var inside = false

    var i = 0
    var j = polygon.length - 1
    while ({
      i < polygon.length
    }) {
      val (xi, yi) = polygon(i).getCoordinate
      val (xj, yj) = polygon(j).getCoordinate

      val intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
      if (intersect) inside = !inside

      j = {
        i += 1; i - 1
      }
    }

    inside
  }

}
