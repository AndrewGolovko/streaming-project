package ua.ucu.edu.feature
import java.util.concurrent.ThreadLocalRandom
import scala.util.Random

class FeatureGenerator extends FeatureApi {

  override def readCurrentValue(featureType: Feature): String = {
    featureType match {
      case TripFeatures => (ThreadLocalRandom.current.nextDouble(-74.25, -72.98)).toString + '|' +
        (ThreadLocalRandom.current.nextDouble(40.56, 41.71)).toString + "|" +(ThreadLocalRandom.current.nextDouble(-74.25, -72.98)).toString + '|' +
        (ThreadLocalRandom.current.nextDouble(40.56, 41.71)).toString + "|" + Random.nextInt(4).toString
    }
  }
}


