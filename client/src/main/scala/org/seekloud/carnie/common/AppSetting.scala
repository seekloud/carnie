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

import java.io.File
import com.typesafe.config.ConfigFactory

/**
  * Created by dry on 2018/10/23.
  **/
object AppSetting {

  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val esheepConfig = config.getConfig("dependence.esheep")
  val esheepProtocol = esheepConfig.getString("protocol")
  val esheepDomain = esheepConfig.getString("domain")
  val esheepGameId = esheepConfig.getLong("gameId")
  val esheepGsKey = esheepConfig.getString("gsKey")

  val file = new File("bot.conf")
  if (file.isFile && file.exists) {
    val botConfig = ConfigFactory.parseResources("bot.conf").withFallback(ConfigFactory.load())

    val appConfig = botConfig.getConfig("app")

    val userConfig = appConfig.getConfig("user")
    val email = userConfig.getString("email")
    val psw = userConfig.getString("psw")

    val render = appConfig.getBoolean("render")
  }

}
