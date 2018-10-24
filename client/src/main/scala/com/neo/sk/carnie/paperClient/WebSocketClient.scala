package com.neo.sk.carnie.paperClient

import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.carnie.paperClient.Protocol._
import com.neo.sk.carnie.paperClient.WsSourceProtocol.{CompleteMsgServer, FailMsgServer, WsMsgSource}
import org.seekloud.byteobject.ByteObject.bytesDecode
import akka.actor.ActorSystem
import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSink
import akka.util.ByteString
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.byteobject.ByteObject._
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.carnie.Boot.{executor, materializer, system}

/**
  * Created by dry on 2018/10/23.
  **/
object WebSocketClient {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  sealed trait WsCommand

  case class ConnectGame(id: String, name: String, accessCode: String, domain: String) extends WsCommand

  def create(): Behavior[WsCommand] = {
    Behaviors.setup[WsCommand] { ctx =>
      val id = "testId"
      val name = "testName"
      val accessCode = "testAccessCode"
      val domain = "testDomain"
      val gameController = ctx.spawn(NetGameHolder.running(id, name), "gameController")
      ctx.self ! ConnectGame(id, name, accessCode, domain)
      idle(gameController)
    }
  }

  def idle(actor: ActorRef[WsMsgSource]): Behavior[WsCommand] = {
    Behaviors.receive[WsCommand] { (ctx, msg) =>
      msg match {
        case ConnectGame(id, name, accessCode, domain) =>
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(getWebSocketUri(id, name, accessCode, domain)))
          val source = getSource
          val ((stream, response), closed) =
            source
              .viaMat(webSocketFlow)(Keep.both) // keep the materialized Future[WebSocketUpgradeResponse]
              .toMat(getSink(actor))(Keep.both) // also keep the Future[Done]
              .run()
          val connected = response.flatMap { upgrade =>
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
              Future.successful("connect success")
            } else {
              throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
            }
          } //链接建立时

          connected.onComplete(i => log.info(i.toString))
          Behaviors.same
      }
    }
  }

  private[this] def getSink(actor: ActorRef[WsMsgSource]) =
    Flow[Message].collect {
      case TextMessage.Strict(msg) =>
        log.debug(s"msg from webSocket: $msg")
        TextMsg(msg)

      case BinaryMessage.Strict(bMsg) =>
        //decode process.
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val msg =
          bytesDecode[GameMessage](buffer) match {
            case Right(v) => v
            case Left(e) =>
              println(s"decode error: ${e.message}")
              TextMsg("decode error")
          }
        msg
    }.to(ActorSink.actorRef[WsMsgSource](actor, CompleteMsgServer, FailMsgServer))

  private[this] def getSource = ActorSource.actorRef[WsSendMsg](
    completionMatcher = {
      case WsSendComplete =>
    }, failureMatcher = {
      case WsSendFailed(ex) ⇒ ex
    },
    bufferSize = 8,
    overflowStrategy = OverflowStrategy.fail
  ).collect {
    case message: UserAction =>
      val sendBuffer = new MiddleBufferInJvm(409600)
      BinaryMessage.Strict(ByteString(
        message.fillMiddleBuffer(sendBuffer).result()
      ))

  }

  def getWebSocketUri(domain: String, playerId: String, playerName: String, accessCode: String): String = {
    val wsProtocol = "ws"
    s"$wsProtocol://$domain/carnie/joinGameClient?playerId=$playerId&playerName=$playerName&accessCode=$accessCode"
  }
}
