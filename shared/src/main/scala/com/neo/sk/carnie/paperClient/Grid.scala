package com.neo.sk.carnie.paperClient

import java.awt.event.KeyEvent

import com.neo.sk.carnie.paperClient.Protocol._

import scala.util.Random
import scala.collection.mutable

/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 5:34 PM
  */
trait Grid {

  val boundary: Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  val random = new Random(System.nanoTime())

  val maxDelayed = 6 //最大接收5帧以内的延时
  val historyRankLength = 5
  var frameCount = 0l
  var grid: Map[Point, Spot] = Map[Point, Spot]()
  var snakes = Map.empty[String, SkDt]
  var actionMap = Map.empty[Long, Map[String, Int]] //Map[frameCount,Map[id, keyCode]]
  var killHistory = Map.empty[String, (String, String, Long)] //killedId, (killerId, killerName,frameCount)
  var snakeTurnPoints = new mutable.HashMap[String, List[Point4Trans]] //保留拐点
  var mayBeDieSnake = Map.empty[String, String] //可能死亡的蛇 killedId,killerId
  var mayBeSuccess = Map.empty[String, Map[Point, Spot]] //圈地成功后的被圈点 userId,points
  var historyStateMap = Map.empty[Long, (Map[String, SkDt], Map[Point, Spot])] //保留近期的状态以方便回溯 (frame, (snake, pointd))

  List(0, BorderSize.w).foreach(x => (0 until BorderSize.h).foreach(y => grid += Point(x, y) -> Border))
  List(0, BorderSize.h).foreach(y => (0 until BorderSize.w).foreach(x => grid += Point(x, y) -> Border))

//  def checkEvents(enclosure: List[(String, List[Point])]): Unit

  def removeSnake(id: String): Option[SkDt] = {
    val r = snakes.get(id)
    if (r.isDefined) {
      snakes -= id
    }
    r
  }

  def addAction(id: String, keyCode: Int): Unit = {
    addActionWithFrame(id, keyCode, frameCount)
  }

  def addActionWithFrame(id: String, keyCode: Int, frame: Long): Unit = {
    val map = actionMap.getOrElse(frame, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (frame -> tmp)
  }

  def deleteActionWithFrame(id: String, frame: Long): Unit = {
    val map = actionMap.getOrElse(frame, Map.empty)
    val tmp = map - id
    actionMap += (frame -> tmp)
  }

  def nextDirection(id: String): Option[Point] = {
    val map = actionMap.getOrElse(frameCount, Map.empty)
    map.get(id) match {
      case Some(KeyEvent.VK_LEFT) => Some(Point(-1, 0))
      case Some(KeyEvent.VK_RIGHT) => Some(Point(1, 0))
      case Some(KeyEvent.VK_UP) => Some(Point(0, -1))
      case Some(KeyEvent.VK_DOWN) => Some(Point(0, 1))
      case _ => None
    }
  }

  def update(origin: String): List[(String, List[Point])] = {
    val isFinish = updateSnakes(origin)
    updateSpots()
    actionMap -= (frameCount - maxDelayed)
    historyStateMap = historyStateMap.filter(_._1 > (frameCount - (maxDelayed + 1)))
    //testEvents
//    checkEvents(isFinish)
    frameCount += 1
    isFinish
  }

  def updateSpots(): Unit = {
    grid = grid.filter { case (p, spot) =>
      spot match {
        case Body(id, _) if snakes.contains(id) => true
        case Field(id) if snakes.contains(id) => true
        case Border => true
        case _ => false
      }
    }
  }

  def randomEmptyPoint(size: Int): Point = {
    var p = Point(random.nextInt(boundary.x.toInt - size), random.nextInt(boundary.y.toInt - size))
    while ((0 until size * 2).flatMap { x =>
      (0 until size * 2).map { y =>
        grid.contains(p.copy(x = p.x + x, y = p.y + y))
      }
    }.contains(true)) {
      p = Point(random.nextInt(boundary.x.toInt - size), random.nextInt(boundary.y.toInt - size))
    }
    p + Point(2 + random.nextInt(2), 2 + random.nextInt(2))
  }

  def updateSnakes(origin: String): List[(String, List[Point])] = {
    var finishFields = List.empty[(String, List[Point])]

    def updateASnake(snake: SkDt, actMap: Map[String, Int]): Either[String, UpdateSnakeInfo] = {
      val keyCode = actMap.get(snake.id)
      val newDirection = {
        val keyDirection = keyCode match {
          case Some(KeyEvent.VK_LEFT) => Point(-1, 0)
          case Some(KeyEvent.VK_RIGHT) => Point(1, 0)
          case Some(KeyEvent.VK_UP) => Point(0, -1)
          case Some(KeyEvent.VK_DOWN) => Point(0, 1)
          case _ => snake.direction
        }
        if (keyDirection + snake.direction != Point(0, 0)) {
          keyDirection
        } else {
          snake.direction
        }
      }

      if (newDirection != Point(0, 0)) {
        val newHeader = snake.header + newDirection

        grid.get(newHeader) match {
          case Some(x: Body) => //进行碰撞检测
            debug(s"snake[${snake.id}] hit wall.")
            if (x.id != snake.id) { //撞到了别人的身体
              killHistory += x.id -> (snake.id, snake.name, frameCount)
            }
            mayBeDieSnake += x.id -> snake.id
            grid.get(snake.header) match { //当上一点是领地时 记录出行的起点
              case Some(Field(fid)) if fid == snake.id =>
                snakeTurnPoints += ((snake.id, snakeTurnPoints.getOrElse(snake.id, Nil) ::: List(Point4Trans(newHeader.x.toInt, newHeader.y.toInt))))
                Right(UpdateSnakeInfo(snake.copy(header = newHeader, direction = newDirection, startPoint = snake.header), x.fid))

              case Some(Body(bid, _)) if bid == snake.id && x.fid.getOrElse(-1L) == snake.id =>
                enclosure(snake, origin, newHeader, newDirection)

              case _ =>
                if (snake.direction != newDirection)
                  snakeTurnPoints += ((snake.id, snakeTurnPoints.getOrElse(snake.id, Nil) ::: List(Point4Trans(snake.header.x.toInt, snake.header.y.toInt))))
                Right(UpdateSnakeInfo(snake.copy(header = newHeader, direction = newDirection), x.fid))
            }

          case Some(Field(id)) =>
            if (id == snake.id) {
              grid(snake.header) match {
                case Body(bid, _) if bid == snake.id => //回到了自己的领域
                  enclosure(snake, origin, newHeader, newDirection)

                case _ =>
                  Right(UpdateSnakeInfo(snake.copy(header = newHeader, direction = newDirection), Some(id)))
              }
            } else { //进入到别人的领域
              grid.get(snake.header) match { //当上一点是领地时 记录出行的起点
                case Some(Field(fid)) if fid == snake.id =>
                  snakeTurnPoints += ((snake.id, snakeTurnPoints.getOrElse(snake.id, Nil) ::: List(Point4Trans(newHeader.x.toInt, newHeader.y.toInt))))
                  Right(UpdateSnakeInfo(snake.copy(header = newHeader, direction = newDirection, startPoint = snake.header), Some(id)))
                case _ =>
                  if (snake.direction != newDirection)
                    snakeTurnPoints += ((snake.id, snakeTurnPoints.getOrElse(snake.id, Nil) ::: List(Point4Trans(snake.header.x.toInt, snake.header.y.toInt))))
                  Right(UpdateSnakeInfo(snake.copy(header = newHeader, direction = newDirection), Some(id)))
              }
            }

          case Some(Border) =>
            Left(snake.id)

          case _ =>
            grid.get(snake.header) match { //当上一点是领地时 记录出行的起点
              case Some(Field(fid)) if fid == snake.id =>
                snakeTurnPoints += ((snake.id, snakeTurnPoints.getOrElse(snake.id, Nil) ::: List(Point4Trans(newHeader.x.toInt, newHeader.y.toInt))))
                Right(UpdateSnakeInfo(snake.copy(header = newHeader, direction = newDirection, startPoint = snake.header)))

              case _ =>
                if (snake.direction != newDirection)
                  snakeTurnPoints += ((snake.id, snakeTurnPoints.getOrElse(snake.id, Nil) ::: List(Point4Trans(snake.header.x.toInt, snake.header.y.toInt))))
                Right(UpdateSnakeInfo(snake.copy(header = newHeader, direction = newDirection)))
            }
        }
      }
      else Right(UpdateSnakeInfo(snake, Some(snake.id)))

    }

    var mapKillCounter = Map.empty[String, Int]
    var updatedSnakes = List.empty[UpdateSnakeInfo]
    var killedSnaked = List.empty[String]

    historyStateMap += frameCount -> (snakes, grid)

    val acts = actionMap.getOrElse(frameCount, Map.empty[String, Int])

    snakes.values.map(updateASnake(_, acts)).foreach {
      case Right(s) =>
        updatedSnakes ::= s

      case Left(sid) =>
        killedSnaked ::= sid
    }

    val intersection = mayBeSuccess.keySet.filter(p => mayBeDieSnake.keys.exists(_ == p))
    if (intersection.nonEmpty) {
      intersection.foreach { snakeId => // 在即将完成圈地的时候身体被撞击则不死但此次圈地作废
        mayBeSuccess(snakeId).foreach { i =>
          i._2 match {
            case Body(_, fid) if fid.nonEmpty => grid += i._1 -> Field(fid.get)
            case Field(fid) => grid += i._1 -> Field(fid)
            case _ => grid -= i._1
          }
        }
        mayBeDieSnake -= snakeId
        killHistory -= snakeId
      }
    }

    //if two (or more) headers go to the same point
    val snakesInDanger = updatedSnakes.groupBy(_.data.header).filter(_._2.lengthCompare(1) > 0).flatMap { res =>
      val sids = res._2.map(_.data.id)
      grid.get(res._1) match {
        case Some(Field(fid)) if sids.contains(fid) =>
          sids.filterNot(_ == fid).foreach { killedId =>
            mayBeDieSnake += killedId -> fid
            killHistory += killedId -> (killedId, snakes.find(_._1 == fid).get._2.name, frameCount)
          }
          sids.filterNot(_ == fid)
        case _ => sids
      }
    }.toList

    mayBeDieSnake.foreach { s =>
      mapKillCounter += s._2 -> (mapKillCounter.getOrElse(s._2, 0) + 1)
      killedSnaked ::= s._1
    }

    finishFields = mayBeSuccess.map(i => (i._1, i._2.keys.toList)).toList

    val noHeaderSnake = snakes.filter(s => finishFields.flatMap(_._2).contains(updatedSnakes.find(_.data.id == s._2.id).getOrElse(UpdateSnakeInfo(SkDt((-1).toString, "", "", Point(0, 0), Point(-1, -1)))).data.header)).keySet

    mayBeDieSnake = Map.empty[String, String]
    mayBeSuccess = Map.empty[String, Map[Point, Spot]]

    val noFieldSnake = snakes.keySet &~ grid.map(_._2 match { case x@Field(uid) => uid case _ => 0.toString }).toSet.filter(_ != 0.toString) //若领地全被其它玩家圈走则死亡

    val finalDie = snakesInDanger ::: killedSnaked ::: noFieldSnake.toList ::: noHeaderSnake.toList

    //    println(s"snakeInDanger:$snakesInDanger\nkilledSnaked:$killedSnaked\nnoFieldSnake:$noFieldSnake\nnoHeaderSnake:$noHeaderSnake")

    finalDie.foreach { sid =>
      returnBackField(sid)
      grid ++= grid.filter(_._2 match { case Body(_, fid) if fid.nonEmpty && fid.get == sid => true case _ => false }).map { g =>
        Point(g._1.x, g._1.y) -> Body(g._2.asInstanceOf[Body].id, None)
      }
      snakeTurnPoints -= sid
    }

    val newSnakes = updatedSnakes.filterNot(s => finalDie.contains(s.data.id)).map { s =>
      mapKillCounter.get(s.data.id) match {
        case Some(k) => s.copy(data = s.data.copy(kill = k + s.data.kill))
        case None => s
      }
    }

    newSnakes.foreach { s =>
      if (s.bodyInField.nonEmpty && s.bodyInField.get == s.data.id) grid += s.data.header -> Field(s.data.id)
      else grid += s.data.header -> Body(s.data.id, s.bodyInField)
    }

    snakes = newSnakes.map(s => (s.data.id, s.data)).toMap

    finishFields
  }

  def enclosure(snake: SkDt, origin: String, newHeader: Point, newDirection: Point) = {
    snakeTurnPoints -= snake.id

    if (mayBeDieSnake.keys.exists(_ == snake.id)) { //如果在即将完成圈地的时候身体被撞击则不死但此次圈地作废
      killHistory -= snake.id
      mayBeDieSnake -= snake.id
      returnBackField(snake.id)
    } else {
      val stillStart = if (grid.get(snake.startPoint) match {
        case Some(Field(fid)) if fid == snake.id => true
        case _ => false
      }) true else false //起点是否被圈走
      if (stillStart && origin == "b") { //只在后台执行圈地算法
        var finalFillPoll = grid.filter(_._2 match { case Body(bodyId, _) if bodyId == snake.id => true case _ => false })

        grid ++= finalFillPoll.keys.map(p => p -> Field(snake.id))

        val myFieldPoint = grid.filter(_._2 match { case Field(fid) if fid == snake.id => true case _ => false }).keys

        val (xMin, xMax, yMin, yMax) = Short.findMyRectangle(myFieldPoint)

        var targets = Set.empty[Point] //所有需要检查的坐标值的集

        for (x <- xMin until xMax) {
          for (y <- yMin until yMax) {
            grid.get(Point(x, y)) match {
              case Some(x: Field) if x.id == snake.id => //donothing
              case _ => targets = targets + Point(x, y)
            }
          }
        }

        while (targets.nonEmpty) {
          var iter = List.empty[Point]
          iter = iter :+ targets.head
          targets = targets.tail

          var fillPool = List.empty[Point] //该次填色需要上色的所有坐标
          var in_bound = true //这次上色是否为内部区域
          while (iter.nonEmpty) {
            val curr = iter.head
            iter = iter.tail
            Array(Point(-1, 0), Point(0, -1), Point(0, 1), Point(1, 0)).foreach { dir =>
              if (targets.contains(dir + curr)) { //如果 targets 包含该坐标，则将该坐标从targets中移除并添加至iter
                targets = targets - (dir + curr)
                iter = iter :+ (dir + curr)
              }
            }
            if (in_bound) {
              //如果curr紧邻field_border(boundary)，将in_bound设为False；否则向fill_pool中加入curr
              val aroundPoints = List(Point(-1, 0), Point(1, 0), Point(0, -1), Point(0, 1)).map(p => p + curr)
              if (aroundPoints.head.x <= xMin || aroundPoints(1).x >= xMax || aroundPoints(2).y <= yMin || aroundPoints(3).y >= yMax) {
                in_bound = false
              } else {
                fillPool ::= curr
              }
            }
          }
          if (in_bound) { //如果 in_bound 为真则将 fill_pool中所有坐标填充为当前玩家id
            var newGrid = grid
            for (p <- fillPool) {
              grid.get(p) match {
                case Some(Body(bodyId, _)) => newGrid += p -> Body(bodyId, Some(snake.id))
                case Some(Border) => //doNothing
                case x => newGrid += p -> Field(snake.id)
                  x match {
                    case Some(Field(fid)) => finalFillPoll += p -> Field(fid)
                    case _ => finalFillPoll += p -> Blank
                  }
              }
            }
            grid = newGrid
          }
        }
        mayBeSuccess += (snake.id -> finalFillPoll)

      } else returnBackField(snake.id)
    }
    Right(UpdateSnakeInfo(snake.copy(header = newHeader, direction = newDirection), Some(snake.id)))
  }

  def getGridData: Protocol.Data4TotalSync = {
    var fields: List[Fd] = Nil

    val bodyDetails = snakes.values.map { s => BodyBaseInfo(s.id, getSnakesTurn(s.id, s.header)) }.toList

    grid.foreach {
      case (p, Field(id)) => fields ::= Fd(id, p.x.toInt, p.y.toInt)
      case _ => //doNothing
    }

    val fieldDetails = fields.groupBy(_.id).map { case (userId, fieldPoints) =>
      FieldByColumn(userId, fieldPoints.groupBy(_.y).map { case (y, target) =>
        ScanByColumn(y.toInt, Tool.findContinuous(target.map(_.x.toInt).toArray.sorted))
      }.toList)
    }.toList

    Protocol.Data4TotalSync(
      frameCount,
      snakes.values.toList,
      bodyDetails,
      fieldDetails,
      killHistory.map(k => Kill(k._1, k._2._1, k._2._2, k._2._3)).toList
    )
  }

  def getKiller(myId: String): Option[(String, String, Long)] = {
    killHistory.get(myId)
  }

  def cleanData(): Unit = {
    snakes = Map.empty[String, SkDt]
    actionMap = Map.empty[Long, Map[String, Int]]
    grid = grid.filter(_._2 match { case Border => true case _ => false })
    killHistory = Map.empty[String, (String, String, Long)]
    snakeTurnPoints = snakeTurnPoints.empty
  }

  def returnBackField(snakeId: String): Unit = { //归还身体部分所占有的领地
    snakeTurnPoints -= snakeId
    val bodyGrid = grid.filter(_._2 match { case Body(bid, _) if bid == snakeId => true case _ => false })
    var newGrid = grid
    bodyGrid.foreach {
      case (p, Body(_, fid)) if fid.nonEmpty => newGrid += p -> Field(fid.get)
      case (p, _) => newGrid -= p
    }
    grid = newGrid
  }

  def recallGrid(startFrame: Long, endFrame: Long): Unit = {
    historyStateMap.get(startFrame) match {
      case Some(state) =>
        println(s"recallGrid-start$startFrame-end-$endFrame")
        snakes = state._1
        grid = state._2
        (startFrame to endFrame).foreach { frame =>
          frameCount = frame
          updateSnakes("b")
        }

      case None =>
        println(s"???can't find-$startFrame-end is $endFrame!!!!tartget-${historyStateMap.keySet}")
    }
  }

  def getSnakesTurn(sid: String, header: Point): TurnInfo = {
    val turnPoint = snakeTurnPoints.getOrElse(sid, Nil)
    TurnInfo(if (turnPoint.nonEmpty) turnPoint ::: List(Point4Trans(header.x.toInt, header.y.toInt)) else turnPoint,
      grid.filter(_._2 match { case Body(id, fid) if id == sid && fid.nonEmpty => true case _ => false }).map(g =>
        (Point4Trans(g._1.x.toInt, g._1.y.toInt), g._2.asInstanceOf[Body].fid.get)).toList)
  }

  def getMyFieldCount(uid: String, maxPoint: Point, minPoint: Point): Int = {
    grid.count { g =>
      (g._2 match {
        case Field(fid) if fid == uid => true
        case _ => false
      }) &&
        (g._1.x < maxPoint.x && g._1.y < maxPoint.y && g._1.y > minPoint.y && g._1.x > minPoint.x)
    }
  }

  def cleanTurnPoint4Reply(sid: String) = {
    if(snakeTurnPoints.contains(sid)) {
//      val turnPoints = snakeTurnPoints(sid).filterNot(t => t.)
        snakeTurnPoints -= sid
    }

  }


}

