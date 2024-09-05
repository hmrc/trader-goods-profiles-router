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

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (config: Configuration) {

  lazy val hawkConfig: HawkInstanceConfig = config.get[HawkInstanceConfig]("microservice.services.hawk")
  lazy val pegaConfig: PegaInstanceConfig = config.get[PegaInstanceConfig]("microservice.services.pega")

  // TODO: After Drop 1.1 this should be removed - Ticket: TGP-2014
  lazy val isDrop1_1_enabled: Boolean =
    config
      .getOptional[Boolean]("features.drop_1_1_enabled")
      .getOrElse(false)

  lazy val isNiphlPaddingEnabled: Boolean =
    config
      .getOptional[Boolean]("features.niphlPaddingEnabled")
      .getOrElse(true)

  lazy val acceptHeaderDisabled: Boolean = config.getOptional[Boolean]("feature.acceptHeaderDisabled").getOrElse(false)

  lazy val isContentTypeHeaderDisabled: Boolean =
    config
      .getOptional[Boolean]("features.contentTypeHeaderDisabled")
      .getOrElse(false)

  lazy val isClientIdHeaderDisabled: Boolean =
    config.getOptional[Boolean]("features.clientIdHeaderDisabled").getOrElse(false)

  lazy val isDrop2Enabled: Boolean =
    config
      .getOptional[Boolean]("features.drop2Enabled")
      .getOrElse(false)
}
