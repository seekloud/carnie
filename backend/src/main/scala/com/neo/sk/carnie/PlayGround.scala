package com.neo.sk.carnie

import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:29 PM
  */


trait PlayGround {


  def joinGame(id: Long, name: String): Flow[String, Protocol.GameMessage, Any]

  def syncData()

}


object PlayGround {

  val border = Point(BorderSize.w, BorderSize.h)

  val log = LoggerFactory.getLogger(this.getClass)

  val roomIdGen = new AtomicInteger(100)

  private val limitNum = 10

  private val winStandard = (BorderSize.w - 2)* (BorderSize.h - 2) * 0.01

  def create(system: ActorSystem)(implicit executor: ExecutionContext): PlayGround = {

    val ground = system.actorOf(Props(new Actor {
      var subscribers = Map.empty[Long, ActorRef]

      var userMap = Map.empty[Long, (Int, String)] //(userId, (roomId, name))

      var roomMap = Map.empty[Int, (Int, GridOnServer)] //(roomId, (roomNum, grid))

      var tickCount = 0l

      override def receive: Receive = {
        case r@Join(id, name, subscriber) =>
          log.info(s"got $r")
          val roomId = if (roomMap.isEmpty) {
            val grid = new GridOnServer(border)
            val newRoomId = roomIdGen.get()
            roomMap += (newRoomId -> (0, grid))
            newRoomId
          } else {
            if (roomMap.exists(_._2._1 < limitNum)) {
              roomMap.filter(_._2._1 < limitNum).head._1
            } else {
              val grid = new GridOnServer(border)
              val newRoomId = roomMap.maxBy(_._1)._1 + 1
              roomMap += (newRoomId -> (0, grid))
              newRoomId
            }
          }
          userMap += (id -> (roomId, name))
          roomMap += (roomId -> (roomMap(roomId)._1 + 1, roomMap(roomId)._2))
          context.watch(subscriber)
          subscribers += (id -> subscriber)
          roomMap(roomId)._2.addSnake(id, roomId, name)
          dispatchTo(id, Protocol.Id(id))
          dispatch(Protocol.NewSnakeJoined(id, name), roomId)
          dispatch(roomMap(roomId)._2.getGridData, roomId)

        case r@Left(id, name) =>
          log.info(s"got $r")
          val roomId = userMap(id)._1
          val newUserNum = roomMap(roomId)._1 - 1
          roomMap(roomId)._2.removeSnake(id)
          if (newUserNum <= 0) roomMap -= roomId else roomMap += (roomId -> (newUserNum, roomMap(roomId)._2))
          userMap -= id
          subscribers.get(id).foreach(context.unwatch)
          subscribers -= id
          dispatch(Protocol.SnakeLeft(id, name), roomId)

        case r@Key(id, keyCode) =>
          log.debug(s"got $r")
          val roomId = userMap(id)._1
          dispatch(Protocol.TextMsg(s"Aha! $id click [$keyCode]"), roomId) //just for test
          if (keyCode == KeyEvent.VK_SPACE) {
            roomMap(roomId)._2.addSnake(id, roomId, userMap.getOrElse(id, (0, "Unknown"))._2)
          } else {
            roomMap(roomId)._2.addAction(id, keyCode)
            dispatch(Protocol.SnakeAction(id, keyCode, roomMap(roomId)._2.frameCount), roomId)
          }

        case r@Terminated(actor) =>
          log.warn(s"got $r")
          subscribers.find(_._2.equals(actor)).foreach { case (id, _) =>
            log.debug(s"got Terminated id = $id")
            val roomId = userMap(id)._1
            userMap -= id
            subscribers -= id
            roomMap(roomId)._2.removeSnake(id).foreach(s => dispatch(Protocol.SnakeLeft(id, s.name), roomId))
            val newUserNum = roomMap(roomId)._1 - 1
            if (newUserNum <= 0) roomMap -= roomId else roomMap += (roomId -> (newUserNum, roomMap(roomId)._2))
          }

        case Sync =>
          tickCount += 1
          roomMap.foreach { r =>
            if (userMap.filter(_._2._1 == r._1).keys.nonEmpty) {
              r._2._2.update()
              if (tickCount % 20 == 5) {
                val gridData = r._2._2.getGridData
                dispatch(gridData, r._1)
              }
              if(tickCount % 3 == 1) dispatch(Protocol.Ranks(r._2._2.currentRank, r._2._2.historyRankList), r._1)
              if(r._2._2.currentRank.nonEmpty && r._2._2.currentRank.head.area >= winStandard) {
                r._2._2.cleanData()
                dispatch(Protocol.SomeOneWin(userMap(r._2._2.currentRank.head.id)._2), r._1)
              }
            }
          }

        case NetTest(id, createTime) =>
          log.info(s"Net Test: createTime=$createTime")
          dispatchTo(id, Protocol.NetDelayTest(createTime))

        case x =>
          log.warn(s"got unknown msg: $x")
      }

      def dispatchTo(id: Long, gameOutPut: Protocol.GameMessage): Unit = {
        subscribers.get(id).foreach { ref => ref ! gameOutPut }
      }

      def dispatch(gameOutPut: Protocol.GameMessage, roomId: Long) = {
        val user = userMap.filter(_._2._1 == roomId).keys.toList
        subscribers.foreach { case (id, ref) if user.contains(id) => ref ! gameOutPut case _ =>}
      }
    }
    ), "ground")

    import concurrent.duration._
    system.scheduler.schedule(3 seconds, Protocol.frameRate millis, ground, Sync) // sync tick


    def playInSink(id: Long, name: String) = Sink.actorRef[UserAction](ground, Left(id, name))


    new PlayGround {
      override def joinGame(id: Long, name: String): Flow[String, Protocol.GameMessage, Any] = {
        val in =
          Flow[String]
            .map { s =>
              if (s.startsWith("T")) {
                val timestamp = s.substring(1).toLong
                NetTest(id, timestamp)
              } else {
                Key(id, s.toInt)
              }
            }
            .to(playInSink(id, name)) //这里不会发left消息吗?有什么特殊执行方式？

        val out =
          Source.actorRef[Protocol.GameMessage](3, OverflowStrategy.dropHead)
            .mapMaterializedValue(outActor => ground ! Join(id, name, outActor))

        Flow.fromSinkAndSource(in, out) //用法?
      }

      override def syncData(): Unit = ground ! Sync
    }

  }


  private sealed trait UserAction

  private case class Join(id: Long, name: String, subscriber: ActorRef) extends UserAction

  private case class Left(id: Long, name: String) extends UserAction

  private case class Key(id: Long, keyCode: Int) extends UserAction

  private case class NetTest(id: Long, createTime: Long) extends UserAction

  private case object Sync extends UserAction


}