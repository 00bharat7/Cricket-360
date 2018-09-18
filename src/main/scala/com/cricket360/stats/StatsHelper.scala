package com.cricket360.stats

import com.cricket360.connector.MongoConnector
import com.mongodb.casbah.Imports.MongoDBObject

/**
  * Created by bkasinadhuni on 9/9/18.
  */
object StatsHelper {

  def getMongoStatsData(playerId: String) = {
    println(s"Fetching Stats for $playerId")
    val statsByPlayer = MongoConnector.stats.findOne(MongoDBObject("playerId" -> playerId)).map(_.asInstanceOf[Stats])

    statsByPlayer
  }
}

case class Stats(playerId: String, battingStats: BatStats , bowlingStats: BowlStats, fieldingStats: FieldStats, wins: Int)

case class BatStats(matchesPlayed: Int, inningsPlayed: Int, notOuts: Int, runs: Int, balls: Int, average: Float, strikeRate: Float, highestScore: Int, hundreds: Int, fifties: Int, thirties: Int, duckOuts: Int, fours: Int, sixes: Int, credits: Int)

case class BowlStats(matchesPlayed: Int, inningsPlayed: Int, overs: Int, runs: Int, wickets: Int, average: Float, economy: Float, strikeRate: Float, threeWicketHaul: Int, fourWicketHaul: Int, fiveWicketHaul: Int, wides: Int,noBalls: Int, credits: Int)

case class FieldStats(catchesTaken: Int, catchesDropped: Int, runOuts: Int, credits: Int)