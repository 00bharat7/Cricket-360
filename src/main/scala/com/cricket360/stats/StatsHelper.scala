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

case class Stats(playerId: String, batting_stats: BatStats , bowling_stats: BowlStats, fielding_stats: FieldStats, wins: Int)

case class BatStats(matches_played: Int, innings_played: Int, not_outs: Int, runs: Int, balls: Int, average: Float, strike_rate: Float, highest_score: Int, hundreds: Int, fifties: Int, thirties: Int, duck_outs: Int, fours: Int, sixes: Int, credits: Int)

case class BowlStats(matches_played: Int, innings_played: Int, overs: Int, runs: Int, wickets: Int, average: Float, economy: Float, strike_rate: Float, three_wickets: Int, four_wickets: Int, five_wickets: Int, wides: Int,no_balls: Int, credits: Int)

case class FieldStats(catches_taken: Int, catches_dropped: Int, run_outs: Int, credits: Int)