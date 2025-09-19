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

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration

class AppConfigSpec extends PlaySpec {
  private val validAppConfig =
    """
      |appName=trader-goods-profiles-router
      |microservice.services.hawk.protocol = http
      |        microservice.services.hawk.host = localhost
      |        microservice.services.hawk.port = 10903
      |        microservice.services.hawk.forwarded-host = "MDTP"
      |        microservice.services.hawk.get-records  = "/tgp/getrecords/v1"
      |        microservice.services.hawk.create-record  = "/tgp/createrecord/v1"
      |        microservice.services.hawk.remove-record  = "/tgp/removerecord/v1"
      |        microservice.services.hawk.update-record  = "/tgp/updaterecord/v1"
      |        microservice.services.hawk.put-update-record  = "/tgp/puttgprecord/v1"
      |        microservice.services.hawk.maintain-profile  = "/tgp/maintainprofile/v1"
      |        microservice.services.hawk.get-profile = "/tgp/getprofile/v1"
      |        microservice.services.hawk.create-profile  = "/tgp/createprofile/v1"
      |        microservice.services.hawk.record-update-token = "c29tZS10b2tlbgo="
      |        microservice.services.hawk.put-record-token = "c29tZS10b2tlbgo="
      |        microservice.services.hawk.record-get-token = "c29tZS10b2tlbgo="
      |        microservice.services.hawk.record-create-token = "c29tZS10b2tlbgo="
      |        microservice.services.hawk.record-remove-token = "c29tZS10b2tlbgo="
      |        microservice.services.hawk.maintain-profile-token = "c29tZS10b2tlbgo="
      |        microservice.services.hawk.get-profile-token = "c29tZS10b2tlbgo="
      |        microservice.services.hawk.create-profile-token = "c29tZS10b2tlbgo="
      |        microservice.services.hawk.default-size = 500
      |        microservice.services.hawk.max-size = 500
      |        microservice.services.pega.protocol = http
      |        microservice.services.pega.host = localhost
      |        microservice.services.pega.port = 10903
      |        microservice.services.pega.forwarded-host = "MDTP"
      |
      |        microservice.services.pega.get-records  = "/tgp/getrecords/v1"
      |        microservice.services.pega.request-advice  = "/tgp/createaccreditation/v1"
      |        microservice.services.pega.withdraw-advice = "/tgp/withdrawaccreditation/v1"
      |        microservice.services.pega.download-trader-data = "/tgp/record/v1"
      |        microservice.services.pega.request-advice-token = "dummyRequestAdviceBearerToken"
      |        microservice.services.pega.record-get-token = "dummyRecordGetBearerToken"
      |        microservice.services.pega.withdraw-advice-token = "dummyWithdrawAdviceBearerToken"
      |        microservice.services.pega.download-trader-data-token = "dummyDownloadTraderDataToken"
    """.stripMargin

  private def createAppConfig(configSettings: String) = {
    val config        = ConfigFactory.parseString(configSettings)
    val configuration = Configuration(config)
    new AppConfig(configuration)
  }

  val configService: AppConfig = createAppConfig(validAppConfig)

  "AppConfig" should {
    "parse hawk config" in {
      val hawkConfig = HawkInstanceConfig(
        "http",
        "localhost",
        10903,
        "/tgp/getrecords/v1",
        "/tgp/createrecord/v1",
        "/tgp/removerecord/v1",
        "/tgp/updaterecord/v1",
        "/tgp/puttgprecord/v1",
        "/tgp/maintainprofile/v1",
        "/tgp/createprofile/v1",
        "/tgp/getprofile/v1",
        "MDTP",
        "c29tZS10b2tlbgo=",
        "c29tZS10b2tlbgo=",
        "c29tZS10b2tlbgo=",
        "c29tZS10b2tlbgo=",
        "c29tZS10b2tlbgo=",
        "c29tZS10b2tlbgo=",
        "c29tZS10b2tlbgo=",
        "c29tZS10b2tlbgo=",
        500,
        500
      )
      configService.hawkConfig mustBe hawkConfig
    }
    "parse pega config" in {
      val pegaConfig = PegaInstanceConfig(
        "http",
        "localhost",
        10903,
        "/tgp/createaccreditation/v1",
        "MDTP",
        "dummyRequestAdviceBearerToken",
        "/tgp/getrecords/v1",
        "dummyRecordGetBearerToken",
        "/tgp/withdrawaccreditation/v1",
        "dummyWithdrawAdviceBearerToken",
        "/tgp/record/v1",
        "dummyDownloadTraderDataToken"
      )
      configService.pegaConfig mustBe pegaConfig
    }
  }
}
