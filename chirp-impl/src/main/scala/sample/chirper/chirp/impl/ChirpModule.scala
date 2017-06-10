/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.chirp.impl

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import sample.chirper.chirp.api.ChirpService

abstract class ChirpModule(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with CassandraPersistenceComponents
    with PubSubComponents
{

  override lazy val lagomServer = serverFor[ChirpService](wire[ChirpServiceImpl])
}
