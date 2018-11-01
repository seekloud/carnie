package com.neo.sk.carnie.core

import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.carnie.paperClient.Protocol._
import org.slf4j.LoggerFactory
import com.neo.sk.carnie.paperClient._
import org.seekloud.byteobject._
import com.neo.sk.carnie.Boot.roomManager
import com.neo.sk.carnie.core.GameRecorder.RecordData
import com.neo.sk.utils.EsheepClient

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import concurrent.duration._

/**
  * Created by dry on 2018/10/12.
  **/
object RoomActor {

  private val log = LoggerFactory.getLogger(this.getClass)
  val border = Point(BorderSize.w, BorderSize.h)
  private val fullSize = (BorderSize.w - 2) * (BorderSize.h - 2)

  private final case object BehaviorChangeKey

  private final case object SyncKey

  sealed trait Command

  case class UserActionOnServer(id: String, action: Protocol.UserAction) extends Command

  case class JoinRoom(id: String, name: String, subscriber: ActorRef[WsSourceProtocol.WsMsgSource]) extends Command

  case class LeftRoom(id: String, name: String) extends Command

//  case class WatcherLeftRoom(playerId: String) extends Command

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  case class WatchGame(uid: String, subscriber: ActorRef[WsSourceProtocol.WsMsgSource]) extends Command

  final case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  private case object Sync extends Command

  private val watcherIdGenerator = new AtomicInteger(100)


  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg: String) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(roomId: Int): Behavior[Command] = {
    log.debug(s"Room Actor-$roomId start...")
    Behaviors.setup[Command] { ctx =>
      Behaviors.withTimers[Command] {
        implicit timer =>
          val subscribersMap = mutable.HashMap[String, ActorRef[WsSourceProtocol.WsMsgSource]]()
          val userMap = mutable.HashMap[String, String]()
          val watcherMap = mutable.HashMap[String, String]()
          val grid = new GridOnServer(border)
          val winStandard = (BorderSize.w - 2) * (BorderSize.h - 2) * 0.7
          //            implicit val sendBuffer = new MiddleBufferInJvm(81920)
          timer.startPeriodicTimer(SyncKey, Sync, Protocol.frameRate millis)
          idle(roomId, grid, userMap, watcherMap, subscribersMap, 0L, mutable.ArrayBuffer[(Long, GameEvent)](), winStandard)
      }
    }
  }

  def idle(
            roomId: Int, grid: GridOnServer,
            userMap: mutable.HashMap[String, String],
            watcherMap: mutable.HashMap[String, String],//(watchId, playerId)
            subscribersMap: mutable.HashMap[String, ActorRef[WsSourceProtocol.WsMsgSource]],
            tickCount: Long,
            gameEvent: mutable.ArrayBuffer[(Long, GameEvent)],
            winStandard: Double
          )(
            implicit timer: TimerScheduler[Command]
          ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case JoinRoom(id, name, subscriber) =>
          log.info(s"got JoinRoom $msg")
          log.info(s"joinRoom roomId: $roomId")
          userMap.put(id, name)
          subscribersMap.put(id, subscriber)
          ctx.watchWith(subscriber, UserLeft(subscriber))
          grid.addSnake(id, roomId, name)
          dispatchTo(subscribersMap, id, Protocol.Id(id))
          val gridData = grid.getGridData
          dispatch(subscribersMap, gridData)
          gameEvent += ((grid.frameCount, JoinEvent(id, name)))
          Behaviors.same

        case WatchGame(playerId, subscriber) =>
          val watchId = watcherIdGenerator.getAndIncrement().toString
//          val randomInt = new Random().nextInt(userMap.toList.length)
          val truePlayerId = if(playerId == "unknown") userMap.head._1 else playerId
          val playerName = userMap.getOrElse(truePlayerId, "unKnown")
          watcherMap.put(watchId, truePlayerId)
          subscribersMap.put(watchId, subscriber)
          dispatchTo(subscribersMap, watchId, Protocol.Id(truePlayerId))
//          ctx.watchWith(subscriber, UserLeft(subscriber)) //此行不删
//          ctx.watchWith(subscriber, LeftRoom(truePlayerId, playerName))//此行不删
          val gridData = grid.getGridData
          dispatch(subscribersMap, gridData)
          Behaviors.same

        case LeftRoom(id, name) =>
          log.debug(s"LeftRoom:::$id")
          grid.removeSnake(id)
          subscribersMap.get(id).foreach(r => ctx.unwatch(r))
          userMap.remove(id)
          subscribersMap.remove(id)
          watcherMap.filter(_._2 == id).keySet.foreach {i =>
            subscribersMap.remove(i)
          }
          gameEvent += ((grid.frameCount, LeftEvent(id, name)))
          if (userMap.isEmpty) Behaviors.stopped else Behaviors.same

        case UserLeft(actor) =>
          log.debug(s"UserLeft:::")
          subscribersMap.find(_._2.equals(actor)).foreach { case (id, _) =>
            log.debug(s"got Terminated id = $id")
            val name = userMap.get(id).head
            subscribersMap.remove(id)
            userMap.remove(id)
            grid.removeSnake(id).foreach { s => dispatch(subscribersMap, Protocol.SnakeLeft(id, s.name)) }
            roomManager ! RoomManager.UserLeft(id)
            gameEvent += ((grid.frameCount, LeftEvent(id, name)))
          }
          if (userMap.isEmpty) Behaviors.stopped else Behaviors.same


        case UserActionOnServer(id, action) =>
          action match {
            case Key(_, keyCode, frameCount, actionId) =>
              if (keyCode == KeyEvent.VK_SPACE) {
                grid.addSnake(id, roomId, userMap.getOrElse(id, "Unknown"))
                watcherMap.filter(_._2 == id).foreach { w =>
                  dispatchTo(subscribersMap, w._1, Protocol.ReStartGame)
                }
                gameEvent += ((grid.frameCount, SpaceEvent(id)))
              } else {
                val realFrame = if (frameCount >= grid.frameCount) frameCount else grid.frameCount
                grid.addActionWithFrame(id, keyCode, realFrame)
                dispatch(subscribersMap, Protocol.SnakeAction(id, keyCode, realFrame, actionId))
              }
            case SendPingPacket(_, createTime) =>
              dispatchTo(subscribersMap, id, Protocol.ReceivePingPacket(createTime))

            case NeedToSync(_) =>
              dispatchTo(subscribersMap, id, grid.getGridData)

            case _ =>
          }

          Behaviors.same

        case Sync =>
          val frame = grid.frameCount //即将执行改帧的数据
          val shouldNewSnake = if (grid.waitingListState) true else if (tickCount % 20 == 5) true else false
          val snapshotData = grid.getGridData
          val finishFields = grid.updateInService(shouldNewSnake) //frame帧的数据执行完毕
          val newData = grid.getGridData
          var newField: List[FieldByColumn] = Nil

          newData.killHistory.foreach { i =>
            if (i.frameCount + 1 == newData.frameCount) {
              dispatch(subscribersMap, Protocol.SomeOneKilled(i.killedId, userMap(i.killedId), i.killerName))
            }
          }

          if (shouldNewSnake) dispatch(subscribersMap, newData)
          else if (finishFields.nonEmpty) {
            val finishUsers = finishFields.map(_._1)
            finishUsers.foreach(u => dispatchTo(subscribersMap, u, newData))
            newField = finishFields.map { f =>
              FieldByColumn(f._1, f._2.groupBy(_.y).map { case (y, target) =>
                ScanByColumn(y.toInt, Tool.findContinuous(target.map(_.x.toInt).toArray.sorted))
              }.toList)
            }
            userMap.filterNot(user => finishUsers.contains(user._1)).foreach(u => dispatchTo(subscribersMap, u._1, NewFieldInfo(grid.frameCount, newField)))
          }
          if (tickCount % 10 == 3) dispatch(subscribersMap, Protocol.Ranks(grid.currentRank))
          val newWinStandard = if (grid.currentRank.nonEmpty) { //胜利条件的跳转
            val maxSize = grid.currentRank.head.area
            if ((maxSize + fullSize * 0.1) < winStandard) fullSize * (0.2 - userMap.size * 0.05) else winStandard
          } else winStandard
          if (grid.currentRank.nonEmpty && grid.currentRank.head.area >= winStandard) {
            val finalData = grid.getGridData
            grid.cleanData()
            dispatch(subscribersMap, Protocol.SomeOneWin(userMap(grid.currentRank.head.id), finalData))
          }

          //for gameRecorder...
          val actionEvent = grid.getDirectionEvent(frame)
          val joinOrLeftEvent = gameEvent.filter(_._1 == frame)
          val baseEvent = if (tickCount % 10 == 3) RankEvent(grid.currentRank) :: (actionEvent ::: joinOrLeftEvent.map(_._2).toList) else actionEvent ::: joinOrLeftEvent.map(_._2).toList
          gameEvent --= joinOrLeftEvent
          val snapshot = Snapshot(snapshotData.snakes, snapshotData.bodyDetails, snapshotData.fieldDetails, snapshotData.killHistory)
          val recordData = if (finishFields.nonEmpty) RecordData(frame, (EncloseEvent(newField) :: baseEvent, snapshot)) else RecordData(frame, (baseEvent, snapshot))
          getGameRecorder(ctx, roomId, grid) ! recordData
          idle(roomId, grid, userMap, watcherMap, subscribersMap, tickCount + 1, gameEvent, newWinStandard)

        case ChildDead(child, childRef) =>
          log.debug(s"roomActor 不再监管 gameRecorder:$child,$childRef")
          ctx.unwatch(childRef)
          Behaviors.same

        case _ =>
          log.warn(s"${ctx.self.path} recv a unknow msg=$msg")
          Behaviors.same
      }
    }

  }

  def dispatchTo(subscribers: mutable.HashMap[String, ActorRef[WsSourceProtocol.WsMsgSource]], id: String, gameOutPut: Protocol.GameMessage): Unit = {
    subscribers.get(id).foreach {
      _ ! gameOutPut
    }
  }

  def dispatch(subscribers: mutable.HashMap[String, ActorRef[WsSourceProtocol.WsMsgSource]], gameOutPut: Protocol.GameMessage) = {
    //    log.info(s"dispatch:::$gameOutPut")
    subscribers.values.foreach {
      _ ! gameOutPut
    }
  }

  private def getGameRecorder(ctx: ActorContext[Command], roomId: Int, grid: GridOnServer): ActorRef[GameRecorder.Command] = {
    val childName = "gameRecorder"
    ctx.child(childName).getOrElse {
      val newData = grid.getGridData
      val actor = ctx.spawn(GameRecorder.create(roomId, Snapshot(newData.snakes, newData.bodyDetails, newData.fieldDetails, newData.killHistory),
        GameInformation(roomId, System.currentTimeMillis(), 0, grid.frameCount)), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.upcast[GameRecorder.Command]
  }

}