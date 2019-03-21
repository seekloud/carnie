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

package org.seekloud.carnie

import akka.actor.{ActorSystem, Scheduler}
import akka.stream.ActorMaterializer
import scala.language.postfixOps
import akka.dispatch.MessageDispatcher
import akka.util.Timeout
import org.seekloud.carnie.common.{AppSetting, Context}
import org.seekloud.carnie.controller._
import org.seekloud.carnie.paperClient.ClientProtocol.PlayerInfoInClient
import org.seekloud.carnie.scene._
import javafx.application.Platform
import javafx.stage.Stage
import org.seekloud.carnie.utils.Api4GameAgent._
import org.slf4j.LoggerFactory
import org.seekloud.carnie.common.BotAppSetting._
import concurrent.duration._
import scala.language.postfixOps

/**
  * Created by dry on 2018/10/23.
  **/
object Boot {
  import org.seekloud.carnie.common.AppSetting._

  implicit val system: ActorSystem = ActorSystem("carnie", config)
  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds)

  def addToPlatform(fun: => Unit) = {
    Platform.runLater(() => fun)
  }
}

class Boot extends javafx.application.Application {

  import Boot._
  import org.seekloud.carnie.common.AppSetting._
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  override def start(mainStage: Stage): Unit = {
//    val para = getParameters.getRaw

//    println("!!!!" + para)

//    是否需要图像渲染
//    if(!para.isEmpty){
//      val file = new File(para.get(0))
//      if (file.isFile && file.exists) {
//        val botConfig = ConfigFactory.parseResources(para(0)).withFallback(ConfigFactory.load())
//
//        val appConfig = botConfig.getConfig("app")
//        val render = appConfig.getBoolean("render")
        val context = new Context(mainStage)
        if(render) {
          val modeScene = new ModeScene()
          val modeController = new ModeController(modeScene,context)
          modeController.showScene()
        } else {
//          val botInfo = appConfig.getConfig("botInfo")
//          val botId = botInfo.getString("botId")
//          val botKey = botInfo.getString("botKey")
          botKey2Token(botId, botKey).map {
            case Right(data) =>
              val gameId = AppSetting.esheepGameId
              linkGameAgent(gameId, botId, data.token).map {
                case Right(rst) =>
                  val layeredGameScreen = new LayeredGameScene(0, 150)
                  new BotController(PlayerInfoInClient(botId, botKey, accessCode = rst.accessCode), context, layeredGameScreen, rst.gsPrimaryInfo.domain)
                case Left(e) =>
                  log.error(s"bot link game agent error, $e")
              }

            case Left(e) =>
              log.error(s"botKey2Token error, $e")
          }
        }
//      }
//    } else {
//      log.debug("未输入参数.")
//    }

    //test
//    val context = new Context(mainStage)
//    val modeScene = new ModeScene()
//    new ModeController(modeScene,context).showScene()


//    val layeredGameScreen = new LayeredGameScene(0, 150)
//    new BotController(PlayerInfoInClient("123", "abc", "test"), context, layeredGameScreen)

//    val context = new Context(mainStage)



//    WarningDialog.initWarningDialog("just test")

//    val loginScene = new LoginScene()
//    new LoginController(loginScene,context).showScene()

//    val botScene = new BotScene()
//    new BotSceneController(botScene,context).showScene()


//    val playGameScreen = new GameScene()
//    context.switchScene(playGameScreen.getScene,fullScreen = true)
    import org.seekloud.carnie.paperClient.ClientProtocol.PlayerInfoInClient
//    new GameController(PlayerInfoInClient("test", "test", "test"), context, playGameScreen, mode = 1).start("")

//    val selectScreen = new SelectScene()
//    new SelectController(PlayerInfoInClient("test", "test", "test"), selectScreen, context, "test").showScene

//    val roomListScene = new RoomListScene()
//    new RoomListController(PlayerInfoInClient("test", "test", "test"), roomListScene, context, "test").showScene

  }
}