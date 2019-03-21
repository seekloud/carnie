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
import org.seekloud.carnie.Boot.executor
import org.seekloud.carnie.scene._
import org.seekloud.carnie.common.{AppSetting, Context}
import org.seekloud.carnie.paperClient.ClientProtocol.PlayerInfoInClient
import org.seekloud.carnie.paperClient.Protocol.{frameRate1, frameRate2}
import org.seekloud.carnie.utils.Api4GameAgent.linkGameAgent
import javafx.collections.FXCollections
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control._
import javafx.scene.layout.GridPane
import org.slf4j.LoggerFactory


class SelectController(playerInfoInClient: PlayerInfoInClient, selectScene: SelectScene, context: Context) {
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  selectScene.setListener(new SelectSceneListener{
    override def joinGame(mode: Int, img: Int): Unit = {
      val frameRate = if(mode==2) frameRate2 else frameRate1
      val playGameScreen = new GameScene(img, frameRate)
//        val LayeredGameScreen = new LayeredGameScene(img, frameRate)
//        val x = false
//        if(x) {
      val gameId = AppSetting.esheepGameId
      Boot.addToPlatform(
        linkGameAgent(gameId, playerInfoInClient.id, playerInfoInClient.token).map {
          case Right(r) =>
            Boot.addToPlatform {
              context.switchScene(playGameScreen.getScene, fullScreen = true, resizable = true)
              new GameController(playerInfoInClient.copy(accessCode = r.accessCode), context, playGameScreen, mode, frameRate).start(r.gsPrimaryInfo.domain, mode, img)
            }

          case Left(e) =>
            log.debug(s"linkGameAgent..$e")
        }
      )

//        }
//        else {
//          context.switchScene(LayeredGameScreen.getScene,fullScreen = true)
//          new GameController(playerInfoInClient, context, playGameScreen, mode, frameRate).start(domain, mode, img)
//        }

    }

    override def gotoRoomList(): Unit = {
      Boot.addToPlatform {
        val roomListScene = new RoomListScene()
        new RoomListController(playerInfoInClient, selectScene, roomListScene, context).showScene
      }
    }
  })

  //todo 创建房间的弹窗demo
  def initDialog = {
    val dialog = new Dialog[(String,String,String)]()
    dialog.setTitle("test")
    val a = new ChoiceBox[String](FXCollections.observableArrayList("正常","反转","加速"))
    a.setValue("反转")
    val tF = new TextField()
    val loginButton = new ButtonType("确认", ButtonData.OK_DONE)
    val grid = new GridPane
    grid.add(a, 0 ,0)
    grid.add(tF, 0 ,1)
    dialog.getDialogPane.getButtonTypes.addAll(loginButton, ButtonType.CANCEL)
    dialog.getDialogPane.setContent(grid)
    dialog.setResultConverter(dialogButton =>
      if(dialogButton == loginButton)
        (a.getValue, "a", tF.getText)
      else
        null
    )
    val rst = dialog.showAndWait()
    rst.ifPresent(a =>
      println(s"${a._1}-${a._3}")
    )
  }

  def showScene: Unit = {
    Boot.addToPlatform {
      context.switchScene(selectScene.getScene, "选择游戏模式及头像", false)
    }
  }
}
