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

package org.seekloud.carnie.models.dao

import org.seekloud.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import org.seekloud.carnie.models.SlickTables._
import org.seekloud.carnie.Boot.executor

object PlayerRecordDAO {

  def addPlayerRecord(playerId:String, nickName:String, killing:Int, killed:Int, score:Float, startTime:Long, endTime:Long) =
    db.run{
      tPlayerRecord += rPlayerRecord(-1l, playerId, nickName, killing, killed, score, startTime, endTime)
    }

  def getPlayerRecord()={
    db.run{
      tPlayerRecord.sortBy(_.startTime.desc).result
    }
  }
}
