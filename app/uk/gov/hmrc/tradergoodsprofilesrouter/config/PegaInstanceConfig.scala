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

package uk.gov.hmrc.tradergoodsprofilesrouter.config

import play.api.{ConfigLoader, Configuration}

case class PegaInstanceConfig(
  protocol: String,
  host: String,
  port: Int,
  requestAdvice: String,
  forwardedHost: String,
  requestAdviceToken: String,
  getRecords: String,
  recordGetToken: String,
  withdrawAdvise: String,
  withdrawAdviseToken: String
) {
  lazy val getRecordsUrl: String    = s"$protocol://$host:$port$getRecords"
  lazy val requestAdviceUrl: String = s"$protocol://$host:$port$requestAdvice"
  lazy val requestAdviceBearerToken = s"Bearer $requestAdviceToken"
  lazy val getRecordBearerToken     = s"Bearer $recordGetToken"
  lazy val getWithdrawAdviseUrl = s"$protocol://$host:$port$withdrawAdvise"
  lazy val getWithdrawAdviceBearerToken     = s"Bearer $withdrawAdviseToken"
}

object PegaInstanceConfig {

  implicit lazy val configLoader: ConfigLoader[PegaInstanceConfig] =
    ConfigLoader { rootConfig => path =>
      val config: Configuration = Configuration(rootConfig.getConfig(path))
      PegaInstanceConfig(
        config.get[String]("protocol"),
        config.get[String]("host"),
        config.get[Int]("port"),
        config.get[String]("request-advice"),
        config.get[String]("forwarded-host"),
        config.getOptional[String]("request-advice-token").getOrElse("dummyWithdrawAdviceBearerToken"),
        config.get[String]("get-records"),
        config.getOptional[String]("record-get-token").getOrElse("dummyRecordGetBearerToken"),
        config.get[String]("withdraw-advice"),
        config.getOptional[String]("withdraw-advice-token").getOrElse("dummyWithdrawAdviceBearerToken")
      )
    }
}
