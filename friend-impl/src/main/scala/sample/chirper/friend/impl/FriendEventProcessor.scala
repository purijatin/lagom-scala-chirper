/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl


import java.util.UUID

import akka.Done
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}


class FriendEventProcessor(implicit ec: ExecutionContext, readSide: CassandraReadSide, session: CassandraSession) extends ReadSideProcessor[FriendEvent] {


  @volatile private var writeFollowers: PreparedStatement = _ // initialized in prepare
  @volatile private var writeOffset: PreparedStatement = _ // initialized in prepare

  private def setWriteFollowers(writeFollowers: PreparedStatement): Unit =
    this.writeFollowers = writeFollowers

  private def setWriteOffset(writeOffset: PreparedStatement): Unit =
    this.writeOffset = writeOffset

  override def aggregateTags = Set(FriendEvent.Tag)

  //  override def prepare(session: CassandraSession) = {
  //    // @formatter:off
  //    prepareCreateTables(session).thenCompose(a =>
  //    prepareWriteFollowers(session).thenCompose(b =>
  //    prepareWriteOffset(session).thenCompose(c =>
  //    selectOffset(session))))
  //    // @formatter:on
  //  }

  private def prepareCreateTables() = {
    // @formatter:off
    session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS follower ("
        + "userId text, followedBy text, "
        + "PRIMARY KEY (userId, followedBy))")
      .flatMap(a => session.executeCreateTable(
        "CREATE TABLE IF NOT EXISTS friend_offset ("
          + "partition int, offset timeuuid, "
          + "PRIMARY KEY (partition))"))
    // @formatter:on
  }

  private def prepareStatements() = {
    for {
      prepareWriteFollow <- session.prepare("INSERT INTO follower (userId, followedBy) VALUES (?, ?)")
      prepareWriteOffset <- session.prepare("INSERT INTO friend_offset (partition, offset) VALUES (1, ?)")
    } yield {
      setWriteFollowers(prepareWriteFollow)
      setWriteOffset(prepareWriteOffset)
      Done
    }
  }

  //  private def prepareWriteFollowers() = {
  //    val statement = session.prepare("INSERT INTO follower (userId, followedBy) VALUES (?, ?)")
  //    statement.map(ps => {
  //      setWriteFollowers(ps)
  //      Done
  //    })
  //  }
  //
  //  private def prepareWriteOffset() = {
  //    val statement = session.prepare("INSERT INTO friend_offset (partition, offset) VALUES (1, ?)")
  //    statement.map(ps => {
  //      setWriteOffset(ps)
  //      Done
  //    })
  //  }

  private def selectOffset() = {
    val select = session.selectOne("SELECT offset FROM friend_offset WHERE partition=1")
    select.map { maybeRow => maybeRow.map[UUID](_.getUUID("offset")) }
  }

  //  override def defineEventHandlers(builder: EventHandlersBuilder): EventHandlers = {
  //    builder.setEventHandler(classOf[FriendAdded], processFriendChanged)
  //    builder.build()
  //  }
  //

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[FriendEvent] = {

    //    prepareCreateTables(session).thenCompose(a =>
    //    prepareWriteFollowers(session).thenCompose(b =>
    //    prepareWriteOffset(session).thenCompose(c =>
    //    selectOffset(session))))

    val x = readSide.builder[FriendEvent]("friendEventOffset")
      .setGlobalPrepare(prepareCreateTables)
      .setPrepare(_ => prepareStatements())
      .setEventHandler[FriendAdded](e => {
        val event = e.event

        val bindWriteFollowers = writeFollowers.bind()
        bindWriteFollowers.setString("userId", event.friendId)
        bindWriteFollowers.setString("followedBy", event.userId)
        val bindWriteOffset = writeOffset.bind().bind(e.offset)
        Future(Seq(bindWriteFollowers, bindWriteOffset))
      }
    ).build

    x
  }
}
