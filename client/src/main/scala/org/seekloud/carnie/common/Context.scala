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

package org.seekloud.carnie.common

import javafx.scene.Scene
import javafx.stage.Stage

/**
  * Created by dry on 2018/10/29.
  **/
class Context(stage: Stage) {

  def getStage: Stage = stage
  def switchScene(scene: Scene, title:String = "carnie", fullScreen: Boolean,resizable: Boolean = false) = {
    stage.setScene(scene)
    stage.sizeToScene()
    stage.setResizable(resizable)
    stage.setTitle(title)
//    stage.setWidth(width)
//    stage.setHeight(height)
//    stage.setIconified(fullScreen)
    stage.setFullScreen(fullScreen)
//    stage.setMaximized(fullScreen)
    stage.show()
//    stage.fullScreenProperty()
  }

  def switchScene4Play(scene: Scene, title:String = "carnie", fullScreen: Boolean,resizable: Boolean = false,width: Int = 1920,height: Int = 1080) = {
    stage.setScene(scene)
    stage.sizeToScene()
    stage.setResizable(resizable)
    stage.setTitle(title)
    stage.setWidth(width)
    stage.setHeight(height)
    stage.setX(130)
    stage.setY(185)
    //    stage.setIconified(fullScreen)
    stage.setFullScreen(fullScreen)
    //    stage.setMaximized(fullScreen)
    stage.show()
    //    stage.fullScreenProperty()
  }

}
