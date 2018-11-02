package com.neo.sk.carnie.paperClient

import com.neo.sk.carnie.common.Constant.ColorsSetting
import com.neo.sk.carnie.paperClient.Protocol.ReceivePingPacket
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text, TextAlignment}

/**
  * Created by dry on 2018/9/3.
  **/
object PerformanceTool {
  //FPS
  private var lastRenderTime = System.currentTimeMillis()
  private var lastRenderTimes = 0
  private var renderTimes = 0
  private var tempTime = System.currentTimeMillis()

  private def addFps() = {
    val time = System.currentTimeMillis()
    renderTimes += 1
//    println(s"addFps time:${time - tempTime}")
    tempTime = time
    if (time - lastRenderTime > 1000) {
      lastRenderTime = time
      lastRenderTimes = renderTimes
      renderTimes = 0
    }
  }

  def renderFps(ctx: GraphicsContext, leftBegin: Int, lineHeight: Int) = {
    addFps()
    ctx.setTextAlign(TextAlignment.LEFT)

    ctx.setFont(Font.font(20))
    ctx.setFill(Color.rgb(0,0,0))
//    ctx.font = "20px Helvetica"
//    ctx.fillStyle = ColorsSetting.fontColor2
    val fpsString = "fps : "
    val txt1 = new Text(fpsString)
    val len1 = txt1.getLayoutBounds.getWidth.toInt
    val pingString = "ping: "
    val txt2 = new Text(pingString)
    val len2 = txt2.getBoundsInLocal.getWidth.toInt
    ctx.fillText(fpsString, leftBegin, lineHeight)
    ctx.fillText(pingString, leftBegin + len1 + 60, lineHeight)
    ctx.setStroke(Color.BLACK)
    ctx.strokeText(lastRenderTimes.toString, leftBegin + len1 + 20, lineHeight)
    if (lastRenderTimes < 50)
      ctx.setFill(Color.RED)
    else
      ctx.setFill(Color.GREEN)
    ctx.fillText(lastRenderTimes.toString, leftBegin + len1 + 20, lineHeight)
    ctx.setStroke(Color.BLACK)
    ctx.strokeText(s"${latency}ms", leftBegin + len1 + len2 + 80, lineHeight)
    if (latency <= 100)
      ctx.setFill(Color.GREEN)
    else if (latency > 100 && latency <= 200)
      ctx.setFill(Color.YELLOW)
    else
      ctx.setFill(Color.RED)
    ctx.fillText(s"${latency}ms", leftBegin + len1 + len2 + 80, lineHeight)

  }

  //PING
  private var receiveNetworkLatencyList: List[Long] = Nil
  private val PingTimes = 10
  private var latency: Long = 0L

  def receivePingPackage(p: ReceivePingPacket): Unit = {
    receiveNetworkLatencyList = (System.currentTimeMillis() - p.createTime) :: receiveNetworkLatencyList
    if (receiveNetworkLatencyList.size >= PingTimes) {
      latency = receiveNetworkLatencyList.sum / receiveNetworkLatencyList.size
      receiveNetworkLatencyList = Nil
    }
  }

}
