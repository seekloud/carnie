package org.seekloud.carnie.paperClient

import org.seekloud.carnie.paperClient.Protocol.Data4TotalSync

/**
  * Created by dry on 2018/11/26.
  **/
object FrontProtocol {

  sealed trait DrawFunction

  case object DrawGameWait extends DrawFunction

  case class DrawGameWin(winnerName: String, winData: WinData4Draw) extends DrawFunction

  case object DrawGameOff extends DrawFunction

  case class DrawBaseGame(data: Data4Draw) extends DrawFunction

  case class DrawGameDie(killerName: Option[String], data: Option[Data4Draw] = None) extends DrawFunction

  case class Data4Draw(
                             frameCount: Int,
                             snakes: List[SkDt],
                             bodyDetails: List[BodyInfo4Draw],
                             fieldDetails: List[Field4Draw]
                           )

  case class WinData4Draw(
                        frameCount: Int,
                        snakes: List[SkDt],
                        fieldDetails: List[Field4Draw]
                      )

  case class BodyInfo4Draw(
                           uid: String,
                           turn: List[Protocol.Point4Trans]
                         )

  case class Field4Draw(
                            uid: String,
                            scanField: List[Scan4Draw]
                          )

  case class Scan4Draw(
                        x: Short,
                        y: List[(Short, Short)]
                       )
}
