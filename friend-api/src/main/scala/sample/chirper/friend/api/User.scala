/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.api

import play.api.libs.json.Json

import scala.collection.immutable.Seq

case class User (userId: String, name: String, friends: Seq[String]) {
  def this(userId: String, name: String) = this(userId, name, Seq.empty)
}

object User {
  implicit val userJson = Json.format[User]
}
