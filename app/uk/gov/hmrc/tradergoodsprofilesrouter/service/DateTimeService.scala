/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofilesrouter.service

import com.google.inject.{ImplementedBy, Singleton}

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

@ImplementedBy(classOf[DateTimeServiceImpl])
trait DateTimeService {
  def timestamp: Instant
}

@Singleton
class DateTimeServiceImpl extends DateTimeService {
  override def timestamp: Instant = Instant.now()
}

object DateTimeService {
  implicit class DateTimeFormat(val dateTime: Instant) extends AnyVal {

    implicit def asStringHttp: String =
      ZonedDateTime
        .ofInstant(dateTime, ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'"))

    implicit def asStringSeconds: String =
      ZonedDateTime
        .ofInstant(dateTime, ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX"))

    implicit def asStringMilliSeconds: String =
      ZonedDateTime
        .ofInstant(dateTime, ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

  }
}
