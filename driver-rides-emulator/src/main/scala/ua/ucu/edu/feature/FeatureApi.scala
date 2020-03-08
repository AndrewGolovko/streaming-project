package ua.ucu.edu.feature

trait FeatureApi {
  def readCurrentValue(featureType: Feature): String
}
