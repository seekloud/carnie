package com.neo.sk.carnie.scene

import java.awt.Graphics
import java.io.File

import com.neo.sk.carnie.paperClient._
import com.neo.sk.carnie.paperClient.Protocol.{Data4TotalSync, FieldByColumn, WinData}
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.{Font, FontPosture, FontWeight, Text}
import com.neo.sk.carnie.common.Constant
import com.neo.sk.carnie.common.Constant.ColorsSetting
import javafx.scene.SnapshotParameters
import javafx.scene.media.{AudioClip, AudioEqualizer, Media, MediaPlayer}
import org.slf4j.LoggerFactory

/**
  * Created by dry on 2018/10/29.
  **/
class GameViewCanvas(canvas: Canvas,rankCanvas: Canvas, img: Int) {//,background: BackgroundCanvas
  private val window = Point(Window.w, Window.h)
  private val border = Point(BorderSize.w, BorderSize.h)
  private var windowBoundary = Point(canvas.getWidth.toFloat, canvas.getHeight.toFloat)
  private val ctx = canvas.getGraphicsContext2D
  private val rankCtx = rankCanvas.getGraphicsContext2D
  private val canvasSize = (border.x - 2) * (border.y - 2)
  private val imgMap: Map[Int, String] =
    Map(
      0 -> "img/luffy.png",
      1 -> "img/fatTiger.png",
      2 -> "img/Bob.png",
      3 -> "img/yang.png",
      4 -> "img/smile.png",
      5 -> "img/pig.png"
    )
  val ab = imgMap(img)
  private val championHeaderImg = new Image("champion.png")
  private val myHeaderImg = new Image(imgMap(img))
  private val crownImg = new Image("crown.png")
  private var canvasUnit = (windowBoundary.x / window.x).toInt
  private var canvasUnitY = (windowBoundary.y / window.y).toInt
  private var scale = 1.0
  private var maxArea: Int = 0
  private val smallMap = Point(littleMap.w, littleMap.h)
  private val textLineHeight = 15
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  def debug(str: String) = log.debug(s"$str")

  def resetScreen(viewWidth:Int, viewHeight:Int, rankWidth:Int, rankHeight:Int) = {
    canvas.setWidth(viewWidth)
    canvas.setHeight(viewHeight)
    rankCanvas.setWidth(rankWidth)
    rankCanvas.setHeight(rankHeight)
    windowBoundary = Point(canvas.getWidth.toFloat, canvas.getHeight.toFloat)
    canvasUnit = (windowBoundary.x / window.x).toInt
    canvasUnitY = (windowBoundary.y / window.y).toInt
  }

  def drawGameOff(firstCome: Boolean): Unit = {
    ctx.save()
    ctx.setFill(ColorsSetting.dieInfoBackgroundColor)
    ctx.fillRect(0, 0, windowBoundary.x, windowBoundary.y)
    ctx.setFill(ColorsSetting.dieInfoFontColor)
    if (firstCome) {
      ctx.setFont(Font.font(30))
      ctx.fillText("Welcome.", 150, 180)
    } else {
      ctx.setFont(Font.font(30))
      ctx.fillText("Ops, connection lost.", 150, 180)
    }
    ctx.restore()
  }

  def drawGameWin(myId: String, winner: String, data: Data4TotalSync,winningData:WinData): Unit = {
    val winnerId = data.snakes.find(_.name == winner).map(_.id).get
    val snakes = data.snakes
    val snakesFields = data.fieldDetails
    scale = 0.33
    val width = windowBoundary.x - BorderSize.w * canvasUnit * scale
    val height = windowBoundary.y - BorderSize.h * canvasUnit * scale
    ctx.clearRect(0, 0, windowBoundary.x, windowBoundary.y)
    ctx.save()
    ctx.scale(scale, scale)
    ctx.setFill(ColorsSetting.borderColor)
    ctx.fillRect(1.5 * width - canvasUnit, 1.5 * height - canvasUnit, canvasUnit * BorderSize.w, canvasUnit)
    ctx.fillRect(1.5 * width - canvasUnit, 1.5 * height - canvasUnit, canvasUnit, canvasUnit * BorderSize.h)
    ctx.fillRect(1.5 * width - canvasUnit, BorderSize.h * canvasUnit + 1.5 * height - canvasUnit, canvasUnit * (BorderSize.w + 1), canvasUnit)
    ctx.fillRect(BorderSize.w * canvasUnit + 1.5 * width - canvasUnit, 1.5 * height - canvasUnit, canvasUnit, canvasUnit * (BorderSize.h + 1))
    snakesFields.foreach { field =>
      if (field.uid == myId || field.uid == winnerId) {
        val color = snakes.find(_.id == field.uid).map(s => Constant.hex2Rgb(s.color)).get
        ctx.setFill(color)
        field.scanField.foreach { point =>
          point.x.foreach { x =>
            ctx.fillRect(x._1 * canvasUnit + 1.5 * width - canvasUnit, point.y * canvasUnit + 1.5 * height - canvasUnit, canvasUnit * (x._2 - x._1 + 1), canvasUnit * 1.05)
          }
        }
      }
    }
    ctx.restore()
    ctx.save()
    ctx.scale(1, 1)
    ctx.setGlobalAlpha(1)
    ctx.setFont(Font.font("Microsoft YaHei", FontPosture.findByName("bold"), 25))
    ctx.setFill(ColorsSetting.defaultColor)

    val txt1 = s"The Winner is $winner"
    val txt2 = s"Press space to reStart"
    val length = new Text(txt1).getLayoutBounds.getWidth
    ctx.fillText(txt1, (windowBoundary.x - length) / 2 , windowBoundary.y / 5)
    val x = (windowBoundary.x / 2).toInt - 145
    val y = (windowBoundary.y / 2).toInt - 180
    ctx.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20))
//    ctx.setfont = "bold 24px Helvetica"
    ctx.setFill(ColorsSetting.fontColor2)
    ctx.fillText("YOUR SCORE:", x, y + 70)
    ctx.fillText(f"${winningData.yourScore.get / canvasSize * 100}%.2f" + "%", x + 230, y + 70)
    if(winningData.yourScore.isDefined){
      ctx.fillText("BEST SCORE:", x, y + 110)
      ctx.fillText(f"${winningData.winnerScore / canvasSize * 100}%.2f" + "%", x + 230, y + 110)
    }
    ctx.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20)) //FontPosture.findByName("bold")
    ctx.fillText(txt2, windowBoundary.x - 300, windowBoundary.y - 100)
    ctx.drawImage(crownImg, (windowBoundary.x - length) / 2 + length - 50, windowBoundary.y / 5 - 75, 50, 50)
    ctx.restore()
  }
  import javafx.scene.text.Text
  def drawUserDieInfo(killedName: String, killerName: String): Unit = {
    ctx.save()
//    ctx.globalAlpha = 0.6

    ctx.restore()
    ctx.save()
    ctx.setFont(Font.font(30))
    ctx.setFill(ColorsSetting.gameNameColor)
    val txt = s"$killedName is killed by $killerName"
    val text = new Text(txt)
    text.setFont(Font.font(30))
    text.setFill(ColorsSetting.gameNameColor)
    val length = text.getLayoutBounds.getWidth
    val offx = length / 2
//    ctx.drawImage(bloodImg, windowBoundary.x / 2 - offx, 115, 300, 50)
    ctx.fillText(s"$killedName is killed by $killerName", windowBoundary.x / 2 - offx, 150)
    ctx.restore()
  }

  def drawGameDie(killerOpt: Option[String],  myScore :BaseScore, maxArea: Int): Unit = {
    //    rankCtx.clearRect(0, 0, windowBoundary.x, windowBoundary.y)
//    val endTime = System.currentTimeMillis()
//    if (myScore.area > maxArea) maxArea = myScore.area
    ctx.setFill(ColorsSetting.dieInfoBackgroundColor)
    ctx.fillRect(0, 0, windowBoundary.x, windowBoundary.y)
    ctx.setFill(ColorsSetting.gameNameColor)
    ctx.setFont(Font.font(20))
    scale = 1
    ctx.scale(1, 1)

    val text = killerOpt match {
      case Some(killer) => s"Ops, You Are Killed By $killer! Press Space Key To Revenge!"
      case None => "Ops, Press Space Key To Restart!"
    }
    val txt =new Text(text)
    val length = txt.getLayoutBounds.getWidth
    val offx = length / 2
    val x = (windowBoundary.x / 2).toInt - 145
    val y = (windowBoundary.y / 2).toInt - 180

    val gameTime = (myScore.endTime - myScore.startTime) / 1000
    val bestScore = maxArea / canvasSize * 100
    val time = {
      val tempM = gameTime / 60
      val s1 = gameTime % 60
      val s = if (s1 < 0) "00" else if (s1 < 10) "0" + s1 else s1.toString
      val m = if (tempM < 0) "00" else if (tempM < 10) "0" + tempM else tempM.toString
      m + ":" + s
    }
    ctx.fillText(text, windowBoundary.x / 2 - offx - 50 , y) //(500,180)
    ctx.save()
    ctx.setFill(ColorsSetting.dieInfoFontColor)
    ctx.setFont(Font.font(20))
    ctx.fillText("YOUR SCORE:", x, y + 70)
    ctx.fillText(f"${myScore.area / canvasSize * 100}%.2f" + "%", x + 230, y + 70)
    ctx.fillText("BEST SCORE:", x, y + 110)
    ctx.fillText(f"$bestScore%.2f" + "%", x + 230, y + 110)
    ctx.fillText(s"PLAYERS KILLED:", x, y + 150)
    ctx.fillText(s"${myScore.kill}", x + 230, y + 150)
    ctx.fillText(s"TIME PLAYED:", x, y + 190)
    ctx.fillText(s"$time", x + 230, y + 190)
    ctx.restore()
  }

  def drawDieBarrage(killedName: String, killerName: String): Unit = {
    ctx.save()
    ctx.setGlobalAlpha(0.6)
//    ctx.drawImage(bloodImg, 670, 115, 300, 50)
    ctx.restore()
    ctx.save()
    ctx.setFill(Color.rgb(255, 88, 9))
    ctx.setFont(Font.font(25))
    val length = 30
    val offx = (270 - length) / 2
    ctx.fillText(s"$killedName is killed by $killerName", 670 + offx, 150)
    ctx.restore()
  }

  def drawSmallMap(myHeader: Point, otherSnakes: List[SkDt]): Unit = {
    val offx = myHeader.x.toDouble / border.x * smallMap.x
    val offy = myHeader.y.toDouble / border.y * smallMap.y
    ctx.setFill(ColorsSetting.mapColor)
    val w = windowBoundary.x * 0.99 - littleMap.w * canvasUnit //* 1.100
    val h = windowBoundary.y - littleMap.h * canvasUnitY //* 1.170
//    val h = w * 7 / 12
    ctx.save()
    ctx.setGlobalAlpha(0.5)
    ctx.fillRect(w.toInt, h.toInt, littleMap.w * canvasUnit + 5, littleMap.h * canvasUnit + 5)
    ctx.restore()
    ctx.drawImage(myHeaderImg, (w + offx * canvasUnit).toInt, (h + offy * canvasUnit).toInt, 10, 10)
    otherSnakes.foreach { i =>
      val x = i.header.x.toDouble / border.x * smallMap.x
      val y = i.header.y.toDouble / border.y * smallMap.y
      ctx.setFill(Constant.hex2Rgb(i.color))
      ctx.fillRect(w + x * canvasUnit, h + y * canvasUnit, 10, 10)
    }
  }

//  var a = 0
//  var b = 0
//  var c = 0
//  var d = 0
//  var e = 0
//  var f = 0
  def drawCache(offx: Float, offy: Float): Unit = { //离屏缓存的更新--缓存边界
//    ctx.clearRect(0,0,canvas.getWidth,canvas.getHeight)
    ctx.setFill(Color.rgb(105,105,105))

    //画边界
    ctx.fillRect(canvasUnit * offx, canvasUnit * offy, canvasUnit * BorderSize.w, canvasUnit)
    ctx.fillRect(canvasUnit * offx, canvasUnit * offy, canvasUnit, canvasUnit * BorderSize.h)
    ctx.fillRect(canvasUnit * offx, (BorderSize.h + offy) * canvasUnit, canvasUnit * (BorderSize.w + 1), canvasUnit)
    ctx.fillRect((BorderSize.w + offx) * canvasUnit, canvasUnit * offy, canvasUnit, canvasUnit * (BorderSize.h + 1))
  }

  def drawGrid(uid: String, data: Data4TotalSync, offsetTime: Long, grid: Grid, championId: String, frameRate: Int): Unit = { //头所在的点是屏幕的正中心
    val snakes = data.snakes

    val lastHeader = snakes.find(_.id == uid) match {
      case Some(s) =>
        val nextDirection = grid.nextDirection(s.id).getOrElse(s.direction)
        val direction = if (s.direction + nextDirection != Point(0, 0)) nextDirection else s.direction
        s.header + direction * offsetTime.toFloat / frameRate

      case None =>
        Point(border.x / 2, border.y / 2)
    }

    val offx = window.x / 2 - lastHeader.x //新的框的x偏移量
    val offy = window.y / 2 - lastHeader.y //新的框的y偏移量

    val newWindowBorder = Point(window.x / scale.toFloat, window.y / scale.toFloat)
    val (minPoint, maxPoint) = (lastHeader - newWindowBorder, lastHeader + newWindowBorder)

    ctx.clearRect(0, 0, windowBoundary.x, windowBoundary.y)
    ctx.setFill(ColorsSetting.backgroundColor)
    ctx.fillRect(0,0,windowBoundary.x,windowBoundary.y)
    val snakeWithOff = data.snakes.map(i => i.copy(header = Point(i.header.x + offx, y = i.header.y + offy)))
    val fieldInWindow = data.fieldDetails.map { f => FieldByColumn(f.uid, f.scanField.filter(p => p.y < maxPoint.y && p.y > minPoint.y)) }

    scale = 1 - grid.getMyFieldCount(uid, maxPoint, minPoint) * 0.00008
    ctx.save()

    setScale(scale, windowBoundary.x / 2, windowBoundary.y / 2)
    drawCache(offx , offy)
    ctx.setGlobalAlpha(0.6)
    data.bodyDetails.foreach { bds =>
      val color = snakes.find(_.id == bds.uid).map(s => Constant.hex2Rgb(s.color)).getOrElse(ColorsSetting.defaultColor)
      ctx.setFill(color)
      val turnPoints = bds.turn.turnPoint
      (0 until turnPoints.length - 1).foreach { i => //拐点渲染
        val start = turnPoints(i)
        val end = turnPoints(i + 1)
//        f = f + 1
//        if(f % 200 == 0 || f == 0) {
//          debug(s"length:${turnPoints.length} turnPoints:$turnPoints ")
//        }
        if (start.x == end.x) { //同x
          if (start.y > end.y) {
//            a = a + 1
////            println(a)
//            if(a % 200 == 0) {
//              debug("111")
//            }
            ctx.fillRect((start.x + offx) * canvasUnit, (end.y + 1 + offy) * canvasUnit, canvasUnit, (start.y - end.y) * canvasUnit)
          } else {
//            b = b + 1
//            if(b % 200 == 0) {
//              debug("222")
//            }
            ctx.fillRect((start.x + offx) * canvasUnit, (start.y + offy) * canvasUnit, canvasUnit, (end.y - start.y) * canvasUnit)
          }
        } else { // 同y

          if (start.x > end.x) {
//            c=c+1
//            if(c % 200 == 0) {
//              debug("333")
//            }
            ctx.fillRect((end.x + 1 + offx) * canvasUnit, (end.y + offy) * canvasUnit, (start.x - end.x) * canvasUnit, canvasUnit)
          } else {
//            d = d + 1
//            if(d % 200 == 0) {
//              debug("444")
//            }
            ctx.fillRect((start.x + offx) * canvasUnit, (start.y + offy) * canvasUnit, (end.x - start.x) * canvasUnit, canvasUnit)
          }
        }
      }
      if (turnPoints.nonEmpty) {
//        e = e + 1
//        if(e % 200 == 0) {
//          debug("555")
//        }
        ctx.fillRect((turnPoints.last.x + offx) * canvasUnit, (turnPoints.last.y + offy) * canvasUnit, canvasUnit, canvasUnit)
      }
    }

    ctx.setGlobalAlpha(1)
    fieldInWindow.foreach { field => //按行渲染
      val color = snakes.find(_.id == field.uid).map(s => Constant.hex2Rgb(s.color)).getOrElse(ColorsSetting.defaultColor)
      ctx.setFill(color)
      field.scanField.foreach { point =>
        point.x.foreach { x =>
          ctx.fillRect((x._1 + offx) * canvasUnit, (point.y + offy) * canvasUnit, canvasUnit * (x._2 - x._1 + 1), canvasUnit * 1.05)
        }
      }
    }

    snakeWithOff.foreach { s =>
      ctx.setFill(Constant.hex2Rgb(s.color))

      val nextDirection = grid.nextDirection(s.id).getOrElse(s.direction)
      val direction = if (s.direction + nextDirection != Point(0, 0)) nextDirection else s.direction
      val off = direction * offsetTime.toFloat / frameRate
      ctx.fillRect((s.header.x + off.x) * canvasUnit, (s.header.y + off.y) * canvasUnit, canvasUnit, canvasUnit)

      if (s.id == championId)
        ctx.drawImage(championHeaderImg, (s.header.x + off.x) * canvasUnit, (s.header.y + off.y - 1) * canvasUnit, canvasUnit, canvasUnit)
      val otherHeaderImg = new Image(imgMap(s.img))
      val img = if (s.id == uid) myHeaderImg else otherHeaderImg
      ctx.drawImage(img, (s.header.x + off.x) * canvasUnit, (s.header.y + off.y) * canvasUnit, canvasUnit, canvasUnit)

      ctx.setFont(Font.font(16))
      ctx.setFill(Color.rgb(0, 0, 0))
      val t = new Text(s"${s.name}")
      ctx.fillText(s.name, (s.header.x + off.x) * canvasUnit + canvasUnit / 2 - t.getLayoutBounds.getWidth / 2, (s.header.y + off.y - 1) * canvasUnit - 3)
    }

    ctx.restore()

    rankCtx.clearRect(20, textLineHeight * 5, 650, textLineHeight * 2)//* 5, * 2
    PerformanceTool.renderFps(rankCtx, 20, 5 * textLineHeight)
  }

  def setScale(scale: Double, x: Double, y: Double): Unit = {
    ctx.translate(x, y)
    ctx.scale(scale, scale)
    ctx.translate(-x, -y)
  }




}
