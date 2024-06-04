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

case class Headers(authorization: String)

case class EISInstanceConfig(
  protocol: String,
  host: String,
  port: Int,
  getRecords: String,
  createRecord: String,
  removeRecord: String,
  updateRecord: String,
  forwardedHost: String,
  updateRecordToken: String,
  recordGetToken: String,
  recordCreateToken: String,
  recordRemoveToken: String,
  accreditationCreateToken: String
) {
  lazy val getRecordsUrl: String   = s"$protocol://$host:$port$getRecords"
  lazy val createRecordUrl: String = s"$protocol://$host:$port$createRecord"
  lazy val removeRecordUrl: String = s"$protocol://$host:$port$removeRecord"
  lazy val updateRecordUrl: String = s"$protocol://$host:$port$updateRecord"

  lazy val recordUpdateToken = s"Bearer $updateRecordToken"
  lazy val recordGetToken    = s"Bearer $updateRecordToken"
  lazy val recordUpdateToken = s"Bearer $updateRecordToken"
  lazy val recordUpdateToken = s"Bearer $updateRecordToken"
  lazy val recordUpdateToken = s"Bearer $updateRecordToken"
  lazy val recordUpdateToken = s"Bearer $updateRecordToken"
}

object EISInstanceConfig {

  implicit lazy val configLoader: ConfigLoader[EISInstanceConfig] =
    ConfigLoader { rootConfig => path =>
      val config: Configuration = Configuration(rootConfig.getConfig(path))
      EISInstanceConfig(
        config.get[String]("protocol"),
        config.get[String]("host"),
        config.get[Int]("port"),
        config.get[String]("get-records"),
        config.get[String]("create-record"),
        config.get[String]("remove-record"),
        config.get[String]("update-record"),
        config.get[String]("forwarded-host"),
        config.getOptional[String]("record-update-token").getOrElse("dummyRecordUpdateBearerToken"),
        config.getOptional[String]("record-get-token").getOrElse("recordGetBearerToke"),
        config.getOptional[String]("record-create-token").getOrElse("recordCreateBearerToke"),
        config.getOptional[String]("record-remove-token").getOrElse("recordRemoveToken"),
        config.getOptional[String]("accreditation-create-token").getOrElse("accreditationCreateToken")
      )
    }
}
