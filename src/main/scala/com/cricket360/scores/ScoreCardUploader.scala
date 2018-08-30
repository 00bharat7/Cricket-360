package com.cricket360.scores
import java.io.File

import com.cricket360.connector.MongoConnector
import com.cricket360.players.Player
import org.apache.poi.ss.usermodel.WorkbookFactory
import com.mongodb.casbah.Imports._
import net.liftweb.json._
import net.liftweb.json.Serialization.write

import collection.JavaConversions._
import scala.util.{Failure, Success}

/**
  * Created by bkasinadhuni on 8/27/18.
  */
object ScoreCardUploader {

  def main(args: Array[String]): Unit = {

    println("uploading score card")
    uploadScorecard("/Users/bkasinadhuni/Documents/ScoreCard-Willowwarriors.xlsx")

  }

  def uploadScorecard(fileLocation: String) = {
    implicit val formats = DefaultFormats

    val f = new File(fileLocation)
    val workbook = WorkbookFactory.create(f)
    val sheet = workbook.getSheetAt(0)

    sheet.drop(1).foreach(row => {
      val playerName = row.getCell(0).getStringCellValue

      try {
        val player = getMongoPlayerData(playerName)

        if (player.isDefined) {
          val opp_team = row.getCell(1).getStringCellValue
          val season = row.getCell(11).getStringCellValue

          val scoresMap = DBObject(
            "player" -> player,
            "opp_team" -> opp_team,
            "runs_scored" -> row.getCell(2).getNumericCellValue.toLong,
            "balls_played" -> row.getCell(3).getNumericCellValue.toLong,
            "dismissal_type" -> row.getCell(4).getStringCellValue,
            "overs_bowled" -> row.getCell(5).getNumericCellValue.toLong,
            "wickets" -> row.getCell(6).getNumericCellValue.toLong,
            "runs_given" -> row.getCell(7).getNumericCellValue.toLong,
            "catches_taken" -> row.getCell(8).getNumericCellValue.toLong,
            "catches_dropped" -> row.getCell(9).getNumericCellValue.toLong,
            "run_outs" -> row.getCell(10).getNumericCellValue.toLong,
            "season" -> season
          )

          MongoConnector.scores.save(scoresMap)

          println(s"Successfully uploaded Score of ${playerName} for Renegades vs ${opp_team} $season")

          //push to mongo scores table
        }
      }catch{
        case e: Exception => println(new Exception(e.getMessage))
      }
    })

  }

  def getMongoPlayerData(playerName: String) = {
    val player = MongoConnector.players.findOne(MongoDBObject("alias" -> MongoDBObject("$regex" -> playerName.toLowerCase)))

    player.map(i => DBObject(
      "player_id" -> i.getAsOrElse[String]("player_id", ""),
      "first_name" -> i.getAsOrElse[String]("first_name", ""),
      "last_name" -> i.getAsOrElse[String]("last_name", ""),
      "email" -> i.getAsOrElse[String]("email", ""),
      "phone" -> i.getAsOrElse[Double]("phone", 0.0),
      "style" -> i.getAsOrElse[String]("style", ""),
      "alias" ->i.getAsOrElse[String]("alias", "")
    ))
  }
}
