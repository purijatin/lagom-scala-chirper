/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import sample.chirper.friend.api.User


case class FriendState(user: Option[User])  {
  def addFriend(friendUserId: String): FriendState = user match {
    case None => throw new IllegalStateException("friend can't be added before user is created")
    case Some(user) =>
      val newFriends = user.friends :+ friendUserId
      FriendState(Some(user.copy(friends = newFriends)))
  }
}

object FriendState {
  def apply(user: User): FriendState = FriendState(Option(user))
}