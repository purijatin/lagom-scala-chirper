/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import akka.Done
import akka.NotUsed
import javax.inject.Inject

import sample.chirper.friend.api.FriendId
import sample.chirper.friend.api.FriendService
import sample.chirper.friend.api.User

class FriendServiceImpl @Inject() (
    persistentEntities: PersistentEntityRegistry,
    readSide: CassandraReadSide,
    db: CassandraSession)(implicit ec: ExecutionContext) extends FriendService {

//  // Needed to convert some Scala types to Java
//  import ServiceCallConverter._
//
//  persistentEntities.register(classOf[FriendEntity])

//  readSide.register(classOf[FriendEventProcessor])

  override def getUser(id: String): ServiceCall[NotUsed, User] = {
    request =>
      friendEntityRef(id).ask(GetUser())
        .map(_.user.getOrElse(throw NotFound(s"user $id not found")))
  }

  override def createUser(): (User) => Future[Done] = {
    request =>
      friendEntityRef(request.userId).ask(CreateUser(request))
  }

  override def addFriend(userId: String): ServiceCall[FriendId, Done] = {
    request =>
      friendEntityRef(userId).ask(AddFriend(request.friendId))
  }

  override def getFollowers(id: String): ServiceCall[NotUsed, Seq[String]] = {
    req =>
      {
        db.selectAll("SELECT * FROM follower WHERE userId = ?", id).map { jrows =>
          val rows = jrows.toVector
          rows.map(_.getString("followedBy"))
        }
      }
  }

  private def friendEntityRef(userId: String) =
    persistentEntities.refFor[FriendEntity](userId)
}