package com.neo.sk.carnie.controller

import com.neo.sk.carnie.Boot
import com.neo.sk.carnie.scene.{GameScene, SelectScene, SelectSceneListener}
import com.neo.sk.carnie.common.Context
import com.neo.sk.carnie.paperClient.ClientProtocol.PlayerInfoInClient
import com.neo.sk.carnie.paperClient.Protocol.{frameRate1, frameRate2}


class SelectController(playerInfoInClient: PlayerInfoInClient, selectScene: SelectScene, context: Context, domain: String) {

  selectScene.setListener(new SelectSceneListener{
    override def joinGame(mode: Int, img: Int): Unit = {
      Boot.addToPlatform {
        val frameRate = if(mode==2) frameRate2 else frameRate1
        val playGameScreen = new GameScene(img, frameRate)
        context.switchScene(playGameScreen.getScene, fullScreen = true)
        new GameController(playerInfoInClient, context, playGameScreen, mode, frameRate).start(domain, mode, img)
      }
    }

    override def createRoom(mode: Int, img: Int, pwd: String): Unit = {
      Boot.addToPlatform {
        val frameRate = if(mode==2) frameRate2 else frameRate1
        println(s"pwd: $pwd")
        val playGameScreen = new GameScene(img, frameRate)
        context.switchScene(playGameScreen.getScene, fullScreen = true)
        new GameController(playerInfoInClient, context, playGameScreen, mode, frameRate).createRoom(domain, mode, img, pwd)
      }
    }
  })

  def showScene: Unit = {
    Boot.addToPlatform {
      context.switchScene(selectScene.scene, "Select", false)
    }
  }
}
