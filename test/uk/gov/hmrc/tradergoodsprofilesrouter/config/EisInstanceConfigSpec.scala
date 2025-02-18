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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.tradergoodsprofilesrouter.config.EISInstanceConfig

class EISInstanceConfigSpec extends AnyWordSpec with Matchers {

  "EISInstanceConfig" should {
    "construct URLs correctly" in {
      val config = EISInstanceConfig(
        protocol = "https",
        host = "example.com",
        port = 443,
        getRecords = "/getRecords",
        createRecord = "/createRecord",
        removeRecord = "/removeRecord",
        updateRecord = "/updateRecord",
        maintainProfile = "/maintainProfile",
        forwardedHost = "MDTP",
        updateRecordToken = "updateToken",
        recordGetToken = "getToken",
        recordCreateToken = "createToken",
        recordRemoveToken = "removeToken",
        maintainProfileToken = "maintainToken"
      )

      config.getRecordsUrl mustBe "https://example.com:443/getRecords"
      config.createRecordUrl mustBe "https://example.com:443/createRecord"
      config.removeRecordUrl mustBe "https://example.com:443/removeRecord"
      config.updateRecordUrl mustBe "https://example.com:443/updateRecord"
      config.maintainProfileUrl mustBe "https://example.com:443/maintainProfile"
    }

    "construct Bearer tokens correctly" in {
      val config = EISInstanceConfig(
        protocol = "https",
        host = "example.com",
        port = 443,
        getRecords = "/getRecords",
        createRecord = "/createRecord",
        removeRecord = "/removeRecord",
        updateRecord = "/updateRecord",
        maintainProfile = "/maintainProfile",
        forwardedHost = "MDTP",
        updateRecordToken = "updateToken",
        recordGetToken = "getToken",
        recordCreateToken = "createToken",
        recordRemoveToken = "removeToken",
        maintainProfileToken = "maintainToken"
      )

      config.updateRecordBearerToken mustBe "Bearer updateToken"
      config.getRecordBearerToken mustBe "Bearer getToken"
      config.createRecordBearerToken mustBe "Bearer createToken"
      config.removeRecordBearerToken mustBe "Bearer removeToken"
      config.maintainProfileBearerToken mustBe "Bearer maintainToken"
    }

    "load configuration correctly with default values for missing tokens" in {
      val config = Configuration(
        "eis.protocol" -> "http",
        "eis.host" -> "localhost",
        "eis.port" -> 8080,
        "eis.get-records" -> "/get",
        "eis.create-record" -> "/create",
        "eis.remove-record" -> "/remove",
        "eis.update-record" -> "/update",
        "eis.maintain-profile" -> "/maintain",
        "eis.forwarded-host" -> "some-host"
      )

      val eisInstanceConfig = EISInstanceConfig.configLoader.load(config.underlying, "eis") // ✅ Use "eis" as path

      eisInstanceConfig.protocol mustBe "http"
      eisInstanceConfig.host mustBe "localhost"
      eisInstanceConfig.port mustBe 8080
      eisInstanceConfig.getRecords mustBe "/get"
      eisInstanceConfig.createRecord mustBe "/create"
      eisInstanceConfig.removeRecord mustBe "/remove"
      eisInstanceConfig.updateRecord mustBe "/update"
      eisInstanceConfig.maintainProfile mustBe "/maintain"
      eisInstanceConfig.forwardedHost mustBe "some-host"

      eisInstanceConfig.updateRecordBearerToken mustBe "Bearer dummyRecordUpdateBearerToken"
      eisInstanceConfig.getRecordBearerToken mustBe "Bearer dummyRecordGetBearerToken"
      eisInstanceConfig.createRecordBearerToken mustBe "Bearer dummyRecordCreateBearerToken"
      eisInstanceConfig.removeRecordBearerToken mustBe "Bearer dummyRecordRemoveBearerToken"
      eisInstanceConfig.maintainProfileBearerToken mustBe "Bearer dummyMaintainProfileBearerToken"
    }
  }
}
