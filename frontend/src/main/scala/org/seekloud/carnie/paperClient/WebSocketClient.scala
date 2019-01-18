package org.seekloud.carnie.paperClient

//import org.scalajs.dom.raw.CloseEvent
//import org.seekloud.carnie.model.ReplayInfo
import org.seekloud.carnie.paperClient.WebSocketProtocol._
import org.seekloud.carnie.paperClient.Protocol._
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.decoder
import org.scalajs.dom
import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw.{Blob, Event, FileReader, MessageEvent}
import org.seekloud.byteobject.MiddleBufferInJs

import scala.scalajs.js.typedarray.ArrayBuffer
import org.seekloud.byteobject.ByteObject._

/**
  * Created by dry on 2018/9/3.
  **/
class WebSocketClient (
                        connectOpenSuccess: (Event, String) => Unit,
                        connectError: Event => Unit,
                        messageHandler: GameMessage => Unit,
                        close:(Event, Boolean) => Unit
                      ) {

  private var wsSetup = false
  private var serverState = true
  private var gameStreamOpt: Option[WebSocket] = None

  def setUp(order: String, setupInfo: WebSocketPara): Unit = {
    if (!wsSetup) {
      val url = setupInfo match {
        case p: PlayGamePara => getWebSocketUri(p.playerId, p.playerName, p.mode, p.img)
        case p: ReJoinGamePara => getWebSocketUri4Rejoin(p.playerId, p.playerName, p.mode, p.img)
        case p: WatchGamePara => getWebSocketUri4WatchGame(p.roomId, p.playerId, p.accessCode)
        case p: WatchRecordPara => getWebSocketUri4WatchRecord(p.recordId, p.playerId, p.frame, p.accessCode)
        case p: joinRoomByIdPara => getWebSocketUri4JoinRoomById(p.playerId, p.playerName, p.pwd, p.mode, p.img,p.roomId)
        case p: CreateRoomPara => getWebSocketUri4CreateRoom(p.playerId, p.playerName, p.pwd, p.mode, p.img)
      }

      val gameStream = new WebSocket(url)
      gameStreamOpt = Some(gameStream)

      gameStream.onopen = { event0: Event =>
        wsSetup = true
        connectOpenSuccess(event0, order)
      }

      gameStream.onerror = { event: Event =>
        wsSetup = false
        serverState = false
        gameStreamOpt = None
        connectError(event)
      }

      gameStream.onclose = { event: Event =>
        println(s"ws close========$event")
        wsSetup = false
        gameStreamOpt = None
        close(event, serverState)
      }

      var snakeAction : Double = 0
      var ping : Double = 0
      var newField : Double = 0
      var data4TotalSync : Double = 0
      var rank : Double = 0
      var newSnakeInfo : Double = 0
      var dead :Double = 0
      var win :Double = 0
      var other :Double = 0
      var updateTime = 0l
      var newData: Double = 0

      gameStream.onmessage = { event: MessageEvent =>
        event.data match {
          case blobMsg: Blob =>
            val fr = new FileReader()
            fr.readAsArrayBuffer(blobMsg)
            fr.onloadend = { _: Event =>
              val middleDataInJs = new MiddleBufferInJs(fr.result.asInstanceOf[ArrayBuffer]) //put data into MiddleBuffer
            val encodedData: Either[decoder.DecoderFailure, Protocol.GameMessage] = bytesDecode[Protocol.GameMessage](middleDataInJs) // get encoded data.
              encodedData match {
                case Right(data) =>
                  data match {
                    case ReceivePingPacket(_) =>
                      ping = ping + blobMsg.size

                    case SnakeAction(_, _, _, _) =>
                      snakeAction = snakeAction + blobMsg.size

                    case OtherAction(_,_,_) =>
                      snakeAction = snakeAction + blobMsg.size

                    case NewData(_, newSnakes, newField) =>
                      newData = newData + blobMsg.size

                    case NewFieldInfo(_, _) =>
                      newField = newField + blobMsg.size

                    case Data4TotalSync(_, _, _, _) =>
                      data4TotalSync = data4TotalSync + blobMsg.size

                    case Ranks(_, _, _, _) =>
                      rank = rank + blobMsg.size

                    case NewSnakeInfo(_, _) =>
                      newSnakeInfo = newSnakeInfo + blobMsg.size

                    case DeadPage(_, _, _) =>
                      dead = dead + blobMsg.size

                    case WinData(_, _, _) =>
                      win = win + blobMsg.size

                    case CloseWs =>
                      gameStream.close()
                      wsSetup = false
                      gameStreamOpt = None
                      close(event, serverState)

                    case _ =>
                      other = other + blobMsg.size
                  }
                  if(System.currentTimeMillis() - updateTime > 30*1000) {
                    updateTime = System.currentTimeMillis()
                    println(s"statistics!!!!!ping:$ping,snakeAction:$snakeAction,newData:$newData,newField:$newField,data4TotalSync$data4TotalSync,rank:$rank,newSnakeInfo:$newSnakeInfo, dead$dead, win:$win,other:$other")
                    snakeAction = 0
                    ping = 0
                    newField = 0
                    data4TotalSync = 0
                    rank = 0
                    newSnakeInfo = 0
                    dead = 0
                    win = 0
                    other = 0
                  }
                  messageHandler(data)

                case Left(e) =>
                  println(s"got error: ${e.message}")
              }
            }
        }
      }
    }
  }

  val sendBuffer = new MiddleBufferInJs(409600) //sender buffer

  def sendMessage(msg: UserAction): Unit = {
    gameStreamOpt match {
      case Some(gameStream) =>
        gameStream.send(msg.fillMiddleBuffer(sendBuffer).result())

      case None => //
    }
  }

  def getWebSocketUri(id: String, name: String, mode: Int, img: Int): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/carnie/join?id=$id&name=$name&mode=$mode&img=$img"
  }

  def getWebSocketUri4Rejoin(id: String, name: String, mode: Int, img: Int): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/carnie/reJoin?id=$id&name=$name&mode=$mode&img=$img"
  }

  def getWebSocketUri4CreateRoom(id: String, name: String, pwd: String, mode: Int, img: Int): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/carnie/createRoom?id=$id&name=$name&mode=$mode&img=$img&pwd=$pwd"
  }

  def getWebSocketUri4WatchGame(roomId: String, playerId: String, accessCode: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/carnie/observeGame?roomId=$roomId&playerId=$playerId&accessCode=$accessCode"
  }

  def getWebSocketUri4WatchRecord(recordId: String, playerId: String, frame: String, accessCode: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/carnie/joinWatchRecord?recordId=$recordId&playerId=$playerId&frame=$frame&accessCode=$accessCode"
  }

  def getWebSocketUri4JoinRoomById(id: String, name: String, pwd: String, mode: Int, img: Int, roomId: Int): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/carnie/joinGameById?id=$id&name=$name&mode=$mode&img=$img&roomId=$roomId"
  }

  def getWsState:Boolean = wsSetup


}