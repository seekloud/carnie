package com.neo.sk.carnie.scene

import com.neo.sk.carnie.paperClient.BorderSize
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color

/**
  * Created by dry on 2018/10/29.
  **/
class BackgroundCanvas(canvas: Canvas) {

  private val ctx = canvas.getGraphicsContext2D
  private val canvasUnit = 20

  def drawCache(): Unit = { //离屏缓存的更新--缓存边界
    ctx.setFill(Color.rgb(105,105,105))

    //画边界
    ctx.fillRect(0, 0, canvasUnit * BorderSize.w, canvasUnit)
    ctx.fillRect(0, 0, canvasUnit, canvasUnit * BorderSize.h)
    ctx.fillRect(0, BorderSize.h * canvasUnit, canvasUnit * (BorderSize.w + 1), canvasUnit)
    ctx.fillRect(BorderSize.w * canvasUnit, 0, canvasUnit, canvasUnit * (BorderSize.h + 1))
  }

}