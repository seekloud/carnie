package com.neo.sk.carnie.common

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
//    stage.setWidth(1920)
//    stage.setHeight(1080)
//    stage.setIconified(fullScreen)
    stage.setFullScreen(fullScreen)
//    stage.setMaximized(fullScreen)
    stage.show()
//    stage.fullScreenProperty()
  }

}
