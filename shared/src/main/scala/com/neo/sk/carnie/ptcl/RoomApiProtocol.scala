package com.neo.sk.carnie.ptcl

/**
  * User: Jason
  * Date: 2018/10/19
  * Time: 14:37
  */
object RoomApiProtocol {

  trait CommonRsp {
    val errCode: Int
    val msg: String
  }

  case class RecordReq(
                            recordId: Long
  )
  case class RecordListReq(
                            lastRecordId: Long,
                            count: Int
  )

  case class RecordByTimeReq(
                            startTime: Long,
                            endTime: Long,
                            lastRecordId: Long,
                            count: Int
  )

  case class RecordByPlayerReq(
                          playerId: String,
                          lastRecordId: Long,
                          count: Int
  )
  case class RecordListRsp(
                          data:records,
                          errCode: Int = 0,
                          msg: String = "ok"
  )
  case class recordInfo(
                          recordId: Long,
                          roomId: Long,
                          startTime: Long,
                          endTime: Long,
                          userCounts: Int,
                          userList: Seq[String]
  )
  case class records(
                    recordList:List[recordInfo]
  )
  case class PlayerIdInfo(
                           playerId: String
                         )

  case class PlayerInfo(
                         playerList: List[PlayerIdName]
                       )

  case class PlayerIdName(
                        playerId: String,
                        nickName: String
  )

  case class RoomIdInfo(
                         roomId: Int
                       )

  case class RoomIdReq(
                        roomId: Int
                      )
  case class AllRoomReq(
                        data: String
  )

  case class RoomIdRsp(
                        data: RoomIdInfo,
                        errCode: Int = 0,
                        msg: String = "ok"
                      )

  case class PlayerListRsp(
                            data: PlayerInfo,
                            errCode: Int = 0,
                            msg: String = "ok"
                          )

  case class RoomListRsp(
                          data: RoomListInfo,
                          errCode: Int = 0,
                          msg: String = "ok"
                        )

  case class RoomListInfo(
                           roomList: List[Int]
                         )

  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp


}