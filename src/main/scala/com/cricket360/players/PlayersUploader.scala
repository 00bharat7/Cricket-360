package com.cricket360.players

import java.io.File

import com.cricket360.connector.MongoConnector

import collection.JavaConversions._
import com.mongodb.casbah.Imports._
import org.apache.poi.hssf.record.BlankRecord
import org.apache.poi.ss.usermodel.{Cell, DataFormatter, Row, WorkbookFactory}

import scala.util.{Failure, Success}

/**
  * Created by bkasinadhuni on 8/27/18.
  */
object PlayersUploader {

  def main(args: Array[String]): Unit = {
    val path = System.getProperty("players.location")
    uploadPlayerDetails(path)
  }

  def uploadPlayerDetails(fileLocation: String) = {

    val f = new File(fileLocation)
    val formatter = new DataFormatter
    val workbook = WorkbookFactory.create(f)
    val sheet = workbook.getSheetAt(0)

    try{
    sheet.drop(1).foreach(row => {

      val firstName = row.getCell(0).getStringCellValue
      val lastName = row.getCell(1).getStringCellValue
      val playerId = s"${firstName}_${lastName}"

      val mayBeEmail: Option[Cell] = Option(row.getCell(2))
      val mayBePhone: Option[Cell] = Option(row.getCell(3))


      val email: String = mayBeEmail match {
        case Some(cell) => cell.getStringCellValue
        case None => ""
      }

      val phone: Long = mayBePhone match {
        case Some(cell) => cell.getNumericCellValue.toLong
        case None => 0
      }

      val playersMap = DBObject(
        "player_id" -> playerId,
        "first_name" -> firstName,
        "last_name" -> lastName,
        "email" -> email,
        "phone" -> phone.toLong,
        "style" -> row.getCell(4).getStringCellValue,
        "alias" -> row.getCell(5).getStringCellValue
      )

      MongoConnector.players.save(playersMap)

      println(s"Successfully uploaded player details of ${playerId}")
    })
  }catch{
      case e: Exception => println(new Exception(e.getMessage))
    }
  }


}

case class Player(playerId: String, firstName: String, lastName: String, email: String, phone: String, style: String, alias: String)
