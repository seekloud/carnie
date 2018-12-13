package com.neo.sk.carnie.paperClient

import com.neo.sk.carnie.common.Constant
import com.neo.sk.carnie.util.{Component, JsFunc, Modal}
import com.neo.sk.carnie.paperClient.Protocol._
import com.neo.sk.carnie.paperClient.WebSocketProtocol.{CreateRoomPara, PlayGamePara, WebSocketPara}
import org.scalajs.dom
import org.scalajs.dom.html.Input

import scala.util.Random
//import org.scalajs.dom.ext.KeyCode
//import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw.{Event, HTMLAudioElement, VisibilityState}
import com.neo.sk.carnie.Main
import mhtml.{Rx, Var}
import scala.scalajs.js.Date
import io.circe.generic.auto._
import io.circe.syntax._


import scala.xml.Elem

/**
  * Created by dry on 2018/11/28.
  **/
class JoinGamePage(order: String, webSocketPara: PlayGamePara) extends Component {

  sealed case class Model(id: Int, img: String, name: String)

  sealed case class Head(id: Int, img: String)

  private var windowBoundary = Point(dom.window.innerWidth.toFloat, dom.window.innerHeight.toFloat)

  var modelLists = List(Model(0,"/carnie/static/img/coffee2.png","正常模式"),
    Model(1,"/carnie/static/img/game.png","反转模式"),Model(2,"/carnie/static/img/rocket1.png","加速模式"))
  var modelSelectMap : Map[Int,Boolean] =Map()
  var modelSelected = Model(0,"/carnie/static/img/coffee2.png","正常模式")
  //模式选择框
  private val modelList: Var[List[Model]] = Var(modelLists)
  private val modelSelectFlag: Var[Map[Int, Boolean]] = Var(Map())

  var headLists = List(Head(0, "/carnie/static/img/luffy.png"), Head(1, "/carnie/static/img/fatTiger.png"), Head(2, "/carnie/static/img/Bob.png"),
    Head(3, "/carnie/static/img/yang.png"), Head(4, "/carnie/static/img/smile.png"), Head(5, "/carnie/static/img/pig.png"))
  var headSelectMap: Map[Int, Boolean] = Map()
  var headSelected = Head(0, "/carnie/static/img/luffy.png")
  //头像选择框
  private val headList: Var[List[Head]] = Var(headLists)
  private val headSelectFlag: Var[Map[Int, Boolean]] = Var(Map())

  private val modelDiv = modelList.map { games =>
    games.map(game =>
      <div style="width:27%;margin:0px;">
        <div style="overflow:hidden" id={game.id.toString}>
          <div class={selectClass(game.id)} onclick={()=>selectGame(game.id)} style="margin-top:0px;height:150px;width:150px;">
            <img class="home-img" src={game.img}></img>
          </div>
          <p style="font-size: 15px;color:white;margin-left:15%;margin-right:15%" > {game.name}</p>
        </div>
      </div>
    )
  }

  private val headDiv = headList.map { games =>
    games.map(game =>
      <div style="width:27%;margin:15px;">
        <div style="overflow:hidden" id={game.id.toString}>
          <div class={selectHeadClass(game.id)} onclick={() => selectHead(game.id)} style="margin-top:0px;height:100px;width:100px;text-align: center">
            <img class="home-img" src={game.img}></img>
          </div>
        </div>
      </div>
    )
  }
  def init()={
    modelLists.foreach(game=>
      if(game.id == 0) modelSelectMap += (game.id -> true)
      else modelSelectMap += (game.id -> false)
    )
    modelSelectFlag := modelSelectMap
    val rnd = Random.nextInt(6)
    headLists.foreach(game =>
      if(game.id == rnd) headSelectMap += (game.id -> true)
      else headSelectMap += (game.id -> false)
    )
    headSelectFlag := headSelectMap
    headSelected = headLists(rnd)
  }

  def selectClass(id: Int): Rx[String] = modelSelectFlag.map { flag =>
    if (flag.contains(id)) {
      flag(id) match {
        case true =>"game-selected"
        case _ =>"game-not-selected"
      }
    }
    else "game-not-selected"
  }

  def selectGame(id: Int): Unit = {
    if (modelSelectMap.contains(modelSelected.id))
      modelSelectMap += (modelSelected.id -> false)
    if (modelSelectMap.contains(id))
      modelSelectMap += (id -> true)
    modelSelectFlag := modelSelectMap
    modelSelected = modelLists.find {
      _.id == id
    }.get
    println(modelSelected.name)
  }

  def selectHeadClass(id: Int): Rx[String] = headSelectFlag.map { flag =>
    if (flag.contains(id)) {
      flag(id) match {
        case true => "game-selected"
        case _ => "game-not-selected"
      }
    }
    else "game-not-selected"
  }

  def selectHead(id: Int): Unit = {
    if (headSelectMap.contains(headSelected.id))
      headSelectMap += (headSelected.id -> false)
    if (headSelectMap.contains(id))
      headSelectMap += (id -> true)
    headSelectFlag := headSelectMap
    headSelected = headLists.find {
      _.id == id
    }.get
    println(headSelected.id)
  }

  def resetScreen(): Unit = {
    if(dom.window.innerWidth.toFloat > windowBoundary.x)
    windowBoundary = Point(dom.window.innerWidth.toFloat, dom.window.innerHeight.toFloat)
  }
  def gotoGame(modelId: Int, headId: Int, playerId: String, playerName: String): Unit = {
    if (modelId == -1 || headId == -1) JsFunc.alert("请选择模式和头像!")
    else {
      Main.refreshPage(new CanvasPage().render)
      dom.document.getElementById("all").setAttribute("display","none")
      dom.document.getElementById("all").setAttribute("hidden","hidden")
      val frameRate = if(modelId==2) frameRate2 else frameRate1
      new NetGameHolder("playGame", PlayGamePara(playerId, playerName, modelId, headId), modelId, headId, frameRate).init()
    }
  }

  def switchToRoomListPage():Unit = {
    dom.document.getElementById("all").setAttribute("display","none")
    dom.document.getElementById("all").setAttribute("hidden","hidden")
    Main.refreshPage(new RoomListPage(webSocketPara).render)
  }

  def createRoomDialog() = {
    val title = <h4 class="modal-title" style="text-align: center;">创建房间</h4>
    val child =
      <div>
        <label for="pwd">房间密码:</label>
        <input type="password" id="pwd"></input>
      </div>
    new Modal(title,child,()=>createRoom(),"createRoom").render
  }

  def createRoom():Unit = {
//    println("prepareto createRoom.")
    val frameRate = if(modelSelected.id==2) frameRate2 else frameRate1
    val pwd = dom.document.getElementById("pwd").asInstanceOf[Input].value
    Main.refreshPage(new CanvasPage().render)
    new NetGameHolder("playGame", CreateRoomPara(webSocketPara.playerId, webSocketPara.playerName, pwd, modelSelected.id, headSelected.id), modelSelected.id, headSelected.id, frameRate).init()
  }

  override def render: Elem = {
    {init()}
    <div id ="all">
      <div  style="background-color: #333333;height:750px" id="body" >
        <div  id="selectPage">
          <div  id="form">
            <h1 style="font-family: Verdana;font-size:30px;color:white;text-align: center;" >欢迎来到carnie</h1>
          </div>
          <div style="overflow: hidden;" >
            <div style="display:flex;flex-direction: row;flex-wrap: wrap;justify-content: center;align-items:center;margin-left:20%;margin-right:20%;text-align:center" >
                {modelDiv}
            </div>
          </div>

          <div style="text-align: center;">
            <button type="button"   style="font-size: 30px ;" class="btn-primary" onclick=
            {() => gotoGame(modelSelected.id,headSelected.id,webSocketPara.playerId,webSocketPara.playerName)}>进入游戏</button>
          </div>

          <div style="text-align: center;">
            <button style="font-size: 30px ;" class="btn-primary" data-toggle="modal" data-target="#createRoom">创建房间</button>
            <button style="font-size: 30px ;" class="btn-primary" onclick=
            {() => switchToRoomListPage()}>房间列表</button>
            {createRoomDialog()}
          </div>

          <div style="overflow: hidden;" >
            <div style="margin-top: 5px;">
              <p style="text-align: center; margin-top: 0px;font-size: 20px;color:white" >选择头像</p>
            </div>
            <div  style="text-align: center;display: flex; flex-wrap: nowrap;margin-left:12%;margin-right:12%">
                {headDiv}
            </div>
          </div>
          </div>
      </div>
    </div>
  }

//
}
