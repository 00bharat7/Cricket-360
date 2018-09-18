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
    val tossResult =resultRow.getCell(6).getStringCellValue
    val tossDecision =resultRow.getCell(7).getStringCellValue
    val winner = resultRow.getCell(8).getStringCellValue

   val resultObject =  DBObject(
      "homeTeam"->homeTeam,
      "oppTeam" -> oppTeam,
      "season" -> season,
      "matchType" -> matchType,
      "matchDate"->resultRow.getCell(4).getDateCellValue,
      "location"->resultRow.getCell(5).getStringCellValue,
      "tossResult"->tossResult,
      "tossDecision"->tossDecision,
      "winner" -> winner,
      "details" -> resultRow.getCell(9).getStringCellValue
    )


    val scoresMap = scoreSheet.drop(1).map(row => {
      var batCredits = 0
      var bowlCredits = 0
      var fieldCredits = 0

      val playerName = row.getCell(0).getStringCellValue
      val runsScored = row.getCell(1).getNumericCellValue.toInt
      val ballsPlayed =row.getCell(2).getNumericCellValue.toInt
      val runsGiven=row.getCell(6).getNumericCellValue.toInt
      val oversBowled =row.getCell(4).getNumericCellValue.toInt
      val dismissalType = row.getCell(3).getStringCellValue


      val isNotOut = if (dismissalType.equalsIgnoreCase("notout")) {
        batCredits = batCredits + 1
        true
      } else false
      val isDuck = if (runsScored == 0) {
        batCredits = batCredits - 1
        true
      } else false

      val economyRate = if (oversBowled != 0 && runsGiven != 0) runsGiven.toDouble / oversBowled else 0
      val srBat = (runsScored.toFloat / ballsPlayed) * 100
      val SR = BigDecimal(srBat).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
      val sixes = row.getCell(10).getNumericCellValue.toLong
      val fours = row.getCell(11).getNumericCellValue.toLong

      val wicketsTaken = row.getCell(5).getNumericCellValue.toInt
      bowlCredits = bowlCredits + wicketsTaken

      val noBalls = row.getCell(12).getNumericCellValue.toLong
      val wideBalls = row.getCell(13).getNumericCellValue.toLong

      val maidenOvers = row.getCell(14).getNumericCellValue.toInt
      bowlCredits = bowlCredits + maidenOvers

      val catchesTaken = row.getCell(7).getNumericCellValue.toInt
      fieldCredits = fieldCredits + catchesTaken


      val catchesDropped = row.getCell(8).getNumericCellValue.toInt
      fieldCredits = fieldCredits - catchesDropped

      val runOuts = row.getCell(9).getNumericCellValue.toInt
      fieldCredits = fieldCredits + runOuts

      val player = PlayerHelper.getMongoPlayerData(playerName)

      val playerId = player.map(i => i.getAsOrElse("playerId", ""))

      val statsByPlayerOptional = StatsHelper.getMongoStatsData(playerId.getOrElse(""))


     val statsByPlayer: Stats =  if(statsByPlayerOptional.isDefined) statsByPlayerOptional.get else null

      // Bat stats
      val totalScore = if (statsByPlayer != null) statsByPlayer.battingStats.runs + runsScored else runsScored
      val totalBalls = if (statsByPlayer != null) statsByPlayer.battingStats.balls + ballsPlayed else ballsPlayed
      val totalMatches = if (statsByPlayer != null) statsByPlayer.battingStats.matchesPlayed + 1 else 1

      val totalInningsBat =
        if (statsByPlayer != null) {
          if (ballsPlayed > 0) statsByPlayer.battingStats.inningsPlayed + 1 else statsByPlayer.battingStats.inningsPlayed
        } else if (ballsPlayed > 0) 1 else 0

      val totalNotOuts =
        if (statsByPlayer != null) {
          if (isNotOut) statsByPlayer.battingStats.notOuts + 1 else statsByPlayer.battingStats.notOuts
        } else if (isNotOut) 1 else 0

      val highestScore = if (statsByPlayer != null) { if (statsByPlayer.battingStats.highestScore < runsScored) runsScored
      else statsByPlayer.battingStats.highestScore } else runsScored

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
            statsByPlayer.battingStats.hundreds + 1

          else
            statsByPlayer.battingStats.hundreds
        } else if (isHundred) 1 else 0

      val isFifty = if (runsScored >= 50 && runsScored < 100) {
        batCredits = batCredits + 2
        true
      } else false

      val totalFifties =
        if (statsByPlayer != null) {
          if (isFifty)
            statsByPlayer.battingStats.fifties + 1
          else
            statsByPlayer.battingStats.fifties
        } else if (isFifty) 1 else 0


      val isThirty = if (runsScored >= 30 && runsScored < 50) {
        batCredits = batCredits + 1
        true
      } else false

      val totalThirties =
        if (statsByPlayer != null) {
          if (isThirty)
            statsByPlayer.battingStats.thirties + 1
          else
            statsByPlayer.battingStats.thirties
        } else if (isThirty) 1 else 0


      val totalDucks = if (statsByPlayer != null) {
        if (isDuck) statsByPlayer.battingStats.duckOuts + 1 else statsByPlayer.battingStats.duckOuts
      } else if (isDuck) 1 else 0

      val totalFours = if (statsByPlayer != null) statsByPlayer.battingStats.fours + fours else fours

      val totalSixes = if (statsByPlayer != null) statsByPlayer.battingStats.sixes + sixes else sixes

      //Bowling Stats

      val totalInningsBowled = if (statsByPlayer != null) {
        if (oversBowled > 0) statsByPlayer.bowlingStats.inningsPlayed + 1 else statsByPlayer.bowlingStats.inningsPlayed
      } else if (oversBowled > 0) 1 else 0

      val totalOversBowled = if (statsByPlayer != null) statsByPlayer.bowlingStats.overs + oversBowled else oversBowled

      val totalRunsGiven = if (statsByPlayer != null) statsByPlayer.bowlingStats.runs + runsGiven else runsGiven

      val totalWicketsTaken = if (statsByPlayer != null) statsByPlayer.bowlingStats.wickets + wicketsTaken else wicketsTaken


      val bowlingAverage = if(totalWicketsTaken!=0) totalRunsGiven / totalWicketsTaken else 0

      val totalEconomyRate = if (totalInningsBowled != 0 && totalRunsGiven != 0) totalRunsGiven.toDouble / totalOversBowled else 0


      val totalSrBowl = if(totalWicketsTaken!=0) (totalRunsGiven / totalWicketsTaken) * 100 else 0
      val totalSrBowling = BigDecimal(totalSrBowl).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

      val isThreeWickets = if (wicketsTaken == 3) {
        bowlCredits = bowlCredits + 1
        true
      } else false

      val totalThreeWickets = if (statsByPlayer != null) {
        if
        (isThreeWickets) statsByPlayer.bowlingStats.threeWicketHaul + 1
        else
          statsByPlayer.bowlingStats.threeWicketHaul
      } else if (isThreeWickets) 1 else 0

      val isFourWickets = if (wicketsTaken == 4) {
        bowlCredits = bowlCredits + 2
        true
      } else false

      val totalFourWickets = if (statsByPlayer != null) {
        if
        (isFourWickets) statsByPlayer.bowlingStats.fourWicketHaul + 1
        else
          statsByPlayer.bowlingStats.fourWicketHaul
      } else if (isFourWickets) 1 else 0

      val isFiveWickets = if (wicketsTaken >= 5) {
        bowlCredits = bowlCredits + 3
        true
      } else false

      val totalFiveWickets = if (statsByPlayer != null) {
        if (isFiveWickets) statsByPlayer.bowlingStats.fiveWicketHaul + 1
        else
          statsByPlayer.bowlingStats.fiveWicketHaul
      } else if (isFiveWickets) 1 else 0

      val totalWides = if (statsByPlayer != null) statsByPlayer.bowlingStats.wides + wideBalls else wideBalls

      val totalNoBalls = if (statsByPlayer != null) statsByPlayer.bowlingStats.noBalls + noBalls else noBalls

      //fielding Stats

      val totalCatchesTaken = if (statsByPlayer != null) statsByPlayer.fieldingStats.catchesTaken + catchesTaken else catchesTaken

      val totalCatchesDropped = if (statsByPlayer != null) statsByPlayer.fieldingStats.catchesDropped + catchesDropped else catchesDropped

      val totalRunOuts = if (statsByPlayer != null) statsByPlayer.fieldingStats.runOuts + runOuts else runOuts

      //Credits

      val totalWins = if (statsByPlayer != null) {
        if (winner.equalsIgnoreCase(homeTeam))
          statsByPlayer.wins + 1
        else
          statsByPlayer.wins
      } else if (winner.equalsIgnoreCase(homeTeam)) 1 else 0


      val totalBatCredits = if (statsByPlayer != null) statsByPlayer.battingStats.credits + batCredits else batCredits

      val totalBowlCredits = if (statsByPlayer != null) statsByPlayer.bowlingStats.credits + bowlCredits else bowlCredits

      val totalFieldCredits = if (statsByPlayer != null) statsByPlayer.fieldingStats.credits + fieldCredits else fieldCredits


      val batStats = DBObject(

        "matchesPlayed" -> totalMatches,
        "inningsPlayed" -> totalInningsBat,
        "notOuts" -> totalNotOuts,
        "runs" -> totalScore,
        "balls" -> totalBalls,
        "average" -> battingAverage,
        "strikeRate" -> totalSrBatting,
        "highestScore" -> highestScore,
        "hundreds" -> totalHundreds,
        "fifties" -> totalFifties,
        "thirties" -> totalThirties,
        "duckOuts" -> totalDucks,
        "fours" -> totalFours,
        "sixes" -> totalSixes,
        "credits" -> batCredits
      )
      val bowlStats = DBObject(

        "matchesPlayed" -> totalMatches,
        "inningsPlayed" -> totalInningsBowled,
        "overs" -> totalOversBowled,
        "runs" -> totalRunsGiven,
        "wickets" -> totalWicketsTaken,
        "average" -> bowlingAverage,
        "economy" -> totalEconomyRate,
        "strikeRate" -> totalSrBowling,
        "threeWicketHaul" -> totalThreeWickets,
        "fourWicketHaul" -> totalFourWickets,
        "fiveWicketHaul" -> totalFiveWickets,
        "wides" -> totalWides,
        "noBalls" -> totalNoBalls,
        "credits" -> totalBowlCredits
      )


      val fieldStats = DBObject(
        "catchesTaken" -> totalCatchesTaken,
        "catchesDropped" -> totalCatchesDropped,
        "runOuts" -> totalRunOuts,
        "credits" -> fieldCredits
      )

      val totalCredits = batCredits + bowlCredits + fieldCredits

      val stats = DBObject(
        "playerId" -> playerId,
        "battingStats" -> batStats,
        "bowlingStats" -> bowlStats,
        "fieldingStats" -> fieldStats,
        "wins" -> totalWins,
        "totalCredits" -> totalCredits
      )

      //update instead of save if current gameId already exists
      MongoConnector.stats.save(stats)

      val batting = DBObject(
          "runsScored" -> runsScored,
          "ballsPlayed" -> ballsPlayed,
          "dismissalType" -> dismissalType,
          "SR" ->SR,
          "sixes" ->sixes,
          "fours" ->fours,
          "isNotOut"-> isNotOut,
          "isDuck"->isDuck
      )

      val bowling=DBObject(
        "oversBowled" ->oversBowled,
        "wicketsTaken" -> wicketsTaken,
        "runsGiven" ->runsGiven,
        "economyRate" ->economyRate,
        "noBalls"->noBalls,
        "wideBalls"->wideBalls,
        "maidenOvers"->maidenOvers
      )
      val fielding=DBObject(
        "catchesTaken" -> catchesTaken,
        "catchesDropped" -> catchesDropped,
        "runOuts" -> runOuts
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
        "updatedAt" -> DateTime.now.toDate
      )

      //update instead of save if current gameId already exists
      MongoConnector.scores.save(scoreObject)
      println(s"Successfully uploaded Score for $homeTeam vs $oppTeam")
    }catch{
      case e: Exception => println(new Exception(e.getMessage))
    }
  }
}
