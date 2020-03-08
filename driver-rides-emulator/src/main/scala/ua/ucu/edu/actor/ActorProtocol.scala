package ua.ucu.edu.actor

case object ReadFeature

case class RespondFeature(featureId: String, featureType: String, value: String)