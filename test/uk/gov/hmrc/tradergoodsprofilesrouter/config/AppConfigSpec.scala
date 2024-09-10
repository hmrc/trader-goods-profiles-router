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
      |features.clientIdHeaderDisabled=true
    """.stripMargin

  private def createAppConfig(configSettings: String) = {
    val config        = ConfigFactory.parseString(configSettings)
    val configuration = Configuration(config)
    new AppConfig(configuration)
  }

  val configService: AppConfig = createAppConfig(validAppConfig)

  "AppConfig" should {

    "return false if contentTypeHeaderDisabled is missing" in {
      createAppConfig("").isContentTypeHeaderDisabled mustBe false
    }

    "return false if contentTypeHeaderDisabled is false" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |features.contentTypeHeaderDisabled=false
          |""".stripMargin
      createAppConfig(validAppConfig).isContentTypeHeaderDisabled mustBe false
    }

    "return true if contentTypeHeaderDisabled is true" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |features.contentTypeHeaderDisabled=true
          |""".stripMargin
      createAppConfig(validAppConfig).isContentTypeHeaderDisabled mustBe true
    }

    "return true for clientIdHeaderDisabled when it is set to true" in {
      val config =
        """
          |appName=trader-goods-profiles-router
          |features.shouldSendClientIdHeader=true
          |""".stripMargin
      createAppConfig(config).shouldSendClientIdHeader mustBe true
    }

    "return false for clientIdHeaderDisabled when it is missing" in {
      createAppConfig("").shouldSendClientIdHeader mustBe false
    }

    "return false for clientIdHeaderDisabled when it is set to false" in {
      val config =
        """
          |appName=trader-goods-profiles-router
          |features.clientIdHeaderDisabled=false
          |""".stripMargin
      createAppConfig(config).shouldSendClientIdHeader mustBe false
    }
  }

  "isPatchMethodEnabled" should {
    "return true if patchMethodEnabled is true" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |features.patchMethodEnabled=true
          |""".stripMargin
      createAppConfig(validAppConfig).isPatchMethodEnabled mustBe true
    }

    "return false if patchMethodEnabled is false" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |features.patchMethodEnabled=false
          |""".stripMargin
      createAppConfig(validAppConfig).isPatchMethodEnabled mustBe false
    }

    "return false if patchMethodEnabled is nor defined" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |""".stripMargin
      createAppConfig(validAppConfig).isPatchMethodEnabled mustBe false
    }
  }

}
