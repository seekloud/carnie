package com.neo.sk.carnie.scene

import com.neo.sk.carnie.common.Constant
import com.neo.sk.carnie.common.Constant.ColorsSetting
import com.neo.sk.carnie.paperClient.{BorderSize, Point, Score, SkDt}
import javafx.geometry.VPos
import javafx.scene.paint.Color
import javafx.scene.text.{Font, FontPosture, FontWeight, Text}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.image.Image
/**
  * Created by dry on 2018/10/29.
  **/
class RankCanvas(canvas: Canvas)  {

  private val windowWidth = canvas.getWidth
  private val windowHeight = canvas.getHeight
  private val windowBoundary = Point(windowWidth.toFloat, windowHeight.toFloat)
  private val ctx = canvas.getGraphicsContext2D
  private val goldImg = new Image("/carnie/static/img/gold.png")
  private val silverImg = new Image("/carnie/static/img/silver.png")
  private val bronzeImg = new Image("/carnie/static/img/bronze.png")
  private val killImg = new Image("/carnie/static/img/kill.png")
  private val border = Point(BorderSize.w, BorderSize.h)
  private val canvasSize = (border.x - 2) * (border.y - 2)

  private val textLineHeight = 15
  private val fillWidth = 33
  private var lastRankNum = 0 //清屏用
  private val myRankBaseLine = 4

  def drawRank(uid: String, snakes: List[SkDt], currentRank: List[Score]): Unit = {

    val leftBegin = 20
    val rightBegin = windowBoundary.x - 230

    ctx.clearRect(0, textLineHeight, fillWidth + windowBoundary.x / 6, textLineHeight * 4) //绘制前清除canvas
    ctx.clearRect(rightBegin - 5 - textLineHeight, textLineHeight, 210 + 5 + textLineHeight, textLineHeight * (lastRankNum + 1) + 3)

    lastRankNum = currentRank.length

    ctx.setGlobalAlpha(1.0)
    ctx.setTextBaseline(VPos.TOP)

    val mySnake = snakes.filter(_.id == uid).head
    val baseLine = 2
    ctx.setFont(Font.font(22))
    ctx.setFill(Color.rgb(0,0,0))
    drawTextLine(s"KILL: ", leftBegin, 0, baseLine)
    ctx.drawImage(killImg, leftBegin + 55, textLineHeight, textLineHeight * 1.4, textLineHeight * 1.4)
    drawTextLine(s" x ${mySnake.kill}", leftBegin + 55 + (textLineHeight * 1.4).toInt, 0, baseLine)

    currentRank.filter(_.id == uid).foreach { score =>
//      myScore = myScore.copy(kill = score.k, area = score.area, endTime = System.currentTimeMillis())
//      if (myScore.area > maxArea)
//        maxArea = myScore.area
      val color = snakes.find(_.id == uid).map(_.color).getOrElse(ColorsSetting.defaultColor)
      ctx.setGlobalAlpha(0.6)
      val (r, g ,b) = Constant.hex2Rgb(color)
      ctx.setFill(Color.color(r, g, b))
      ctx.save()
      ctx.fillRect(leftBegin, (myRankBaseLine - 1) * textLineHeight, fillWidth + windowBoundary.x / 8 * (score.area.toDouble / canvasSize), textLineHeight + 10)
      ctx.restore()

      ctx.setGlobalAlpha(1)
      ctx.setFont(Font.font(22))
      ctx.setFill(Color.rgb(0,0,0))
      drawTextLine(f"${score.area.toDouble / canvasSize * 100}%.2f" + s"%", leftBegin, 0, myRankBaseLine)
    }

    val currentRankBaseLine = 2
    var index = 0
    ctx.setFont(Font.font(14))

    drawTextLine(s" --- Current Rank --- ", rightBegin.toInt, index, currentRankBaseLine)
    if (currentRank.lengthCompare(3) >= 0) {
      ctx.drawImage(goldImg, rightBegin - 5 - textLineHeight, textLineHeight * 2, textLineHeight, textLineHeight)
      ctx.drawImage(silverImg, rightBegin - 5 - textLineHeight, textLineHeight * 3, textLineHeight, textLineHeight)
      ctx.drawImage(bronzeImg, rightBegin - 5 - textLineHeight, textLineHeight * 4, textLineHeight, textLineHeight)
    }
    else if (currentRank.lengthCompare(2) == 0) {
      ctx.drawImage(goldImg, rightBegin - 5 - textLineHeight, textLineHeight * 2, textLineHeight, textLineHeight)
      ctx.drawImage(silverImg, rightBegin - 5 - textLineHeight, textLineHeight * 3, textLineHeight, textLineHeight)
    }
    else {
      ctx.drawImage(goldImg, rightBegin - 5 - textLineHeight, textLineHeight * 2, textLineHeight, textLineHeight)
    }
    currentRank.foreach { score =>
      val color = snakes.find(_.id == score.id).map(_.color).getOrElse(ColorsSetting.defaultColor)
      ctx.setGlobalAlpha(0.6)
      val (r, g ,b) = Constant.hex2Rgb(color)
      ctx.setFill(Color.color(r, g, b))
      ctx.save()
      ctx.fillRect(windowBoundary.x - 20 - fillWidth - windowBoundary.x / 8 * (score.area.toDouble / canvasSize), (index + currentRankBaseLine) * textLineHeight,
        fillWidth + windowBoundary.x / 8 * (score.area.toDouble / canvasSize), textLineHeight)
      ctx.restore()

      ctx.setGlobalAlpha(1)
      ctx.setFill(Color.rgb(0,0,0))
      index += 1
      drawTextLine(s"[$index]: ${score.n.+("   ").take(3)}", rightBegin.toInt, index, currentRankBaseLine)
      drawTextLine(s"area=" + f"${score.area.toDouble / canvasSize * 100}%.2f" + s"%", rightBegin.toInt + 70, index, currentRankBaseLine)
      drawTextLine(s"kill=${score.k}", rightBegin.toInt + 160, index, currentRankBaseLine)
    }
  }

  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0): Unit = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }


}
