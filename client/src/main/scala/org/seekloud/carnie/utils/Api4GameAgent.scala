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

package org.seekloud.carnie.utils

import org.seekloud.carnie.protocol.Protocol4Agent._
import org.slf4j.LoggerFactory
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.seekloud.carnie.Boot.executor
import org.seekloud.carnie.common.AppSetting

object Api4GameAgent extends HttpUtil {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  def getLoginRspFromEs() = {
    val methodName = "GET"
    val url = "http://" + AppSetting.esheepDomain + "/esheep/api/gameAgent/login"
    log.info("start getLoginRspFromEs.")
    getRequestSend(methodName, url, Nil).map {
      case Right(r) =>
        decode[LoginRsp](r) match {
          case Right(rsp) =>
            log.info("end getLoginRspFromEs.")
            Right(UrlData(rsp.data.wsUrl, rsp.data.scanUrl.replaceFirst("data:image/png;base64,", "")))
          case Left(e) =>
            Left(s"error:$e")
        }
      case Left(e) =>
        log.info(s"$e")
        Left("error")
    }
  }

  def linkGameAgent(gameId: Long, playerId: String, token: String) = {
    val data = LinkGameAgentReq(gameId, playerId).asJson.noSpaces
    val url = "http://" + AppSetting.esheepDomain + "/esheep/api/gameAgent/joinGame?token=" + token

    postJsonRequestSend("post", url, Nil, data).map {
      case Right(jsonStr) =>
        decode[LinkGameAgentRsp](jsonStr) match {
          case Right(res) =>
            Right(LinkGameAgentData(res.data.accessCode, res.data.gsPrimaryInfo))
          case Left(le) =>
            Left("decode error: " + le)
        }
      case Left(erStr) =>
        Left("get return error:" + erStr)
    }

  }

  def loginByMail(email:String, pwd:String) = {
    val data = LoginByMailReq(email, pwd).asJson.noSpaces
    val url = "http://" + AppSetting.esheepDomain + "/esheep/rambler/login"

    postJsonRequestSend(s"post:$url", url, Nil, data).map {
      case Right(jsonStr) =>
        decode[ESheepUserInfoRsp](jsonStr) match {
          case Right(res) =>
            if(res.errCode==0)
              Right(res)
            else {
              log.debug(s"loginByMail error: ${res.errCode} msg: ${res.msg}")
              Left(s"errCode: ${res.errCode}")
            }
          case Left(le) =>
            Left("decode error: " + le)
        }
      case Left(erStr) =>
        Left("get return error:" + erStr)
    }
  }

  def botKey2Token(botId: String, botKey: String) = {
    val data = BotKey2TokenReq(botId, botKey).asJson.noSpaces
    val url = "http://" + AppSetting.esheepDomain + "/esheep/api/sdk/botKey2Token"

    postJsonRequestSend("post", url, Nil, data).map {
      case Right(jsonStr) =>
        println(s"jsonnStr::::::$jsonStr")
        decode[BotKey2TokenRsp](jsonStr) match {
          case Right(res) =>
            Right(res.data)
          case Left(le) =>
            Left("decode error: " + le)
        }
      case Left(erStr) =>
        Left("get return error:" + erStr)
    }
  }

  def refreshToken(playerId: String, token: String) = {//todo 暂时用不到
    val data = RefreshTokenReq(playerId).asJson.noSpaces
    val url = "http://" + AppSetting.esheepDomain + s"/esheep/api/gameAgent/gaRefreshToken?token=$token"

    postJsonRequestSend("post", url, Nil, data).map {
      case Right(jsonStr) =>
        decode[BotKey2TokenRsp](jsonStr) match {
          case Right(res) =>
            Right(res.data)
          case Left(le) =>
            Left("decode error: " + le)
        }
      case Left(erStr) =>
        Left("get return error:" + erStr)
    }
  }

  def main(args: Array[String]): Unit = {
//    getLoginRspFromEs()
    loginByMail("test@neotel.com.cn","test123")
  }

}
