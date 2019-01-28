//  Copyright 2018 seekloud (https://github.com/seekloud)
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.seekloud.carnie.controller

import org.seekloud.carnie.Boot
import org.seekloud.carnie.common.{AppSetting, Context}
import org.seekloud.carnie.paperClient.ClientProtocol.PlayerInfoInClient
import org.seekloud.carnie.paperClient.Protocol.{frameRate1, frameRate2}
import org.seekloud.carnie.ptcl.RoomApiProtocol.{PwdReq, RoomListRsp4Client, SuccessRsp}
import org.seekloud.carnie.scene._
import org.seekloud.carnie.utils.HttpUtil
import org.seekloud.carnie.utils.SecureUtil.{PostEnvelope, generateSignatureParameters}
import javafx.scene.control.TextInputDialog
import javafx.scene.image.ImageView
import org.slf4j.LoggerFactory
import org.seekloud.carnie.Boot.executor
import org.seekloud.carnie.ptcl.EsheepPtcl.{BotInfo, BotListReq, GetBotListRsp}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

import scala.util.{Failure, Success}

class BotListController(playerInfoInClient: PlayerInfoInClient, botListScene: BotListScene, context: Context, domain: String) extends HttpUtil {
  private val log = LoggerFactory.getLogger(this.getClass)
  //或许需要一个定时器,定时刷新请求
  updateBotList()

  var lastId:Long = 0L

  private def getBotList() = {
    val url = s"http://$domain/carnie/getBotList"
    val appId = AppSetting.esheepGameId.toString
    val sn = appId + System.currentTimeMillis().toString
    val data = BotListReq(playerInfoInClient.id.drop(4).toLong, lastId).asJson.noSpaces //need data
    val gsKey = AppSetting.esheepGsKey
    val (timestamp, nonce, signature) = generateSignatureParameters(List(appId, sn, data), gsKey)
    val params = PostEnvelope(appId, sn, timestamp, nonce, data,signature).asJson.noSpaces
    postJsonRequestSend("post",url,List(),params,needLogRsp = false).map{
      case Right(value) =>
        decode[GetBotListRsp](value) match {
          case Right(r) =>
            if(r.errCode == 0){
              println(s"roomData: $r")
              Right(r)
            }else{
              log.debug(s"获取bot列表失败，errCode:${r.errCode},msg:${r.msg}")
              Left("Error")
            }
          case Left(error) =>
            log.debug(s"获取bot列表失败1，${error}")
            Left("Error")

        }
      case Left(error) =>
        log.debug(s"获取bot列表失败2，${error}")
        Left("Error")
    }
  }

  //fixme test
    private def updateBotList() = {
      Boot.addToPlatform(
        botListScene.updateBotList(List(BotInfo(1111,"test","test","test","test")))
      )
    }

//  private def updateBotList() = {
//    getBotList().onComplete{
//      case Success(res) =>
//        res match {
//          case Right(roomListRsp) =>
//            Boot.addToPlatform(
//              botListScene.updateBotList(roomListRsp.data)
//            )
//          case Left(e) =>
//            log.error(s"获取bot列表失败，error：${e}")
//        }
//      case Failure(e) =>
//        log.error(s"failure:${e}")
//    }
//  }

  botListScene.listener = new BotListSceneListener {
    override def joinGame(mode: Int, img: Int): Unit = {
//      if(roomId.toString != null) {
//        val img = 0 //头部图像
//        val frameRate = if(mode==2) frameRate2 else frameRate1
//        val pwd = if(hasPwd) inputPwd else None
//        if(hasPwd){
//          if(pwd.nonEmpty) {
//            verifyPwd(roomId, pwd.get).map{
//              case true =>
//                Boot.addToPlatform(
//                  playGame(mode, img, frameRate, roomId)
//                )
//              case false =>
//              //              密码错误不做任何处理
//            }
//          }
//        } else {
//          Boot.addToPlatform(
//            playGame(mode, img, frameRate, roomId)
//          )
//        }
//      }
    }
  }

  def verifyPwd(roomId:Int, pwd:String) = {
    val url = s"http://$domain/carnie/verifyPwd"
    val data = PwdReq(roomId,pwd).asJson.noSpaces
    val appId = AppSetting.esheepGameId.toString
    val sn = appId + System.currentTimeMillis().toString
    val gsKey = AppSetting.esheepGsKey
    val (timestamp, nonce, signature) = generateSignatureParameters(List(appId, sn, data), gsKey)
    val params = PostEnvelope(appId, sn, timestamp, nonce, data,signature).asJson.noSpaces
    postJsonRequestSend("post",url,List(),params,needLogRsp = false).map { //data
      case Right(value) =>
        decode[SuccessRsp](value) match {
          case Right(r) =>
            if(r.errCode==0){
              true
            } else {
              log.debug("some errors in verifyPwd1.")
              false
            }
          case Left(e) =>
            log.debug(s"some errors verifyPwd2: $e")
            false
        }
      case Left(e) =>
        log.debug(s"some errors verifyPwd3: $e")
        false
    }
  }

//  def playGame(mode: Int,
//               img:Int,
//               frameRate:Int,
//               roomId:Int) = {
//    val playGameScreen = new GameScene(img, frameRate)
//    val LayeredGameScreen = new LayeredGameScene(img, frameRate)
//    context.switchScene(playGameScreen.getScene, fullScreen = true)
//    new GameController(playerInfoInClient, context, playGameScreen,LayeredGameScreen, mode, frameRate).joinByRoomId(domain, roomId, img)
//  }

  def inputPwd = {
    val dialog = new TextInputDialog()
    dialog.setTitle("房间密码")
    dialog.setHeaderText("")
    dialog.setGraphic(new ImageView())
    dialog.setContentText("请输入密码:")
    val rst = dialog.showAndWait()
    var pwd: Option[String] = None
    rst.ifPresent(a => pwd = Some(a))
    pwd
  }

  def showScene: Unit = {
    Boot.addToPlatform {
      context.switchScene(botListScene.getScene, "BotList", false)
    }
  }
}
