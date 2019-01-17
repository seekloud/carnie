package org.seekloud.carnie.controller

import org.seekloud.carnie.Boot
import org.seekloud.carnie.common.Context
import org.seekloud.carnie.scene.{BotScene, LoginScene, ModeScene, ModeSceneListener}

class ModeController(modeScene: ModeScene, context: Context) {

  modeScene.setListener(new ModeSceneListener {
    override def gotoNormalScene(): Unit = {
      Boot.addToPlatform{
        val loginScene = new LoginScene
        val loginController = new LoginController(modeScene, loginScene, context)
        loginController.init()
        loginController.showScene()
      }
    }

    override def gotoBotScene(): Unit = {
      Boot.addToPlatform{
        val botScene = new BotScene
        val botController = new BotSceneController(modeScene,botScene,context)
        botController.showScene()
      }
    }
  })

  def showScene() {
    Boot.addToPlatform {
      context.switchScene(modeScene.getScene, "模式选择", false)
    }
  }
}
