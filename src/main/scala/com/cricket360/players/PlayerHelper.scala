package com.cricket360.players

import com.cricket360.connector.MongoConnector
import com.mongodb.casbah.Imports._

/**
  * Created by bkasinadhuni on 9/9/18.
  */
object PlayerHelper {


  def getMongoPlayerData(playerName: String) = {
    println("Fetching Player Info")
    val player = MongoConnector.players.findOne(MongoDBObject("alias" -> MongoDBObject("$regex" -> playerName.toLowerCase)))
    player
  }

}
