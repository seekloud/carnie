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
