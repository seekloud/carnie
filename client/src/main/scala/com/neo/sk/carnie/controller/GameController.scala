package com.neo.sk.carnie.controller

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import com.neo.sk.carnie.Boot
import com.neo.sk.carnie.common.{Constant, Context}
import com.neo.sk.carnie.paperClient.Protocol._
import com.neo.sk.carnie.paperClient._
import com.neo.sk.carnie.scene.{GameScene, LayeredGameScene, PerformanceTool, SelectScene}
import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import javafx.scene.input.KeyCode
import javafx.util.Duration
import akka.actor.typed.scaladsl.adapter._
import org.slf4j.LoggerFactory
import com.neo.sk.carnie.actor.PlayGameWebSocket
import com.neo.sk.carnie.paperClient.ClientProtocol.PlayerInfoInClient
import javafx.scene.media.{AudioClip, Media, MediaPlayer}
import java.awt.event.KeyEvent
//import org.seekloud.esheepapi.pb.observations.{ImgData, LayeredObservation}

/**
  * Created by dry on 2018/10/29.
  **/

class GameController(player: PlayerInfoInClient,
                     stageCtx: Context,
                     gameScene: GameScene,
//                     layeredGameScene: LayeredGameScene,
                     mode: Int =0,
                     frameRate: Int
                     ) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  private val playActor = Boot.system.spawn(PlayGameWebSocket.create(this), "playActor")

  var currentRank = List.empty[Score]
  val bounds = Point(Boundary.w, Boundary.h)
  var grid = new GridOnClient(bounds)
  var firstCome = true
  val idGenerator = new AtomicInteger(1)
  var playBgm = true
  var isWin = false
  var exitFullScreen = false
  var winnerName = "unknown"
  var isContinues = true
  var btnFlag = true
  var winnerData : Option[Protocol.Data4TotalSync] = None
  val audioFinish = new AudioClip(getClass.getResource("/mp3/finish.mp3").toString)
  val audioKill = new AudioClip(getClass.getResource("/mp3/kill.mp3").toString)
  val audioWin = new AudioClip(getClass.getResource("/mp3/win.mp3").toString)
  val audioDie = new AudioClip(getClass.getResource("/mp3/killed.mp3").toString)
  val bgm1 = new AudioClip(getClass.getResource("/mp3/bgm1.mp3").toString)
  val bgm3 = new AudioClip(getClass.getResource("/mp3/bgm3.mp3").toString)
  val bgm4 = new AudioClip(getClass.getResource("/mp3/bgm4.mp3").toString)
  val bgm7 = new AudioClip(getClass.getResource("/mp3/bgm7.mp3").toString)
  val bgm8 = new AudioClip(getClass.getResource("/mp3/bgm8.mp3").toString)
  val bgmList = List(bgm1,bgm3,bgm4,bgm7,bgm8)

  var BGM = new AudioClip(getClass.getResource("/mp3/bgm4.mp3").toString)
  var newFieldInfo = Map.empty[Long, Protocol.NewFieldInfo] //[frame, newFieldInfo)
  private val bgmAmount = bgmList.length
  var syncGridData: scala.Option[Protocol.Data4TotalSync] = None
  var newSnakeInfo: scala.Option[Protocol.NewSnakeInfo] = None
  var drawFunction: FrontProtocol.DrawFunction = FrontProtocol.DrawGameWait
  private var recallFrame: scala.Option[Long] = None
  private var stageWidth = stageCtx.getStage.getWidth.toInt
  private var stageHeight = stageCtx.getStage.getHeight.toInt
  private var isContinue = true
  private var myScore = BaseScore(0, 0, 0l, 0l)
  private var maxArea: Int = 0
  private var winningData = WinData(0,Some(0))
  private val timeline = new Timeline()
  private var logicFrameTime = System.currentTimeMillis()
  private val animationTimer = new AnimationTimer() {
    override def handle(now: Long): Unit = {
      draw(System.currentTimeMillis() - logicFrameTime)
    }
  }

  def loseConnect(): Unit = {
    gameScene.drawGameOff(firstCome)
//    layeredGameScene.drawGameOff(firstCome)
  }

  def getRandom(s: Int):Int ={
    val rnd = new scala.util.Random
    rnd.nextInt(s)
  }

  def start(domain: String, mode: Int, img: Int): Unit = {
//    println(s"got accCode:${player.msg}")
    playActor ! PlayGameWebSocket.ConnectGame(player, domain, mode, img)
    addUserActionListen()
    startGameLoop()
  }

  def joinByRoomId(domain: String, roomId: Int, img: Int): Unit = {
    playActor ! PlayGameWebSocket.JoinByRoomId(player, domain, roomId, img)
    addUserActionListen()
    startGameLoop()
  }

  def createRoom(domain: String, mode: Int, img: Int, pwd: String): Unit = {
    playActor ! PlayGameWebSocket.CreateRoom(player, domain, mode, img, pwd)
    addUserActionListen()
    startGameLoop()
  }

  def startGameLoop(): Unit = { //渲染帧
    BGM = bgmList(getRandom(bgmAmount))
    logicFrameTime = System.currentTimeMillis()
    timeline.setCycleCount(Animation.INDEFINITE)
    val keyFrame = new KeyFrame(Duration.millis(frameRate), { _ =>
      logicLoop()
    })
    timeline.getKeyFrames.add(keyFrame)
    animationTimer.start()
    timeline.play()
  }

  private def logicLoop(): Unit = { //逻辑帧
    if(!stageCtx.getStage.isFullScreen && !exitFullScreen) {
      gameScene.resetScreen(Constant.CanvasWidth,Constant.CanvasHeight,Constant.CanvasWidth,Constant.CanvasHeight)
      stageCtx.getStage.setWidth(Constant.CanvasWidth)
      stageCtx.getStage.setHeight(Constant.CanvasHeight)
      exitFullScreen = true
    }
    if(stageWidth != stageCtx.getStage.getWidth.toInt || stageHeight != stageCtx.getStage.getHeight.toInt){
      stageWidth = stageCtx.getStage.getWidth.toInt
      stageHeight = stageCtx.getStage.getHeight.toInt
      gameScene.resetScreen(stageWidth,stageHeight,stageWidth,stageHeight)
      stageCtx.getStage.setWidth(stageWidth)
      stageCtx.getStage.setHeight(stageHeight)
    }
    logicFrameTime = System.currentTimeMillis()
    playActor ! PlayGameWebSocket.MsgToService(Protocol.SendPingPacket(player.id, System.currentTimeMillis()))

    recallFrame match {
      case Some(-1) =>
        println("!!!!!!!!:NeedToSync2")
        playActor ! PlayGameWebSocket.MsgToService(NeedToSync(player.id).asInstanceOf[UserAction])
        recallFrame = None

      case Some(frame) =>
        val time1 = System.currentTimeMillis()
        println(s"recall ...before frame:${grid.frameCount}")
        val oldGrid = grid
        oldGrid.recallGrid(frame, grid.frameCount)
        grid = oldGrid
        println(s"after recall time: ${System.currentTimeMillis() - time1}...after frame:${grid.frameCount}")
        recallFrame = None

      case None =>
    }

    if (newSnakeInfo.nonEmpty) {
      //        println(s"newSnakeInfo: ${newSnakeInfo.get.snake.map(_.id)}")
      if (newSnakeInfo.get.snake.map(_.id).contains(player.id) && !firstCome) spaceKey()
      newSnakeInfo.get.snake.foreach { s =>
        grid.cleanSnakeTurnPoint(s.id) //清理死前拐点
      }
      grid.snakes ++= newSnakeInfo.get.snake.map(s => s.id -> s).toMap
      grid.addNewFieldInfo(NewFieldInfo(newSnakeInfo.get.frameCount, newSnakeInfo.get.filedDetails))

      newSnakeInfo = None
    }

    if (newFieldInfo.get(grid.frameCount + 1).nonEmpty && newFieldInfo(grid.frameCount + 1).fieldDetails.map(_.uid).contains(player.id)){
      println(s"11111111111111111")
      val snake = grid.snakes(player.id)
      val acts = grid.actionMap.getOrElse(grid.frameCount + 1, Map.empty[String, Int])
      val keyCode = acts.get(player.id)
      val newDirection = {
        val keyDirection = keyCode match {
          case Some(KeyEvent.VK_LEFT) => Point(-1, 0)
          case Some(KeyEvent.VK_RIGHT) => Point(1, 0)
          case Some(KeyEvent.VK_UP) => Point(0, -1)
          case Some(KeyEvent.VK_DOWN) => Point(0, 1)
          case _ => snake.direction
        }
        if (keyDirection + snake.direction != Point(0, 0)) {
          keyDirection
        } else {
          snake.direction
        }
      }
      val header = grid.grid.get(snake.header)
      val newHeader = grid.grid.get(snake.header + newDirection)

      println(s"old header:$header --new header:$newHeader ")
    }

    if (syncGridData.nonEmpty) { //逻辑帧更新数据
      grid.initSyncGridData(syncGridData.get)
      syncGridData = None
    } else {
      grid.update("f")
//      println(s"update: ${grid.getGridData.snakes.map(_.id)}")
    }

    if (newFieldInfo.nonEmpty) {
      val minFrame = newFieldInfo.keys.min
      (minFrame to grid.frameCount).foreach { frame =>
        if (newFieldInfo.get(frame).nonEmpty) {
          val newFieldData = newFieldInfo(frame)
          if (newFieldData.fieldDetails.map(_.uid).contains(player.id))
            println("after newFieldInfo, my turnPoint:" + grid.snakeTurnPoints.get(player.id))
          grid.addNewFieldInfo(newFieldData)
          newFieldInfo -= frame
        }
      }
    }

//    if (newSnakeInfo.nonEmpty) {
//      grid.snakes ++= newSnakeInfo.get.snake.map(s => s.id -> s).toMap
//      grid.addNewFieldInfo(NewFieldInfo(newSnakeInfo.get.frameCount, newSnakeInfo.get.filedDetails))
//      newSnakeInfo = None
//    }
//
//    if(syncGridData.nonEmpty) {
//      grid.initSyncGridData(syncGridData.get)
//      syncGridData = None
//      println(s"sync: ${grid.getGridData.snakes.map(_.id)}")
//    } else {
//      grid.update("f")
//      println(s"update: ${grid.getGridData.snakes.map(_.id)}")
//      if (newFieldInfo.nonEmpty) {
//        val frame = newFieldInfo.keys.min
//        val newFieldData = newFieldInfo(frame)
//        if (frame == grid.frameCount) {
//          grid.addNewFieldInfo(newFieldData)
//          newFieldInfo -= frame
//        } else if (frame < grid.frameCount) {
//          playActor ! PlayGameWebSocket.MsgToService(NeedToSync(player.id).asInstanceOf[UserAction])
//        }
//      }
//    }

    val gridData = grid.getGridData

    gridData.snakes.find(_.id == player.id) match {
      case Some(_) =>
        if(firstCome)
          firstCome = false
//        if(playBgm) {
//          BGM.play(30)
//          playBgm = false
//        }
//        if(!BGM.isPlaying){
//          BGM = bgmList(getRandom(bgmAmount))
//          BGM.play(30)
//        }
//        val offsetTime = System.currentTimeMillis() - logicFrameTime
//        val a = System.currentTimeMillis()
//        layeredGameScene.draw(currentRank,player.id, gridData, offsetTime, grid, currentRank.headOption.map(_.id).getOrElse(player.id))
//        val x = getAllImage
//        val b = System.currentTimeMillis()
//        println("drawTime:" + (b-a) )
//        println(s"${grid.getGridData.snakes.map(_.id)}")
        drawFunction = FrontProtocol.DrawBaseGame(gridData)



      case None if isWin =>

      case None if !firstCome =>
        if(btnFlag) {
          println("first come to diePage.")
          gameScene.group.getChildren.add(gameScene.backBtn)
          btnFlag = false
        }
        println("111" + grid.killHistory)
//        val a = System.currentTimeMillis()
//        println(s"${grid.getGridData.snakes.map(_.id)},$a")
        drawFunction = FrontProtocol.DrawGameDie(grid.getKiller(player.id).map(_._2))


      case _ =>
        drawFunction = FrontProtocol.DrawGameWait
    }
  }

  def draw(offsetTime: Long): Unit = {
    drawFunction match {
      case FrontProtocol.DrawGameWait =>
        if(BGM.isPlaying){
          BGM.stop()
        }
        gameScene.drawGameWait()
//        layeredGameScene.drawGameWait()

      case FrontProtocol.DrawGameOff =>
        if(BGM.isPlaying){
          BGM.stop()
        }
        gameScene.drawGameOff(firstCome)
//        layeredGameScene.drawGameOff(firstCome)

      case FrontProtocol.DrawGameWin(winner, winData) =>
        if(BGM.isPlaying){
          BGM.stop()
        }
        gameScene.drawGameWin(player.id, winner, winData,winningData)
//        layeredGameScene.drawGameWin(player.id, winner, winData,winningData)
        isContinue = false

      case FrontProtocol.DrawBaseGame(data) =>
        gameScene.draw(player.id, data, offsetTime, grid, currentRank.headOption.map(_.id).getOrElse(player.id))

        if (grid.killInfo.nonEmpty) {
          val killBaseInfo = grid.killInfo.get
          gameScene.drawBarrage(killBaseInfo._2, killBaseInfo._3)
          grid.barrageDuration -= 1
          if (grid.barrageDuration == 0) grid.killInfo = None
        }

      case FrontProtocol.DrawGameDie(killerName) =>
        if(BGM.isPlaying){
          BGM.stop()
        }
//        if (isContinue) audioDie.play()
        gameScene.drawGameDie(killerName, myScore, maxArea)
//        layeredGameScene.drawGameDie(killerName, myScore, maxArea)
        grid.killInfo = None
        isContinue = false
    }
  }

  def gameMessageReceiver(msg: WsSourceProtocol.WsMsgSource): Unit = {
    msg match {
      case Protocol.Id(id) =>
        log.debug(s"i receive my id:$id")

      case Protocol.SnakeAction(id, keyCode, frame, actionId) =>
        Boot.addToPlatform {
          if (grid.snakes.exists(_._1 == id)) {
            if (id == player.id) { //收到自己的进行校验是否与预判一致，若不一致则回溯
              if (grid.myActionHistory.get(actionId).isEmpty) { //前端没有该项，则加入
                grid.addActionWithFrame(id, keyCode, frame)
                if (frame < grid.frameCount) {
                  if (grid.frameCount - frame <= (grid.maxDelayed - 1)) { //回溯
                    recallFrame = recallFrame match {
                      case Some(oldFrame) => Some(Math.min(frame, oldFrame))
                      case None => Some(frame)
                    }
                  } else {
                    recallFrame = Some(-1)
                  }
                }
              } else {
                if (grid.myActionHistory(actionId)._1 != keyCode || grid.myActionHistory(actionId)._2 != frame) { //若keyCode或则frame不一致则进行回溯
                  println(s"now:${grid.frameCount}...history:${grid.myActionHistory(actionId)._2}...backend:$frame")
                  grid.deleteActionWithFrame(id, grid.myActionHistory(actionId)._2)
                  grid.addActionWithFrame(id, keyCode, frame)
                  val miniFrame = Math.min(frame, grid.myActionHistory(actionId)._2)
                  if (miniFrame < grid.frameCount) {
                    if (grid.frameCount - miniFrame <= (grid.maxDelayed - 1)) { //回溯
                      recallFrame = recallFrame match {
                        case Some(oldFrame) => Some(Math.min(frame, oldFrame))
                        case None => Some(frame)
                      }
                    } else {
                      recallFrame = Some(-1)
                    }
                  }
                }
                grid.myActionHistory -= actionId
              }
            } else { //收到别人的动作则加入action，若帧号滞后则进行回溯
              grid.addActionWithFrame(id, keyCode, frame)
              if (frame < grid.frameCount) {
                if (grid.frameCount - frame <= (grid.maxDelayed - 1)) { //回溯
                  recallFrame = recallFrame match {
                    case Some(oldFrame) => Some(Math.min(frame, oldFrame))
                    case None => Some(frame)
                  }
                } else {
                  recallFrame = Some(-1)
                }
              }
            }
          }
//          if (grid.snakes.exists(_._1 == id)) {
//            if (id == player.id) { //收到自己的进行校验是否与预判一致，若不一致则回溯
//              if (grid.myActionHistory.get(actionId).isEmpty) { //前端没有该项，则加入
//                grid.addActionWithFrame(id, keyCode, frame)
//                if (frame < grid.frameCount && grid.frameCount - frame <= (grid.maxDelayed - 1)) { //回溯
//                  val oldGrid = grid
//                  oldGrid.recallGrid(frame, grid.frameCount)
//                  grid = oldGrid
//                }
//              } else {
//                if (grid.myActionHistory(actionId)._1 != keyCode || grid.myActionHistory(actionId)._2 != frame) { //若keyCode或则frame不一致则进行回溯
//                  grid.deleteActionWithFrame(id, grid.myActionHistory(actionId)._2)
//                  grid.addActionWithFrame(id, keyCode, frame)
//                  val miniFrame = Math.min(frame, grid.myActionHistory(actionId)._2)
//                  if (miniFrame < grid.frameCount && grid.frameCount - miniFrame <= (grid.maxDelayed - 1)) { //回溯
//                    val oldGrid = grid
//                    oldGrid.recallGrid(miniFrame, grid.frameCount)
//                    grid = oldGrid
//                  }
//                }
//                grid.myActionHistory -= actionId
//              }
//            } else { //收到别人的动作则加入action，若帧号滞后则进行回溯
//              grid.addActionWithFrame(id, keyCode, frame)
//              if (frame < grid.frameCount && grid.frameCount - frame <= (grid.maxDelayed - 1)) { //回溯
//                val oldGrid = grid
//                oldGrid.recallGrid(frame, grid.frameCount)
//                grid = oldGrid
//              }
//            }
//          }
        }

      case Protocol.SomeOneWin(winner, finalData) =>
        Boot.addToPlatform {
          drawFunction = FrontProtocol.DrawGameWin(winner, finalData)
          winnerName = winner
          winnerData = Some(finalData)
          isWin = true
//          audioWin.play()
          //        gameScene.drawGameWin(player.id, winner, finalData)
          grid.cleanData()
        }

      case x@Protocol.WinnerBestScore(score) =>
        log.debug(s"receive winnerBestScore msg:$x")
          maxArea = Math.max(maxArea,score)

      case UserLeft(id) =>
        Boot.addToPlatform {
          println(s"user $id left:::")
          if (grid.snakes.contains(id)) grid.snakes -= id
          grid.returnBackField(id)
          grid.grid ++= grid.grid.filter(_._2 match { case Body(_, fid) if fid.nonEmpty && fid.get == id => true case _ => false }).map { g =>
            Point(g._1.x, g._1.y) -> Body(g._2.asInstanceOf[Body].id, None)
          }
        }

      case x@Protocol.DeadPage(id, kill, area, start, end) =>
        println(s"recv userDead $x")
        Boot.addToPlatform {
          myScore = BaseScore(kill, area, start, end)
        }


      case Protocol.Ranks(current, score, rank) =>
        Boot.addToPlatform {
          currentRank = current
          maxArea = Math.max(maxArea, score.area)
          if (grid.getGridData.snakes.exists(_.id == player.id) && !isWin){
            gameScene.drawRank(player.id, grid.getGridData.snakes, current, score, rank)
//            layeredGameScene.drawRank(player.id, grid.getGridData.snakes, current)
//            layeredGameScene.drawHumanRank(player.id, grid.getGridData.snakes, currentRank)
          }
        }

      case data: Protocol.Data4TotalSync =>
//        println(s"all data : ${data.snakes.map(_.id)}")
        Boot.addToPlatform{
          syncGridData = Some(data)
          if (data.fieldDetails.nonEmpty) newFieldInfo = newFieldInfo.filterKeys(_ > data.frameCount)
//          newFieldInfo = newFieldInfo.filterKeys(_ > data.frameCount)
        }

      case data: Protocol.NewSnakeInfo =>
//        println(s"!!!!!!new snake join!!!")
        Boot.addToPlatform{
          newSnakeInfo = Some(data)
        }

      case Protocol.SomeOneKilled(killedId, killedName, killerName) =>
        Boot.addToPlatform {
          grid.killInfo = Some(killedId, killedName, killerName)
          grid.barrageDuration = 100
        }

      case data: Protocol.NewFieldInfo =>
        Boot.addToPlatform{
          if(data.fieldDetails.exists(_.uid == player.id))
            audioFinish.play()
          newFieldInfo += data.frameCount -> data
          grid.historyFieldInfo += data.frameCount -> data
        }

      case x@Protocol.ReceivePingPacket(_) =>
        Boot.addToPlatform{
          PerformanceTool.receivePingPackage(x)
        }

      case x@Protocol.WinData(_,_) =>
        log.debug(s"receive winningData msg:$x")
        winningData = x

      case unknown@_ =>
        log.debug(s"i receive an unknown msg:$unknown")
    }
  }

  def addUserActionListen():Unit = {
//    layeredGameScene.positionCanvas.requestFocus()

//    layeredGameScene.positionCanvas.setOnKeyPressed{ event =>
//      val key = event.getCode
//      if (Constant.watchKeys.contains(key)) {
//        val delay = if(mode==2) 4 else 2
//        val frame = grid.frameCount + delay
//        val actionId = idGenerator.getAndIncrement()
//        val keyCode = Constant.keyCode2Int(key)
//        grid.addActionWithFrame(player.id, keyCode, frame)
//
//        if (key != KeyCode.SPACE) {
//          grid.myActionHistory += actionId -> (keyCode, frame)
//        } else {
//          //数据重置
//          drawFunction match {
//            case FrontProtocol.DrawBaseGame(_) =>
//            case _ =>
//              grid.cleanData()
//              drawFunction = FrontProtocol.DrawGameWait
//              audioWin.stop()
//              audioDie.stop()
//              firstCome = true
//              if(isWin){
//                isWin = false
//                winnerName = "unknown"
//              }
//              animationTimer.start()
//              isContinue = true
//          }
//        }
//        val newKeyCode =
//          if(mode == 1)
//            key match {
//              case KeyCode.LEFT => KeyCode.RIGHT
//              case KeyCode.RIGHT => KeyCode.LEFT
//              case KeyCode.DOWN => KeyCode.UP
//              case KeyCode.UP => KeyCode.DOWN
//              case _ => KeyCode.SPACE
//            }
//          else key
//        playActor ! PlayGameWebSocket.MsgToService(Protocol.Key(player.id, Constant.keyCode2Int(newKeyCode), frame, actionId))
//      }
//    }
    gameScene.viewCanvas.requestFocus()

    gameScene.viewCanvas.setOnKeyPressed{ event =>
      val key = event.getCode
      if (Constant.watchKeys.contains(key)) {
        val delay = if(mode==2) 4 else 2
        val frame = grid.frameCount + delay
//        val actionId = idGenerator.getAndIncrement()
        val keyCode = Constant.keyCode2Int(key)
        grid.addActionWithFrame(player.id, keyCode, frame)
        key match {
          case KeyCode.SPACE =>
            val msg: Protocol.UserAction = PressSpace
            playActor ! PlayGameWebSocket.MsgToService(msg)

          case _ =>
            val actionFrame = grid.getUserMaxActionFrame(player.id, frame)
            if(actionFrame < frame + maxContainableAction) {
              val actionId = idGenerator.getAndIncrement()
              val newKeyCode =
                if (mode == 1)
                  key match {
                    case KeyCode.LEFT => KeyCode.RIGHT
                    case KeyCode.RIGHT => KeyCode.LEFT
                    case KeyCode.DOWN => KeyCode.UP
                    case KeyCode.UP => KeyCode.DOWN
                    case _ => KeyCode.SPACE
                  } else key
              println(s"onkeydown：${Key(player.id, Constant.keyCode2Int(newKeyCode), actionFrame, actionId)}")
              grid.addActionWithFrame(player.id, Constant.keyCode2Int(newKeyCode), actionFrame)
              grid.myActionHistory += actionId -> (Constant.keyCode2Int(newKeyCode), actionFrame)
              val msg: Protocol.UserAction = Protocol.Key(player.id, Constant.keyCode2Int(newKeyCode), frame, actionId)
              playActor ! PlayGameWebSocket.MsgToService(msg)
            }

        }
      }
//      gameScene.viewCanvas.setOnKeyReleased { e =>
//        val myField = grid.grid.filter(_._2 == Field(player.id))
//        val myBody = grid.snakeTurnPoints.getOrElse(player.id, Nil)
//
//
//        //        newField = myField.map { f =>
//        val myGroupField =  FieldByColumn(player.id, myField.keys.groupBy(_.y).map { case (y, target) =>
//          (y.toInt, Tool.findContinuous(target.map(_.x.toInt).toArray.sorted))//read
//        }.toList.groupBy(_._2).map { case (r, target) =>
//          ScanByColumn(Tool.findContinuous(target.map(_._1).toArray.sorted), r)
//        }.toList)
//        println(s"=======myField:$myGroupField, myBody:$myBody")
        //        }
//      }
//        if (key != KeyCode.SPACE) {
//          grid.myActionHistory += actionId -> (keyCode, frame)
//        } else {
//          //数据重置
//          drawFunction match {
//            case FrontProtocol.DrawBaseGame(_) =>
//            case _ =>
//              val msg: Protocol.UserAction = PressSpace
//              playActor ! PlayGameWebSocket.MsgToService(msg)
//              grid.cleanData()
//              drawFunction = FrontProtocol.DrawGameWait
//              audioWin.stop()
//              audioDie.stop()
//              firstCome = true
//              if(isWin){
//                isWin = false
//                winnerName = "unknown"
//              }
//              animationTimer.start()
//              isContinue = true
//              gameScene.group.getChildren.remove(gameScene.backBtn)
//              btnFlag = true
//          }
//        }
//        val newKeyCode =
//          if(mode == 1)
//            key match {
//              case KeyCode.LEFT => KeyCode.RIGHT
//              case KeyCode.RIGHT => KeyCode.LEFT
//              case KeyCode.DOWN => KeyCode.UP
//              case KeyCode.UP => KeyCode.DOWN
//              case _ => KeyCode.SPACE
//            }
//          else key
//        println(s"onkeydown：${Key(player.id, Constant.keyCode2Int(newKeyCode), frame, actionId)}")
//        playActor ! PlayGameWebSocket.MsgToService(Protocol.Key(player.id, Constant.keyCode2Int(newKeyCode), frame, actionId))
//      }
    }

    gameScene.backBtn.setOnAction(_ => switchToSelecting())
  }

  def switchToSelecting():Unit = {
    //fixme 假设用户一次玩游戏时间不超过两小时，否则需要刷新token
    Boot.addToPlatform{
      println("come back to selectScene.")
      stageCtx.getStage.setFullScreen(false)
      Boot.system.stop(playActor.toUntyped)
      Boot.addToPlatform{
        val selectScene = new SelectScene()
        new SelectController(player, selectScene, stageCtx).showScene
      }
    }
  }

  def spaceKey(): Unit = {
    grid.cleanSnakeTurnPoint(player.id)
    grid.actionMap = grid.actionMap.filterNot(_._2.contains(player.id))
    drawFunction = FrontProtocol.DrawGameWait
    firstCome = true
    if (isWin) isWin = false
    myScore = BaseScore(0, 0, 0l, 0l)
    isContinue = true
//    animationTimer.start()
    //                  backBtn.style.display="none"
    //                  rankCanvas.addEventListener("",null)
  }

//  def getAllImage  = {
//    val imageList = layeredGameScene.layered.getAllImageData
//    val humanObservation : _root_.scala.Option[ImgData] = imageList.find(_._1 == "6").map(_._2)
//    val layeredObservation : LayeredObservation = LayeredObservation(
//      imageList.find(_._1 == "0").map(_._2),
//      imageList.find(_._1 == "1").map(_._2),
//      imageList.find(_._1 == "2").map(_._2),
//      imageList.find(_._1 == "3").map(_._2),
//      imageList.find(_._1 == "4").map(_._2),
//      imageList.find(_._1 == "5").map(_._2)
//    )
//    (humanObservation,layeredObservation, grid.frameCount.toInt)
//  }
}
