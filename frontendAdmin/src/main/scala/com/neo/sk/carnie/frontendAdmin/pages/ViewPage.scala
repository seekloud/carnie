package com.neo.sk.carnie.frontendAdmin.pages

import com.neo.sk.carnie.frontendAdmin.Routes
import com.neo.sk.carnie.frontendAdmin.util.{Http, JsFunc, Page}
import com.neo.sk.carnie.ptcl.AdminPtcl.{PageReq, PlayerRecord, PlayerRecordRsp}
import com.neo.sk.carnie.frontendAdmin.util.TimeTool
import mhtml.Var
import org.scalajs.dom.raw.MouseEvent

import scala.xml.Elem
import scala.concurrent.ExecutionContext.Implicits.global
//import org.scalajs.dom
//import org.scalajs.dom.html.{Button, Input}
//import org.scalajs.dom.raw.KeyboardEvent
import io.circe.generic.auto._
import io.circe.syntax._
//
//import scala.xml.{Elem, Node}
/**
  * User: Jason
  * Date: 2018/12/18
  * Time: 13:04
  */
object ViewPage extends Page{
  override val locationHashString: String = "#/View"

  val playerRecordsVar = Var(List.empty[PlayerRecord])

  var playerAmount = 0

  var playerAmountToday= 0

  private var page = 1

  private val pageVar = Var(1,1)//left:page,right:pageNum

  private val pageLimit = 5

  val isGetAmount = Var(false)

  val pageRx = pageVar.map{
    i => i._1
  }

  val pageNumRx = pageVar.map{
    i =>
      if(i._2%pageLimit == 0)
        i._2/pageLimit
      else
        i._2/pageLimit + 1
  }

  val upBtnRx = pageVar.map {
    i =>
      if(i._1>1)
        None
      else
        Some("disabled")
  }

  val downBtnRx = pageVar.map {
    i =>
      if(i._1*pageLimit<i._2)
        None
      else
        Some("disabled")
  }
  val upBtn = <button class="btn btn-default" onclick={(e:MouseEvent) => e.preventDefault();getPlayerRecord(page-1)} disabled={upBtnRx}>上一页</button>
  val downBtn = <button class="btn btn-default" onclick={(e:MouseEvent) => e.preventDefault();getPlayerRecord(page+1)} disabled={downBtnRx}>下一页</button>


  private val playerRecordsRx = playerRecordsVar.map{
    case Nil =>
      <div>
        <p style="text-align:center;">
          没有游戏记录
        </p>
      </div>
    case other =>
      showplayerRecords(other)
  }

  def showplayerRecords(list: List[PlayerRecord]) = {
    <div>
      {
      list.map {
        i =>
          showPlayerRecord(i)
      }
      }
      <div class="row" style="text-align:center;padding: 1rem 1rem 2rem 2rem;">
        {upBtn}{pageRx}/{pageNumRx}{downBtn}
      </div>
    </div>
  }

  private def showPlayerRecord(record: PlayerRecord) = {
    <div class="row" style="padding: 1rem 1rem 2rem 2rem;">
      <div class="col-xs-1" style="text-align:center;">
        {record.id}
      </div>
      <div class="col-xs-2" style="text-align:center;">
        {record.playerId}
      </div>
      <div class="col-xs-1" style="text-align:center;">
        {record.nickname}
      </div>
      <div class="col-xs-1" style="text-align:center;">
        {record.killing}
      </div>
      <div class="col-xs-1" style="text-align:center;">
        {record.killed}
      </div>
      <div class="col-xs-2" style="text-align:center;">
        {record.score}
      </div>
      <div class="col-xs-2" style="text-align:center;">
        {TimeTool.dateFormatDefault(record.startTime)}
      </div>
      <div class="col-xs-2" style="text-align:center;">
        {TimeTool.dateFormatDefault(record.endTime)}
      </div>
      <hr></hr>
      <br></br>
    </div>
  }


  def getPlayerRecord(page: Int) : Unit = {
    val url = Routes.Admin.getPlayerRecord
    val data = PageReq(page).asJson.noSpaces
    Http.postJsonAndParse[PlayerRecordRsp](url,data).map{
      case Right(rsp) =>
        try {
          if (rsp.errCode == 0) {
            playerRecordsVar := rsp.data
            playerAmount = rsp.playerAmount
            playerAmountToday = rsp.playerAmountToday
            isGetAmount := true
            ViewPage.page = page
            if(rsp.data.nonEmpty)
              pageVar := (page, rsp.playerAmount)
            else
              pageVar := (page, 0)
          }
          else {
            println("error======" + rsp.msg)
            JsFunc.alert(rsp.msg)
          }
        }
        catch {
          case e: Exception =>
            println(e)
        }

      case Left(e) =>
        println("error======" + e)
        JsFunc.alert("get player records error!")
    }
  }

  override def render: Elem = {
    getPlayerRecord(1)
    <div>
      <div class="container">
        <h1 style="text-align:center;font-family:楷体;">历史玩家记录</h1>
        <div>
          {
          isGetAmount.map{
            case true => <div style="font-family:楷体;font-size:20px;">历史玩家总数:{playerAmount},今日玩家数:{playerAmountToday}。</div>
            case _ => <div></div>
          }
          }
        </div>
        <br></br>
        <h4 style="padding-left: 100px;"><b>游戏记录：</b></h4>
        <div class="row" style="padding: 1rem 1rem 2rem 2rem;">
          <div class="col-xs-1" style="text-align:center;">
            <label style="text-align:center">id:</label>
          </div>
          <div class="col-xs-2" style="text-align:center;">
            <label style="text-align:center">playerId:</label>
          </div>
          <div class="col-xs-1" style="text-align:center;">
            <label style="text-align:right">nickname:</label>
          </div>
          <div class="col-xs-1" style="text-align:center;">
            <label style="text-align:right">killing:</label>
          </div>
          <div class="col-xs-1" style="text-align:center;">
            <label style="text-align:right">killed:</label>
          </div>
          <div class="col-xs-2" style="text-align:center;">
            <label style="text-align:right">score:</label>
          </div>
          <div class="col-xs-2" style="text-align:center;overflow:auto;">
            <label style="text-align:right">startTime:</label>
          </div>
          <div class="col-xs-2" style="text-align:center;overflow:auto;">
            <label style="text-align:right">stopTime:</label>
          </div>
        </div>
        {playerRecordsRx}
      </div>
    </div>
  }
}