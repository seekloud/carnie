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

package org.seekloud.carnie.models

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.PostgresProfile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tGameRecord.schema ++ tPlayerRecord.schema ++ tUserInRecord.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tGameRecord
    *  @param recordId Database column record_id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param roomId Database column room_id SqlType(int4)
    *  @param startTime Database column start_time SqlType(int8)
    *  @param endTime Database column end_time SqlType(int8)
    *  @param filePath Database column file_path SqlType(varchar), Length(255,true) */
  case class rGameRecord(recordId: Long, roomId: Int, startTime: Long, endTime: Long, filePath: String)
  /** GetResult implicit for fetching rGameRecord objects using plain SQL queries */
  implicit def GetResultrGameRecord(implicit e0: GR[Long], e1: GR[Int], e2: GR[String]): GR[rGameRecord] = GR{
    prs => import prs._
      rGameRecord.tupled((<<[Long], <<[Int], <<[Long], <<[Long], <<[String]))
  }
  /** Table description of table game_record. Objects of this class serve as prototypes for rows in queries. */
  class tGameRecord(_tableTag: Tag) extends profile.api.Table[rGameRecord](_tableTag, "game_record") {
    def * = (recordId, roomId, startTime, endTime, filePath) <> (rGameRecord.tupled, rGameRecord.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(recordId), Rep.Some(roomId), Rep.Some(startTime), Rep.Some(endTime), Rep.Some(filePath)).shaped.<>({r=>import r._; _1.map(_=> rGameRecord.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column record_id SqlType(bigserial), AutoInc, PrimaryKey */
    val recordId: Rep[Long] = column[Long]("record_id", O.AutoInc, O.PrimaryKey)
    /** Database column room_id SqlType(int4) */
    val roomId: Rep[Int] = column[Int]("room_id")
    /** Database column start_time SqlType(int8) */
    val startTime: Rep[Long] = column[Long]("start_time")
    /** Database column end_time SqlType(int8) */
    val endTime: Rep[Long] = column[Long]("end_time")
    /** Database column file_path SqlType(varchar), Length(255,true) */
    val filePath: Rep[String] = column[String]("file_path", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table tGameRecord */
  lazy val tGameRecord = new TableQuery(tag => new tGameRecord(tag))

  /** Entity class storing rows of table tPlayerRecord
    *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param playerId Database column player_id SqlType(varchar), Length(255,true)
    *  @param nickname Database column nickname SqlType(varchar), Length(255,true)
    *  @param killing Database column killing SqlType(int4)
    *  @param killed Database column killed SqlType(int4)
    *  @param score Database column score SqlType(float8)
    *  @param startTime Database column start_time SqlType(int8)
    *  @param endTime Database column end_time SqlType(int8) */
  case class rPlayerRecord(id: Long, playerId: String, nickname: String, killing: Int, killed: Int, score: Double, startTime: Long, endTime: Long)
  /** GetResult implicit for fetching rPlayerRecord objects using plain SQL queries */
  implicit def GetResultrPlayerRecord(implicit e0: GR[Long], e1: GR[String], e2: GR[Int], e3: GR[Double]): GR[rPlayerRecord] = GR{
    prs => import prs._
      rPlayerRecord.tupled((<<[Long], <<[String], <<[String], <<[Int], <<[Int], <<[Double], <<[Long], <<[Long]))
  }
  /** Table description of table player_record. Objects of this class serve as prototypes for rows in queries. */
  class tPlayerRecord(_tableTag: Tag) extends profile.api.Table[rPlayerRecord](_tableTag, "player_record") {
    def * = (id, playerId, nickname, killing, killed, score, startTime, endTime) <> (rPlayerRecord.tupled, rPlayerRecord.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(playerId), Rep.Some(nickname), Rep.Some(killing), Rep.Some(killed), Rep.Some(score), Rep.Some(startTime), Rep.Some(endTime)).shaped.<>({r=>import r._; _1.map(_=> rPlayerRecord.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column player_id SqlType(varchar), Length(255,true) */
    val playerId: Rep[String] = column[String]("player_id", O.Length(255,varying=true))
    /** Database column nickname SqlType(varchar), Length(255,true) */
    val nickname: Rep[String] = column[String]("nickname", O.Length(255,varying=true))
    /** Database column killing SqlType(int4) */
    val killing: Rep[Int] = column[Int]("killing")
    /** Database column killed SqlType(int4) */
    val killed: Rep[Int] = column[Int]("killed")
    /** Database column score SqlType(float8) */
    val score: Rep[Double] = column[Double]("score")
    /** Database column start_time SqlType(int8) */
    val startTime: Rep[Long] = column[Long]("start_time")
    /** Database column end_time SqlType(int8) */
    val endTime: Rep[Long] = column[Long]("end_time")
  }
  /** Collection-like TableQuery object for table tPlayerRecord */
  lazy val tPlayerRecord = new TableQuery(tag => new tPlayerRecord(tag))

  /** Entity class storing rows of table tUserInRecord
    *  @param userId Database column user_id SqlType(varchar), Length(255,true)
    *  @param recordId Database column record_id SqlType(int8)
    *  @param roomId Database column room_id SqlType(int4)
    *  @param nickname Database column nickname SqlType(varchar), Length(255,true), Default() */
  case class rUserInRecord(userId: String, recordId: Long, roomId: Int, nickname: String = "")
  /** GetResult implicit for fetching rUserInRecord objects using plain SQL queries */
  implicit def GetResultrUserInRecord(implicit e0: GR[String], e1: GR[Long], e2: GR[Int]): GR[rUserInRecord] = GR{
    prs => import prs._
      rUserInRecord.tupled((<<[String], <<[Long], <<[Int], <<[String]))
  }
  /** Table description of table user_in_record. Objects of this class serve as prototypes for rows in queries. */
  class tUserInRecord(_tableTag: Tag) extends profile.api.Table[rUserInRecord](_tableTag, "user_in_record") {
    def * = (userId, recordId, roomId, nickname) <> (rUserInRecord.tupled, rUserInRecord.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(userId), Rep.Some(recordId), Rep.Some(roomId), Rep.Some(nickname)).shaped.<>({r=>import r._; _1.map(_=> rUserInRecord.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column user_id SqlType(varchar), Length(255,true) */
    val userId: Rep[String] = column[String]("user_id", O.Length(255,varying=true))
    /** Database column record_id SqlType(int8) */
    val recordId: Rep[Long] = column[Long]("record_id")
    /** Database column room_id SqlType(int4) */
    val roomId: Rep[Int] = column[Int]("room_id")
    /** Database column nickname SqlType(varchar), Length(255,true), Default() */
    val nickname: Rep[String] = column[String]("nickname", O.Length(255,varying=true), O.Default(""))

    /** Index over (recordId) (database name user_in_record_record_id_idx) */
    val index1 = index("user_in_record_record_id_idx", recordId)
  }
  /** Collection-like TableQuery object for table tUserInRecord */
  lazy val tUserInRecord = new TableQuery(tag => new tUserInRecord(tag))
}
