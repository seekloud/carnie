package com.neo.sk.carnie.common

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

/**
  * Created by dry on 2018/10/23.
  **/
object AppSetting {

  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig = config.getConfig("app")
  val gameDataDirectoryPath = appConfig.getString("gameDataDirectoryPath")

  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")


  val appSecureMap = {
    import collection.JavaConverters._
    val appIds = appConfig.getStringList("client.appIds").asScala
    val secureKeys = appConfig.getStringList("client.secureKeys").asScala
    require(appIds.length == secureKeys.length, "appIdList.length and secureKeys.length not equel.")
    appIds.zip(secureKeys).toMap
  }

  val esheepConfig = config.getConfig("dependence.esheep")
  val esheepProtocol = esheepConfig.getString("protocol")
  val esheepDomain = esheepConfig.getString("domain")
  val esheepUrl = esheepConfig.getString("url")
  val esheepAppId = esheepConfig.getString("appId")
  val esheepSecureKey = esheepConfig.getString("secureKey")
  val esheepGameId = esheepConfig.getLong("gameId")
  val esheepGsKey = esheepConfig.getString("gsKey")

  val slickConfig = config.getConfig("slick.db")
  val slickUrl = slickConfig.getString("url")
  val slickUser = slickConfig.getString("user")
  val slickPassword = slickConfig.getString("password")
  val slickMaximumPoolSize = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime = slickConfig.getInt("maxLifetime")

}