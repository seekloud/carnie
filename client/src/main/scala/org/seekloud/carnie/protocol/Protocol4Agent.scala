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

package org.seekloud.carnie.protocol

object Protocol4Agent {

  case class UrlData(
                      wsUrl: String,
                      scanUrl: String
                    )

  case class LoginRsp(
                       data: UrlData,
                       errCode: Int,
                       msg: String
                     )

  case class GameServerInfo(
                             ip: String,
                             port: Int,
                             domain: String
                           )

  case class LinkGameAgentData(
                                accessCode: String,
                                gsPrimaryInfo: GameServerInfo
                              )

  case class LinkGameAgentRsp(
                               data: LinkGameAgentData,
                               errCode: Int,
                               msg: String
                             )

  case class LinkGameAgentReq(
                               gameId: Long,
                               playerId: String
                             )

  case class BotKey2TokenReq(
                         botId: String,
                         botKey: String
                         )

  case class BotTokenData(
                         botName: String,
                         token: String,
                         expireTime: Long
                         )

  case class BotKey2TokenRsp(
                            data: BotTokenData,
                            errCode: Int,
                            msg: String
                            )

  case class RefreshTokenReq(
                              playerId: String
                            )

  sealed trait WsData

  case class Ws4AgentRsp(
                          data: UserInfo,
                          errCode: Int,
                          msg: String
                        ) extends WsData

  case object HeartBeat extends WsData

  case class UserInfo(
                       userId: Long,
                       nickname: String,
                       token: String,
                       tokenExpireTime: Long
                     )

  final case class LoginByMailReq(
                             email: String,
                             password: String
                           )

  final case class ESheepUserInfoRsp(
                                      userName: String,
                                      userId: Long,
                                      headImg: String,
                                      token: String,
                                      gender: Int,
                                      errCode: Int = 0,
                                      msg: String = "ok"
                                    )

}
