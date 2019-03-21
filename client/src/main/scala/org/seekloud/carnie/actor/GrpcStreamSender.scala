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

package org.seekloud.carnie.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.grpc.stub.StreamObserver
import org.seekloud.carnie.bot.BotServer
import org.seekloud.carnie.controller.BotController
import org.seekloud.carnie.paperClient.Score
import org.seekloud.esheepapi.pb.api.{CurrentFrameRsp, ObservationRsp, ObservationWithInfoRsp, State}
import org.seekloud.esheepapi.pb.observations.{ImgData, LayeredObservation}
import org.slf4j.LoggerFactory

/**
  * Created by dry on 2019/1/18.
  **/
object GrpcStreamSender {

  private[this] val log = LoggerFactory.getLogger("GrpcStreamSender")

  sealed trait Command

  case class ObservationObserver(observationObserver: StreamObserver[ObservationWithInfoRsp]) extends Command

  case class NewObservation(image: Option[ImgData], layeredObservation: Option[LayeredObservation], score: Score, frame: Int, isAlive: Boolean) extends Command

  case object LeaveRoom extends Command


  def create(gameController: BotController): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      val oStream = new StreamObserver[ObservationWithInfoRsp] {
        override def onNext(value: ObservationWithInfoRsp): Unit = {}

        override def onCompleted(): Unit = {}

        override def onError(t: Throwable): Unit = {}
      }
      working(gameController, oStream)
    }
  }


  def working(botController: BotController, obServer: StreamObserver[ObservationWithInfoRsp]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case ObservationObserver(observationObserver) =>
          working(botController, observationObserver)

        case NewObservation(image, layeredObservation, score, frame, isAlive) =>
          val rsp = if (isAlive) {//in game
            BotServer.state = State.in_game
            ObservationWithInfoRsp(layeredObservation, image, score.area, score.k, frameIndex = frame, state = BotServer.state, msg = "ok")
          } else { //killed
            BotServer.state = State.killed
            ObservationWithInfoRsp(frameIndex = frame, errCode = 10004, state = BotServer.state, msg = s"not in_game state")
          }

          try {
            obServer.onNext(rsp)
            Behavior.same
          } catch {
            case e: Exception =>
              log.warn(s"obServer error: ${e.getMessage}")
              Behavior.stopped
          }

        case LeaveRoom =>
          obServer.onCompleted()
          Behaviors.stopped
      }
    }
  }

}
