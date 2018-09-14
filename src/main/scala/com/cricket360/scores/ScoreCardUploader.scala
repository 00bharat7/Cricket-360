package com.cricket360.scores
import java.io.File

import com.cricket360.connector.MongoConnector
import com.cricket360.players.{Player, PlayerHelper}
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
    val winner = resultRow.getCell(8).getStringCellValue

   val resultObject =  DBObject(
     "home_team"->homeTeam,
      "opp_team" -> oppTeam,
      "season" -> season,
      "matchType" -> matchType,
     "match_date"->resultRow.getCell(4).getDateCellValue,
     "location"->resultRow.getCell(5).getStringCellValue,
      "toss_result"->toss_result,
      "toss_decision"->toss_decision,
      "winner" -> winner,
      "details" -> resultRow.getCell(9).getStringCellValue,
    )


    val scoresMap = scoreSheet.drop(1).map(row => {
      var batCredits = 0
      var bowlCredits = 0
      var fieldCredits = 0

      val playerName = row.getCell(0).getStringCellValue
      val runsScored = row.getCell(1).getNumericCellValue.toLong
      val ballsPlayed = row.getCell(2).getNumericCellValue.toLong
      val runsGiven = row.getCell(6).getNumericCellValue.toLong
      val oversBowled = row.getCell(4).getNumericCellValue.toLong
      val dismissalType = row.getCell(3).getStringCellValue
      val catches_dropped = row.getCell(8).getNumericCellValue.toInt


      val isNotOut = if (dismissalType.equalsIgnoreCase("notout")) {
        batCredits = batCredits + 1
        true
      } else false
      val IsDuck = if (runsScored == 0) {
        batCredits = batCredits - 1
        true
      } else false
      val economyRate = if (oversBowled != 0 && runsGiven != 0) runsGiven.toDouble / oversBowled else 0
      val srBat = (runsScored.toFloat / ballsPlayed) * 100
      val SR = BigDecimal(srBat).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
      val sixes = row.getCell(10).getNumericCellValue.toLong
      val fours = row.getCell(11).getNumericCellValue.toLong

      val wickets_Taken = row.getCell(5).getNumericCellValue.toInt
      bowlCredits = bowlCredits + wickets_Taken

      val No_Balls = row.getCell(12).getNumericCellValue.toLong
      val wide_Balls = row.getCell(13).getNumericCellValue.toLong

      val Maiden_overs = row.getCell(14).getNumericCellValue.toInt
      bowlCredits = bowlCredits + Maiden_overs

      val catches_taken = row.getCell(7).getNumericCellValue.toInt
      fieldCredits = fieldCredits + catches_taken

      "catches_dropped" -> catches_dropped
      fieldCredits = fieldCredits - catches_dropped

      val run_outs = row.getCell(9).getNumericCellValue.toInt
      fieldCredits = fieldCredits + run_outs

      val player = PlayerHelper.getMongoPlayerData(playerName)

      val playerId = player.map(i => i.getAsOrElse("player_id", ""))

      val statsByPlayerOptional = StatsHelper.getMongoStatsData(playerId.getOrElse(""))


     val statsByPlayer: Stats =  if(statsByPlayerOptional.isDefined) statsByPlayerOptional.get else null

    /*  val totalScore = if(statsByPlayer.isDefined){
        val totalRuns = statsByPlayer.get.getAs[Int]("batting_stats.runs").getOrElse(0)
        totalRuns + runsScored
      }else{
        runsScored
      }*/


      // Bat stats
      val totalScore = if (statsByPlayer != null) statsByPlayer.batting_stats.runs + runsScored else runsScored
      val totalBalls = if (statsByPlayer != null) statsByPlayer.batting_stats.balls + ballsPlayed else ballsPlayed
      val totalMatches = if (statsByPlayer != null) statsByPlayer.batting_stats.matches_played + 1 else 1

      val totalInningsBat =
        if (statsByPlayer != null) {
          if (ballsPlayed > 0) statsByPlayer.batting_stats.innings_played + 1 else statsByPlayer.batting_stats.innings_played
        } else if (ballsPlayed > 0) 1 else 0

      val totalNotOuts =
        if (statsByPlayer != null) {
          if (isNotOut) statsByPlayer.batting_stats.not_outs + 1 else statsByPlayer.batting_stats.not_outs
        } else if (isNotOut) 1 else 0

      val highestScore = if (statsByPlayer != null) { if (statsByPlayer.batting_stats.highest_score < runsScored) runsScored
      else statsByPlayer.batting_stats.highest_score } else runsScored

      val totalSrBat = if(totalBalls!= 0) (totalScore.toFloat / totalBalls) * 100 else 0
      val totalSrBatting = BigDecimal(totalSrBat).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
      val totalOuts = (totalInningsBat - totalNotOuts)
      val battingAverage = if((totalOuts)!=0)totalScore / totalOuts else 0

      val isHundred = if (runsScored >= 100) {
        batCredits = batCredits + 3
        true
      } else false

      val totalHundreds =
        if (statsByPlayer != null) {
          if (isHundred)
            statsByPlayer.batting_stats.hundreds + 1

          else
            statsByPlayer.batting_stats.hundreds
        } else if (isHundred) 1 else 0

      val isFifty = if (runsScored >= 50 && runsScored < 100) {
        batCredits = batCredits + 2
        true
      } else false

      val totalFifties =
        if (statsByPlayer != null) {
          if (isFifty)
            statsByPlayer.batting_stats.fifties + 1
          else
            statsByPlayer.batting_stats.fifties
        } else if (isFifty) 1 else 0


      val isThirty = if (runsScored >= 30 && runsScored < 50) {
        batCredits = batCredits + 1
        true
      } else false

      val totalThirties =
        if (statsByPlayer != null) {
          if (isThirty)
            statsByPlayer.batting_stats.thirties + 1
          else
            statsByPlayer.batting_stats.thirties
        } else if (isThirty) 1 else 0


      val totalDucks = if (statsByPlayer != null) {
        if (IsDuck) statsByPlayer.batting_stats.duck_outs + 1 else statsByPlayer.batting_stats.duck_outs
      } else if (IsDuck) 1 else 0

      val totalFours = if (statsByPlayer != null) statsByPlayer.batting_stats.fours + fours else fours

      val totalSixes = if (statsByPlayer != null) statsByPlayer.batting_stats.sixes + sixes else sixes

      //Bowling Stats

      val totalInningsBowled = if (statsByPlayer != null) {
        if (oversBowled > 0) statsByPlayer.bowling_stats.innings_played + 1 else statsByPlayer.bowling_stats.innings_played
      } else if (oversBowled > 0) 1 else 0

      val totalOversBowled = if (statsByPlayer != null) statsByPlayer.bowling_stats.overs + oversBowled else oversBowled

      val totalRunsGiven = if (statsByPlayer != null) statsByPlayer.bowling_stats.runs + runsGiven else runsGiven

      val totalWicketsTaken = if (statsByPlayer != null) statsByPlayer.bowling_stats.wickets + wickets_Taken else wickets_Taken


      val bowlingAverage = if(totalWicketsTaken!=0) totalRunsGiven / totalWicketsTaken else 0

      val totalEconomyRate = if (totalInningsBowled != 0 && totalRunsGiven != 0) totalRunsGiven.toDouble / totalOversBowled else 0


      val totalSrBowl = if(totalWicketsTaken!=0) (totalRunsGiven / totalWicketsTaken) * 100 else 0
      val totalSrBowling = BigDecimal(totalSrBowl).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

      val isThreeWickets = if (wickets_Taken == 3) {
        bowlCredits = bowlCredits + 1
        true
      } else false

      val totalThreeWickets = if (statsByPlayer != null) {
        if
        (isThreeWickets) statsByPlayer.bowling_stats.three_wickets + 1
        else
          statsByPlayer.bowling_stats.three_wickets
      } else if (isThreeWickets) 1 else 0

      val isFourWickets = if (wickets_Taken == 4) {
        bowlCredits = bowlCredits + 2
        true
      } else false

      val totalFourWickets = if (statsByPlayer != null) {
        if
        (isFourWickets) statsByPlayer.bowling_stats.four_wickets + 1
        else
          statsByPlayer.bowling_stats.four_wickets
      } else if (isFourWickets) 1 else 0

      val isFiveWickets = if (wickets_Taken >= 5) {
        bowlCredits = bowlCredits + 3
        true
      } else false

      val totalFiveWickets = if (statsByPlayer != null) {
        if (isFiveWickets) statsByPlayer.bowling_stats.five_wickets + 1
        else
          statsByPlayer.bowling_stats.five_wickets
      } else if (isFiveWickets) 1 else 0

      val totalWides = if (statsByPlayer != null) statsByPlayer.bowling_stats.wides + wide_Balls else wide_Balls

      val totalNoBalls = if (statsByPlayer != null) statsByPlayer.bowling_stats.no_balls + No_Balls else No_Balls

      //fielding Stats

      val totalCatchesTaken = if (statsByPlayer != null) statsByPlayer.fielding_stats.catches_taken + catches_taken else catches_taken

      val totalCatchesDroped = if (statsByPlayer != null) statsByPlayer.fielding_stats.catches_dropped + catches_dropped else catches_dropped

      val totalRunOuts = if (statsByPlayer != null) statsByPlayer.fielding_stats.run_outs + run_outs else run_outs

      //Credits

      val totalWins = if (statsByPlayer != null) {
        if (winner.equalsIgnoreCase(homeTeam))
          statsByPlayer.wins + 1
        else
          statsByPlayer.wins
      } else if (winner.equalsIgnoreCase(homeTeam)) 1 else 0


      val totalBatCredits = if (statsByPlayer != null) statsByPlayer.batting_stats.credits + batCredits else batCredits

      val totalBowlCredits = if (statsByPlayer != null) statsByPlayer.bowling_stats.credits + bowlCredits else bowlCredits

      val totalFieldCredits = if (statsByPlayer != null) statsByPlayer.fielding_stats.credits + fieldCredits else fieldCredits


      val batStats = DBObject(

        "matches_played" -> totalMatches,
        "innings_played" -> totalInningsBat,
        "not_outs" -> totalNotOuts,
        "runs" -> totalScore,
        "balls" -> totalBalls,
        "average" -> battingAverage,
        "strike_rate" -> totalSrBatting,
        "highest_score" -> highestScore,
        "hundreds" -> totalHundreds,
        "fifties" -> totalFifties,
        "thirties" -> totalThirties,
        "duck_outs" -> totalDucks,
        "fours" -> totalFours,
        "sixes" -> totalSixes,
        "credits" -> batCredits
      )
      val bowlStats = DBObject(

        "matches_played" -> totalMatches,
        "innings_played" -> totalInningsBowled,
        "overs" -> totalOversBowled,
        "runs" -> totalRunsGiven,
        "wickets" -> totalWicketsTaken,
        "average" -> bowlingAverage,
        "economy" -> totalEconomyRate,
        "strike_rate" -> totalSrBowling,
        "three_wickets" -> totalThreeWickets,
        "four_wickets" -> totalFourWickets,
        "five_wickets" -> totalFiveWickets,
        "wides" -> totalWides,
        "no_balls" -> totalNoBalls,
        "credits" -> totalBowlCredits
      )


      val fieldStats = DBObject(
        "catches_taken" -> totalCatchesTaken,
        "catches_dropped" -> totalCatchesDroped,
        "run_outs" -> totalRunOuts,
        "credits" -> fieldCredits
      )

      val stats = DBObject(
        "playerId" -> playerId,
        "batting_stats" -> batStats,
        "bowling_stats" -> bowlStats,
        "fielding_stats" -> fieldStats,
        "wins" -> totalWins
      )

      //update instead of save
      MongoConnector.stats.save(stats)

      val batting = DBObject(
          "runs_scored" -> runsScored,
          "balls_played" -> ballsPlayed,
          "dismissal_type" -> dismissalType,
          "SR" ->SR,
          "sixes" ->sixes,
          "fours" ->fours,
         "IsNotOut"-> isNotOut,
          "IsDuck"->IsDuck
      )

      val bowling=DBObject(
        "overs_bowled" ->oversBowled,
        "wickets_Taken" -> wickets_Taken,
        "runs_given" ->runsGiven,
        "Economy_Rate" ->economyRate,
        "No_Balls"->No_Balls,
        "wide_Balls"->wide_Balls,
        "wickets_Taken"->wickets_Taken,
        "Maiden_overs"->Maiden_overs
      )
      val fielding=DBObject(
        "catches_taken" -> catches_taken,
        "catches_dropped" -> catches_dropped,
        "run_outs" -> run_outs
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
