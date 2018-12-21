package com.neo.sk.carnie.paperClient

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:40 PM
  */
object Protocol {

  sealed trait GameMessage extends WsSourceProtocol.WsMsgSource


//  case class GridDataSync(
//                           frameCount: Long,
//                           snakes: List[SkDt],
//                           bodyDetails: List[Bd],
//                           fieldDetails: List[Fd],
//                           borderDetails: List[Bord],
//                           killHistory: List[Kill]
//                         ) extends GameMessage

  case class NewFieldInfo(
                           frameCount: Long,
                           fieldDetails: List[FieldByColumn]
                         ) extends GameMessage

  case class Data4TotalSync(
                             frameCount: Long,
                             snakes: List[SkDt],
                             bodyDetails: List[BodyBaseInfo],
                             fieldDetails: List[FieldByColumn]
//                             killHistory: List[Kill]
                           ) extends GameMessage

  case class Data4TotalSyncNoField(
                             frameCount: Long,
                             snakes: List[SkDt],
                             bodyDetails: List[BodyBaseInfo]
//                             fieldDetails: List[FieldByColumn]
                           ) extends GameMessage

  case class NewSnakeInfo(
                         frameCount: Long,
                         snake: List[SkDt],
                         filedDetails: List[FieldByColumn]
                         ) extends GameMessage

  case class KilledSkData(
                         killedSkInfo: List[KilledSkDt]
                         )

  case class Point4Trans(x: Int, y: Int)

  case class BodyBaseInfo(
                           uid: String,
                           turn: TurnInfo
                         )

  case class TurnInfo(
                       turnPoint: List[Point4Trans],
                       pointOnField: List[(Point4Trans, String)]
                     ) // (拐点顺序list， 占着别人领域的身体点)

  case class FieldByColumn(
                            uid: String,
                            scanField: List[ScanByColumn]
                          )

  case class ScanByColumn(
                           y: List[(Int, Int)],
                           x: List[(Int, Int)]
                         )

  case class TextMsg(
                      msg: String
                    ) extends GameMessage

  case class Id(id: String) extends GameMessage

  case class RoomId(roomId: String) extends GameMessage

  case class Id4Watcher(id: String, watcher: String) extends GameMessage

  case class StartWatching(mode: Int, img: Int) extends GameMessage

  case class Mode(mode: Int) extends GameMessage

  case class StartLoading(frame: Int) extends GameMessage

  case class StartReplay(firstSnapshotFrame: Int, firstReplayFrame: Int) extends GameMessage

  case class DeadPage(id: String, kill: Int, area: Int, startTime: Long, endTime: Long) extends GameMessage

  case class UserLeft(userId: String) extends GameMessage

  case class InitReplayError(info: String) extends GameMessage

  case class NewSnakeJoined(id: Long, name: String) extends GameMessage

  case class SnakeAction(id: String, keyCode: Int, frame: Long, actionId: Int) extends GameMessage

  case class SnakeLeft(id: String, name: String) extends GameMessage

  case class ReplayFinish(id: String) extends GameMessage

  //  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends GameMessage
  case class Ranks(currentRank: List[Score], personalScore: Score, personalRank: Int) extends GameMessage
//  case class Rank4Self(score: Score, rank: Int) extends GameMessage

  case object ReStartGame extends GameMessage

  case class SomeOneWin(winnerName: String, data: Data4TotalSync) extends GameMessage with GameEvent

  case class WinData(winnerScore: Int,yourScore: Option[Int]) extends GameMessage with GameEvent

  case class SomeOneKilled(killedId: String, killedName: String, killerName: String) extends GameMessage with GameEvent

  case class ReceivePingPacket(createTime: Long) extends GameMessage

  case class WinnerBestScore(Score: Int) extends GameMessage

  sealed trait WsSendMsg
  case object WsSendComplete extends WsSendMsg
  case class WsSendFailed(ex: Throwable) extends WsSendMsg

  sealed trait UserAction extends WsSendMsg

  case class Key(id: String, keyCode: Int, frameCount: Long, actionId: Int) extends UserAction

  case class TextInfo(msg: String) extends UserAction

  case class SendPingPacket(id: String, createTime: Long) extends UserAction

  case class NeedToSync(id: String) extends UserAction

  case object PressSpace extends UserAction

  //essf
  sealed trait GameEvent extends GameMessage

  case class JoinEvent(id: String, name: String) extends GameEvent

  case class LeftEvent(id: String, nickName: String) extends GameEvent

  case class SpaceEvent(id: String) extends GameEvent

  case class DirectionEvent(id: String, keyCode: Int) extends GameEvent

  case class EncloseEvent(fieldDetails: List[FieldByColumn]) extends GameEvent

  case class EventData(events: List[GameEvent]) extends GameEvent

  case class RankEvent(rank: List[Score]) extends GameEvent

  case class Snapshot(snakes: List[SkDt], bodyDetails: List[BodyBaseInfo], fieldDetails: List[FieldByColumn]) extends GameEvent

  case class DecodeError() extends GameEvent

  case class GameInformation(roomId: Int, startTime: Long, index: Int, initFrame: Long, mode: Int)

  case class UserJoinLeft(joinFrame: Long, leftFrame: Long)

  case class UserBaseInfo(id:String, name: String)

  case class EssfMapInfo(m:List[(UserBaseInfo, List[UserJoinLeft])])

  //for replay
//  sealed trait ReplayMessage extends WsSourceProtocol.WsMsgSource

  /**
    * replay-frame-msg*/
//  case class ReplayFrameData(frameIndex: Int, eventsData: Array[Byte], stateData: Option[Array[Byte]]) extends GameMessage
  case class ReplayFrameData(frameIndex: Int, eventsData: GameEvent, stateData: Option[GameEvent]) extends GameMessage

  val frameRate1 = 150 //normal-mode
  val frameRate2 = 75 //doubleSpeed-mode
  val maxContainableAction = 3


}
