package org.seekloud.carnie.http

import java.text.SimpleDateFormat

import akka.http.scaladsl.server.Directives.{getFromResource, path}
import akka.http.scaladsl.server.Route
import org.seekloud.utils.{CirceSupport, SessionSupport}
import org.slf4j.LoggerFactory
import akka.http.scaladsl.server.Directives._
import org.seekloud.carnie.ptcl.AdminPtcl.{PageReq,PageTimeReq}
import org.seekloud.carnie.ptcl.RoomApiProtocol._

import scala.collection.mutable
//import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity}
import akka.http.scaladsl.server.{Directive1, Route}
import org.seekloud.carnie.core.RoomManager
import io.circe.generic.auto._
import io.circe.Error
import org.seekloud.carnie.http.SessionBase.{AdminInfo, AdminSession}
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.Future
import org.seekloud.carnie.ptcl.AdminPtcl
import org.seekloud.carnie.Boot.{executor, scheduler}
import org.seekloud.carnie.common.AppSettings
import org.seekloud.carnie.models.dao.PlayerRecordDAO
//import scala.util.{Failure, Success}

/**
  * User: Jason
  * Date: 2018/12/17
  * Time: 16:29
  */
trait AdminService extends ServiceUtils
  with CirceSupport
  with SessionBase
  with SessionSupport
  with RoomApiService
{

  private val log = LoggerFactory.getLogger(this.getClass)


  private val login = (path("login") & post & pathEndOrSingleSlash){
    entity(as[Either[Error, AdminPtcl.LoginReq]]) {
      case Right(req) =>
        if(AppSettings.adminId == req.id && AppSettings.adminPassWord == req.passWord){
          setSession(
            AdminSession(AdminInfo(req.id, req.passWord), System.currentTimeMillis()).toAdminSessionMap
          ) { ctx =>
            ctx.complete(SuccessRsp())
          }
        }
        else {
          log.info("Administrator's account or password is wrong!")
          complete(ErrorRsp(140001, "Some errors happened in adminLogin!"))
        }
      case Left(e) =>
        complete(ErrorRsp(140001, s"Some errors happened in adminLogin：$e"))
    }
  }

//  private val getRoomList = (path("getRoomList") & get & pathEndOrSingleSlash){
//    adminAuth{ _ =>
//      dealFutureResult {
//        val msg: Future[List[String]] = roomManager ? RoomManager.FindAllRoom4Client
//        msg.map {
//          allRoom =>
//            log.info("prepare to return roomList.")
//            complete(RoomApiProtocol.RoomListRsp4Client(RoomListInfo4Client(allRoom)))
//        }
//      }
//    }
//  }

  private val getRoomPlayerList = (path("getRoomPlayerList") & get & pathEndOrSingleSlash) {
    adminAuth {
      _ =>
        //    val msg: Future[List[PlayerIdName]] = roomManager ? (RoomManager.FindPlayerList(req.roomId, _))
        val msg: Future[mutable.HashMap[Int, (Int, Option[String], mutable.HashSet[(String, String)])]] = roomManager ? RoomManager.ReturnRoomMap
        dealFutureResult {
          msg.map { plist =>
            //        val plist =r.map(i => i._1 -> i._2._3)
            complete(RoomMapRsp(RoomMapInfo(plist)))
          }
        }
    }
  }

  private val getPlayerRecord = (path("getPlayerRecord") & post & pathEndOrSingleSlash) {
    adminAuth {
      _ =>
        entity(as[Either[Error, PageReq]]) {
          case Right(req) =>
            val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            dealFutureResult {
              PlayerRecordDAO.getPlayerRecord().map { p =>
                complete(AdminPtcl.PlayerRecordRsp(p.toList.map { i =>
                  AdminPtcl.PlayerRecord(i.id, i.playerId, i.nickname, i.killing, i.killed,
                    i.score, i.startTime, i.endTime)
                }.slice((req.page - 1) * 5, req.page * 5), p.length
                ))
              }.recover {
                case e: Exception =>
                  log.info(s"getPlayerRecord exception.." + e.getMessage)
                  complete(ErrorRsp(130019, "getPlayerRecord error."))
              }
            }
          case Left(_) =>
            complete(ErrorRsp(130026, "parse error."))
        }
    }
  }

  private val getPlayerRecordAmount = (path("getPlayerRecordAmount") & get & pathEndOrSingleSlash) {
    adminAuth {
      _ =>
        val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = dateFormat.format(System.currentTimeMillis())
        val start = date.take(11) + "00:00:00"
        val end = date.take(11) + "23:59:59"
        dealFutureResult {
          PlayerRecordDAO.getPlayerRecord().map { p =>
            complete(AdminPtcl.PlayerAmountRsp(p.map(_.playerId).distinct.length,
              p.filter(i => dateFormat.format(i.startTime) >= start && dateFormat.format(i.endTime) <= end).map(_.playerId).distinct.length))
          }.recover {
            case e: Exception =>
              log.info(s"getPlayerRecordAmount exception.." + e.getMessage)
              complete(ErrorRsp(130020, "getPlayerRecordAmount error."))
          }
        }
    }
  }

  private val getPlayerRecordByTime = (path("getPlayerRecordByTime") & post & pathEndOrSingleSlash) {
    adminAuth {
      _ =>
        entity(as[Either[Error, PageTimeReq]]) {
          case Right(req) =>
            val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val startS = req.time.take(10) + " 00:00:00"
            val endS = req.time.take(10) + " 23:59:59"
            dealFutureResult {
              PlayerRecordDAO.getPlayerRecord().map { p =>
                complete(AdminPtcl.PlayerRecordRsp(p.toList.filter(i => dateFormat.format(i.startTime) >= startS && dateFormat.format(i.endTime) <= endS).map { i =>
                  AdminPtcl.PlayerRecord(i.id, i.playerId, i.nickname, i.killing, i.killed, i.score, i.startTime, i.endTime)
                }.slice((req.page - 1) * 5, req.page * 5),
                  p.count(i => dateFormat.format(i.startTime) >= startS && dateFormat.format(i.endTime) <= endS)))
              }.recover {
                case e: Exception =>
                  log.info(s"getPlayerRecordByTime exception.." + e.getMessage)
                  complete(ErrorRsp(130019, "getPlayerRecordByTime error."))
              }
            }
          case Left(_) =>
            complete(ErrorRsp(130026, "parse error."))
        }
    }
  }

  private val getPlayerByTimeAmount = (path("getPlayerByTimeAmount") & post & pathEndOrSingleSlash) {
    adminAuth {
      _ =>
        entity(as[Either[Error, AdminPtcl.TimeReq]]) {
          case Right(req) =>
            val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val startS = req.time.take(10) + " 00:00:00"
            val endS = req.time.take(10) + " 23:59:59"
            dealFutureResult {
              PlayerRecordDAO.getPlayerRecord().map { p =>
                complete(AdminPtcl.PlayerByTimeAmountRsp(p.filter(i => dateFormat.format(i.startTime) >= startS &&
                  dateFormat.format(i.endTime) <= endS).map(_.playerId).distinct.length))
              }.recover {
                case e: Exception =>
                  log.info(s"getPlayerByTimeAmount exception.." + e.getMessage)
                  complete(ErrorRsp(130021, "getPlayerByTimeAmount error."))
              }
            }
          case Left(_) =>
            complete(ErrorRsp(130026, "parse error."))
        }
    }
  }

  private val logout = path("logout") {
    adminAuth {
      _ =>
        invalidateSession {
          complete(SuccessRsp())
        }
    }
  }

  val adminRoutes: Route = pathPrefix("admin"){
    pathEndOrSingleSlash {
      getFromResource("html/admin.html")
    } ~
    login ~ logout ~ getRoomPlayerList ~ getPlayerRecord ~ getPlayerRecordByTime ~ getPlayerRecordAmount ~ getPlayerByTimeAmount
  }
}
