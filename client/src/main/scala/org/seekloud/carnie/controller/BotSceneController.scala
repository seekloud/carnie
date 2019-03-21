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
import org.seekloud.carnie.scene.{BotScene, BotSceneListener, LayeredGameScene, ModeScene}
import org.seekloud.carnie.utils.Api4GameAgent.{botKey2Token, linkGameAgent}
import org.seekloud.carnie.Boot.executor
import org.slf4j.LoggerFactory

class BotSceneController(modeScene: ModeScene,botScene: BotScene, context: Context) {
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  botScene.setListener(new BotSceneListener {
    override def confirm(botId: String, botKey: String): Unit = {
      botKey2Token(botId, botKey).map {
        case Right(data) =>
          val gameId = AppSetting.esheepGameId
          linkGameAgent(gameId, "bot" + botId, data.token).map {
            //todo 连接webSocket
            case Right(rst) =>
              Boot.addToPlatform {
                val domain = rst.gsPrimaryInfo.domain
                val layeredGameScreen = new LayeredGameScene(0, 150)
                context.switchScene4Play(layeredGameScreen.getScene, "layered", false, false,1630,660)
                layeredGameScreen.drawGameWait(context.getStage.getWidth.toInt, context.getStage.getHeight.toInt)
                new BotController(PlayerInfoInClient(botId, botKey, rst.accessCode), context, layeredGameScreen, domain)
              }
            case Left(e) =>
              log.error(s"bot link game agent error, $e")
          }

        case Left(e) =>
          log.error(s"botKey2Token error, $e")
      }
    }

    override def comeBack(): Unit = {
      Boot.addToPlatform(
        context.switchScene(modeScene.getScene, "模式选择", false)
      )
    }
  })

  def showScene() {
    Boot.addToPlatform {
      context.switchScene(botScene.getScene, "Bot模式", false)
    }
  }
}
