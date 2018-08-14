package com.neo.sk.carnie.paper

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:40 PM
  */
object Protocol {

  sealed trait GameMessage

  case class GridDataSync(
                           frameCount: Long,
                           snakes: List[SkDt],
                           bodyDetails: List[Bd],
                           fieldDetails: List[Fd],
                           borderDetails: List[Bord],
                           killHistory: List[Kill]
                         ) extends GameMessage

  case class Data4Sync(
                           frameCount: Long,
                           snakes: List[SkDt],
                           bodyDetails: List[Bd],
                           fieldDetails: List[Fd],
                           blankDetails: List[Point],
                           killHistory: List[Kill]
                         ) extends GameMessage

  case class TextMsg(
                      msg: String
                    ) extends GameMessage

  case class InitInfo(id: Long, gridData: GridDataSync) extends GameMessage

  case class NewSnakeJoined(id: Long, name: String) extends GameMessage

  case class SnakeAction(id: Long, keyCode: Int, frame: Long) extends GameMessage

  case class SnakeLeft(id: Long, name: String) extends GameMessage

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends GameMessage

  case class NetDelayTest(createTime: Long) extends GameMessage

  case class SomeOneWin(winnerName: String) extends GameMessage


  sealed trait UserAction

  case class Key(id: Long, keyCode: Int, frameCount: Long) extends UserAction

  case class NetTest(id: Long, createTime: Long) extends UserAction

  case class TextInfo(msg: String) extends UserAction

  val frameRate = 120

}