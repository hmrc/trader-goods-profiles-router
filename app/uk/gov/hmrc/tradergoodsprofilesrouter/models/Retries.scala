/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.tradergoodsprofilesrouter.models

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import org.slf4j.LoggerFactory
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpErrorResponse

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

trait Retries {

  protected def actorSystem: ActorSystem

  protected def configuration: Config

  private val logger = LoggerFactory.getLogger("application")

  def retryFor[A](
    label: String
  )(condition: PartialFunction[EisHttpErrorResponse, Boolean])(block: => Future[Either[EisHttpErrorResponse, A]])(
    implicit ec: ExecutionContext
  ): Future[Either[EisHttpErrorResponse, A]] = {

    def loop(remainingIntervals: Seq[FiniteDuration]): Future[Either[EisHttpErrorResponse, A]] =
      block.flatMap {
        case Left(error) if condition.lift(error).getOrElse(false) && remainingIntervals.nonEmpty =>
          val delay   = remainingIntervals.head
          logger.warn(s"Retrying $label in $delay due to error: ${error.errorResponse}")
          val mdcData = Mdc.mdcData
          after(delay, actorSystem.scheduler) {
            Mdc.putMdc(mdcData)
            loop(remainingIntervals.tail)
          }
        case result                                                                               => Future.successful(result)
      }

    loop(intervals)
  }

  private lazy val intervals: Seq[FiniteDuration] =
    configuration.getDurationList("http-verbs.retries.intervals").asScala.toSeq.map { d =>
      FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS)
    }
}
