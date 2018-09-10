package com.cricket360.scores
import java.io.File

import com.cricket360.connector.MongoConnector
import com.cricket360.players.PlayerHelper
import com.cricket360.stats.{Stats, StatsHelper}
import org.apache.poi.ss.usermodel.WorkbookFactory
import com.mongodb.casbah.Imports._
import net.liftweb.json._
import org.joda.time.DateTime

import collection.JavaConversions._


/**
  * Created by bkasinadhuni on 8/27/18.
  */
object ScoreCardUploader {

  def main(args: Array[String]): Unit = {
    println("uploading score card")

    val path = System.getProperty("scorecard.location")

    uploadScorecard(path)

  }

  def uploadScorecard(fileLocation: String) = {
    implicit val formats = DefaultFormats

    val f = new File(fileLocation)
    val workbook = WorkbookFactory.create(f)
    val scoreSheet = workbook.getSheetAt(0)
    val resultSheet = workbook.getSheetAt(1)

    val resultRow = resultSheet.drop(1).head
    val homeTeam = resultRow.getCell(0).getStringCellValue
    val oppTeam = resultRow.getCell(1).getStringCellValue
    val season = resultRow.getCell(2).getStringCellValue
    val matchType = resultRow.getCell(3).getStringCellValue
    val toss_result =resultRow.getCell(6).getStringCellValue
    val toss_decision =resultRow.getCell(7).getStringCellValue

   val resultObject =  DBObject(
     "home_team"->homeTeam,
      "opp_team" -> oppTeam,
      "season" -> season,
      "type" -> matchType,
     "match date"->resultRow.getCell(4).getDateCellValue,
     "location"->resultRow.getCell(5).getStringCellValue,
      "toss_result"->toss_result,
      "toss_decision"->toss_decision,
      "winner" -> resultRow.getCell(8).getStringCellValue,
      "details" -> resultRow.getCell(9).getStringCellValue,
    )


    val scoresMap = scoreSheet.drop(1).map(row => {

      val playerName = row.getCell(0).getStringCellValue
      val runsScored = row.getCell(1).getNumericCellValue.toLong
      val ballsPlayed =row.getCell(2).getNumericCellValue.toLong
      val runsGiven=row.getCell(6).getNumericCellValue.toLong
      val oversBowled =row.getCell(4).getNumericCellValue.toLong
      val dismissalType = row.getCell(3).getStringCellValue

      val isNotOut = if(dismissalType.equalsIgnoreCase("notout")) true else false
      val IsDuck = if(runsScored == 0) true else false
      val economyRate =if(oversBowled!=0 && runsGiven!=0) runsGiven.toDouble/oversBowled else 0
      val SR=(runsScored/ballsPlayed)*100
      val playerId = ""

      val player = PlayerHelper.getMongoPlayerData(playerName)

      val statsByPlayer: Stats = StatsHelper.getMongoStatsData(playerId)

      // Bat stats
      val totalScore = if(statsByPlayer!=null) statsByPlayer.batting_stats.runs + runsScored else runsScored
      val totalBalls = if(statsByPlayer!=null) statsByPlayer.batting_stats.balls + runsScored else ballsPlayed
      val totalMatches = if(statsByPlayer!=null) statsByPlayer.batting_stats.matches_played + 1 else 1

      val totalInnings =
        if(statsByPlayer!=null){
          if(ballsPlayed > 0) statsByPlayer.batting_stats.innings_played + 1 else statsByPlayer.batting_stats.innings_played
      }else if(ballsPlayed > 0) 1 else 0

      val totalNotOuts =
        if(statsByPlayer!=null){
          if(isNotOut) statsByPlayer.batting_stats.not_outs + 1 else statsByPlayer.batting_stats.not_outs
        }else if(isNotOut) 1 else 0

      val highestScore = if(statsByPlayer.batting_stats.highest_score < runsScored) runsScored else statsByPlayer.batting_stats.highest_score




      val batting = DBObject(
          "runs_scored" -> runsScored,
          "balls_played" -> ballsPlayed,
          "dismissal_type" -> dismissalType,
          "SR%" ->SR,
          "sixes" ->row.getCell(10).getNumericCellValue.toLong,
          "fours" ->row.getCell(11).getNumericCellValue.toLong,
          "IsNotOut"-> isNotOut,
          "IsDuck"->IsDuck
      )
      val bowling=DBObject(
        "overs_bowled" ->oversBowled,
        "wickets_Taken" -> row.getCell(5).getNumericCellValue.toLong,
        "runs_given" ->runsGiven,
        "Economy_Rate" ->economyRate,
        "No_Balls"->row.getCell(12).getNumericCellValue.toLong,
        "wide_Balls"->row.getCell(13).getNumericCellValue.toLong,
        "Maiden_overs"->row.getCell(14).getNumericCellValue.toLong
      )
      val fielding=DBObject(
        "catches_taken" -> row.getCell(7).getNumericCellValue.toLong,
        "catches_dropped" -> row.getCell(8).getNumericCellValue.toLong,
        "run_outs" -> row.getCell(9).getNumericCellValue.toLong
      )

        if (player.isDefined) {
          DBObject(
            "player" -> player,
            "batting"-> batting,
            "bowling"->bowling,
            "fielding"->fielding
          )
        } else DBObject()
    }).toList

    try {
      //push to mongo scores table ->

      val scoreObject = DBObject(
        "gameId" -> s"${homeTeam}_${oppTeam}_${season}_${matchType}",
        "result" -> resultObject,
        "scoreCard" -> scoresMap,
        "UpdatedAt" -> DateTime.now.toDate
      )

      MongoConnector.scores.save(scoreObject)
      println(s"Successfully uploaded Score for $homeTeam vs $oppTeam")
    }catch{
      case e: Exception => println(new Exception(e.getMessage))
    }
  }
}
