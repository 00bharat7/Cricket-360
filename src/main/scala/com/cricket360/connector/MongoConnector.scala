package com.cricket360.connector

import com.mongodb.casbah.MongoConnection

/**
  * Created by bkasinadhuni on 8/27/18.
  */
object MongoConnector {

  private val SERVER = "localhost"
  private val PORT   = 27017
  private val DATABASE = "cricket360"
  private val COLLECTION1 = "scores"
  private val COLLECTION2 = "players"
  private val COLLECTION3 = "stats"
  val connection = MongoConnection(SERVER)
  val scores = connection(DATABASE)(COLLECTION1)
  val players= connection(DATABASE)(COLLECTION2)
  val stats= connection(DATABASE)(COLLECTION3)

}
