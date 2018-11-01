package com.neo.sk.carnie.http

import java.io.File
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity}
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream.scaladsl.{FileIO, Sink, Source }
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.HttpEntity
import com.neo.sk.carnie.ptcl.RoomApiProtocol._
import com.neo.sk.carnie.core.RoomManager
import com.neo.sk.utils.CirceSupport
import com.neo.sk.carnie.Boot.scheduler
import com.neo.sk.carnie.models.dao.RecordDAO
import com.neo.sk.carnie.core.TokenActor.AskForToken

import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import scala.concurrent.Future
import io.circe.Error

import scala.collection.mutable


/**
  * Created by dry on 2018/10/18.
  **/
trait RoomApiService extends ServiceUtils with CirceSupport with PlayerService with EsheepService{

  private val log = LoggerFactory.getLogger(this.getClass)

  val roomManager: akka.actor.typed.ActorRef[RoomManager.Command]


  private val getRoomId = (path("getRoomId") & post & pathEndOrSingleSlash) {
    dealPostReq[PlayerIdInfo]{ req =>
            val msg: Future[Option[(Int, mutable.HashSet[(String, String)])]] = roomManager ? (RoomManager.FindRoomId(req.playerId, _))
            msg.map {
              case Some(rid) => complete(RoomIdRsp(RoomIdInfo(rid._1)))
              case _ =>
                log.info("this player doesn't exist")
                complete(ErrorRsp(100010, "get roomId error:this player doesn't exist"))
            }
    }
  }

  private val getRoomPlayerList = (path("getRoomPlayerList") & post & pathEndOrSingleSlash) {
    dealPostReq[RoomIdReq]{req =>
          val msg: Future[Option[List[(String, String)]]] = roomManager ? (RoomManager.FindPlayerList(req.roomId, _))
            msg.map {
              case Some(plist) =>
                complete(PlayerListRsp(PlayerInfo(plist.map(p => PlayerIdName(p._1,p._2)))))
              case None =>
                log.info("get player list error")
                complete(ErrorRsp(100001, "get player list error"))
            }
    }
  }



  private val getRoomList = (path("getRoomList") & post & pathEndOrSingleSlash) {
    dealPostReq[AllRoomReq] { req =>
      val msg: Future[List[Int]] = roomManager ? (RoomManager.FindAllRoom(_))
      msg.map {
        allRoom =>
          if (allRoom.nonEmpty)
            complete(RoomListRsp(RoomListInfo(allRoom)))
          else {
            log.info("get all room error,there are no rooms")
            complete(ErrorRsp(100000, "get all room error,there are no rooms"))
          }
      }
    }
  }

  private val getRecordList = (path("getRecordList") & post & pathEndOrSingleSlash) {
    dealPostReq[RecordListReq]{ req =>
            RecordDAO.getRecordList(req.lastRecordId,req.count).map{recordL =>
              complete(RecordListRsp(records(recordL.toList.map(_._1).distinct.map{ r =>
                val userList = recordL.map(i => i._2).distinct.filter(_.recordId == r.recordId).map(_.userId)
                recordInfo(r.recordId,r.roomId,r.startTime,r.endTime,userList.length,userList)
              })))
            }
    }
  }

  private val getRecordListByTime = (path("getRecordListByTime") & post & pathEndOrSingleSlash) {
   dealPostReq[RecordByTimeReq]{ req =>
           RecordDAO.getRecordListByTime(req.startTime,req.endTime,req.lastRecordId,req.count).map{recordL =>
             complete(RecordListRsp(records(recordL.toList.map(_._1).distinct.map{ r =>
               val userList = recordL.map(i => i._2).distinct.filter(_.recordId == r.recordId).map(_.userId)
               recordInfo(r.recordId,r.roomId,r.startTime,r.endTime,userList.length,userList)
             })))
           }
   }
  }

  private val getRecordListByPlayer = (path("getRecordListByPlayer") & post & pathEndOrSingleSlash) {
  dealPostReq[RecordByPlayerReq]{ req =>
          RecordDAO.getRecordListByPlayer(req.playerId,req.lastRecordId,req.count).map{recordL =>
            complete(RecordListRsp(records(recordL.toList.filter(_._2.userId == req.playerId).map(_._1).distinct.map{ r =>
              val userList = recordL.map(i => i._2).distinct.filter(_.recordId == r.recordId).map(_.userId)
              recordInfo(r.recordId,r.roomId,r.startTime,r.endTime,userList.length,userList)
            })))
          }
        }
  }

  private val downloadRecord = (path("downloadRecord" ) & post & pathEndOrSingleSlash) {parameter(
    'token.as[String]
  ){
    token =>
      dealFutureResult {
      val msg: Future[String] = tokenActor ? AskForToken
      msg.map {
        nowToken =>
          if(token == nowToken){
            entity(as[Either[Error, RecordReq]]) {
              case Right(req) =>
                dealFutureResult{
                  RecordDAO.getRecordPath(req.recordId).map{
                    case Some(file) =>
                      val f = new File(file)
                      println(s"getFile $file")
                      if (f.exists()) {
                        val responseEntity = HttpEntity(
                          ContentTypes.`application/octet-stream`,
                          f.length,
                          FileIO.fromPath(f.toPath, chunkSize = 262144))
                        complete(responseEntity)
                      }
                      else
                        complete(ErrorRsp(1001011,"record doesn't exist."))
                    case None =>
                      complete(ErrorRsp(1001011,"record id failed."))
                  }
                }

              case Left(error) =>
                log.warn(s"some error: $error")
                complete(ErrorRsp(0, ""))
            }
          }
          else {
            log.warn("token error")
            complete(ErrorRsp(1100111, "token error"))
          }
      }
    }

  }
  }


  val roomApiRoutes: Route =  {
    getRoomId ~ getRoomPlayerList ~ getRoomList ~ getRecordList ~ getRecordListByTime ~
    getRecordListByPlayer ~ downloadRecord
  }


}