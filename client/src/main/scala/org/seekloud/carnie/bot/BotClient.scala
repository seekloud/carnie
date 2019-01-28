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

package org.seekloud.carnie.bot

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.seekloud.esheepapi.pb.api.{CreateRoomReq, CreateRoomRsp, Credit, ObservationRsp, SimpleRsp}
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgentStub
import scala.concurrent.Future

/**
  * Created by dry on 2018/11/29.
  **/

//内部测试用
class BotClient (
                  host: String,
                  port: Int,
                  playerId: String,
                  apiToken: String
                ) {

  private[this] val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build

  private val esheepStub: EsheepAgentStub = EsheepAgentGrpc.stub(channel)

  val credit = Credit(apiToken = apiToken)


  def createRoom(): Future[CreateRoomRsp] = esheepStub.createRoom(CreateRoomReq(Some(credit), "password"))

  def observation(): Future[ObservationRsp] = esheepStub.observation(credit)
}

object BotClient{


  def main(args: Array[String]): Unit = {
    //import concurrent.ExecutionContext.Implicits.global

    val host = "127.0.0.1"
    val port = 5321
    val playerId = "gogo"
    val apiToken = "lala"

    val client = new BotClient(host, port, playerId, apiToken)

    val rsp1 = client.createRoom()

    val rsp2 = client.observation()

    println("--------  begin sleep   ----------------")
    Thread.sleep(10000)
    println("--------  end sleep   ----------------")

    println(rsp1)
    println("------------------------")
    println(rsp2)
    println("client DONE.")

  }
}
