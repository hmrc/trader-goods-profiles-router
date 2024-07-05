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

case class EISInstanceConfig(
  protocol: String,
  host: String,
  port: Int,
  getRecords: String,
  createRecord: String,
  removeRecord: String,
  updateRecord: String,
  maintainProfile: String,
  requestAdvice: String,
  withdrawAdvice: String,
  forwardedHost: String,
  updateRecordToken: String,
  recordGetToken: String,
  recordCreateToken: String,
  recordRemoveToken: String,
  requestAdviceToken: String,
  maintainProfileToken: String,
  withdrawAdviceToken: String
) {
  lazy val getRecordsUrl: String      = s"$protocol://$host:$port$getRecords"
  lazy val createRecordUrl: String    = s"$protocol://$host:$port$createRecord"
  lazy val removeRecordUrl: String    = s"$protocol://$host:$port$removeRecord"
  lazy val updateRecordUrl: String    = s"$protocol://$host:$port$updateRecord"
  lazy val maintainProfileUrl: String = s"$protocol://$host:$port$maintainProfile"
  lazy val requestAdviceUrl: String   = s"$protocol://$host:$port$requestAdvice"
  lazy val withdrawAdviceUrl          = s"$protocol://$host:$port$withdrawAdvice"

  lazy val updateRecordBearerToken    = s"Bearer $updateRecordToken"
  lazy val getRecordBearerToken       = s"Bearer $recordGetToken"
  lazy val createRecordBearerToken    = s"Bearer $recordCreateToken"
  lazy val removeRecordBearerToken    = s"Bearer $recordRemoveToken"
  lazy val requestAdviceBearerToken   = s"Bearer $requestAdviceToken"
  lazy val maintainProfileBearerToken = s"Bearer $maintainProfileToken"
  lazy val withdrawAdviceBearerToken  = s"Bearer $withdrawAdviceToken"

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
        config.get[String]("maintain-profile"),
        config.get[String]("request-advice"),
        config.get[String]("withdraw-advice"),
        config.get[String]("forwarded-host"),
        config.getOptional[String]("record-update-token").getOrElse("dummyRecordUpdateBearerToken"),
        config.getOptional[String]("record-get-token").getOrElse("dummyRecordGetBearerToken"),
        config.getOptional[String]("record-create-token").getOrElse("dummyRecordCreateBearerToken"),
        config.getOptional[String]("record-remove-token").getOrElse("dummyRecordRemoveBearerToken"),
        config.getOptional[String]("request-advice-token").getOrElse("dummyRequestAdviceBearerToken"),
        config.getOptional[String]("maintain-profile-token").getOrElse("dummyMaintainProfileBearerToken"),
        config.getOptional[String]("withdraw-advice-token").getOrElse("dummyWithdrawAdviceBearerToken")
      )
    }
}
