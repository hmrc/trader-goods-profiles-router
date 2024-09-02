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
      |features.drop_1_1_enabled=true
      |features.clientIdOptional=true
    """.stripMargin

  private def createAppConfig(configSettings: String) = {
    val config        = ConfigFactory.parseString(configSettings)
    val configuration = Configuration(config)
    new AppConfig(configuration)
  }

  val configService: AppConfig = createAppConfig(validAppConfig)

  "AppConfig" should {
    "return true for isDrop1_1_Enabled" in {
      configService.isDrop1_1_enabled mustBe true
    }

    "return false if isDrop1_1_Enabled is missing" in {
      createAppConfig("").isDrop1_1_enabled mustBe false
    }

    "return false if isDrop2Enabled is missing" in {
      createAppConfig("").isDrop2Enabled mustBe false
    }

    "return false if isDrop2Enabled is false" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |features.drop2Enabled=false
          |""".stripMargin
      createAppConfig(validAppConfig).isDrop2Enabled mustBe false
    }

    "return true if isDrop2Enabled is true" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |features.drop2Enabled=true
          |""".stripMargin
      createAppConfig(validAppConfig).isDrop2Enabled mustBe true
    }

    "return true for clientIdOptional when it is set to true" in {
      val config =
        """
          |appName=trader-goods-profiles-router
          |features.clientIdOptional=true
          |""".stripMargin
      createAppConfig(config).isClientIdOptional mustBe true
    }

    "return false for clientIdOptional when it is missing" in {
      createAppConfig("").isClientIdOptional mustBe false
    }

    "return false for clientIdOptional when it is set to false" in {
      val config =
        """
          |appName=trader-goods-profiles-router
          |features.clientIdOptional=false
          |""".stripMargin
      createAppConfig(config).isClientIdOptional mustBe false
    }
  }
}
