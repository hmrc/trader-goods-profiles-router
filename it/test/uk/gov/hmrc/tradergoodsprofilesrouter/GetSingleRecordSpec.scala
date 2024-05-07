package uk.gov.hmrc.tradergoodsprofilesrouter
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.Mockito.when
import play.api.http.Status._

import java.time.Instant
import java.util.UUID

class GetSingleRecordSpec extends BaseIntegrationWithConnectorSpec {

  val eori                           = "GB123456789001"
  val recordId                       = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  override def connectorPath: String = s"/tgp/getrecords/v1"
  override def connectorName: String = "eis"

  override def beforeEach(): Unit = {
    when(uuidService.uuid()).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to get records, when" - {
    "the request is" - {
      "valid" in {

        stubFor(
          get(urlEqualTo(s"$connectorPath/$eori"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("X-Forwarded-Host", equalTo("MDTP"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("bearerToken"))
            .withHeader("X-Client-ID", equalTo("tss"))
            .willReturn(
              aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(OK)
                .withBody(s"""
                       |{
                       |    "goodsItemRecords": [
                       |        {
                       |            "eori": "GB1234567890",
                       |            "actorId": "GB1234567890",
                       |            "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                       |            "traderRef": "BAN001001",
                       |            "comcode": "104101000",
                       |            "accreditationRequest": "Not requested",
                       |            "goodsDescription": "Organic bananas",
                       |            "countryOfOrigin": "EC",
                       |            "category": 3,
                       |            "assessments": [
                       |                {
                       |                    "assessmentId": "abc123",
                       |                    "primaryCategory": "1",
                       |                    "condition": {
                       |                        "type": "abc123",
                       |                        "conditionId": "Y923",
                       |                        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                       |                        "conditionTraderText": "Excluded product"
                       |                    }
                       |                }
                       |            ],
                       |            "supplementaryUnit": 500,
                       |            "measurementUnit": "square meters(m^2)",
                       |            "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
                       |            "comcodeEffectiveToDate": "",
                       |            "version": 1,
                       |            "active": true,
                       |            "toReview": false,
                       |            "reviewReason": null,
                       |            "declarable": "IMMI declarable",
                       |            "ukimsNumber": "XIUKIM47699357400020231115081800",
                       |            "nirmsNumber": "RMS-GB-123456",
                       |            "niphlNumber": "6 S12345",
                       |            "locked": false,
                       |            "srcSystemName": "CDAP",
                       |            "createdDateTime": "2024-11-18T23:20:19Z",
                       |            "updatedDateTime": "2024-11-18T23:20:19Z"
                       |        }
                       |    ],
                       |    "pagination": {
                       |        "totalRecords": 1,
                       |        "currentPage": 0,
                       |        "totalPages": 1,
                       |        "nextPage": null,
                       |        "prevPage": null
                       |    }
                       |}
                       |""".stripMargin)
            )
        )

        val response = wsClient
          .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
          .get()
          .futureValue

        assertAsExpected(
          response = response,
          status = OK,
          jsonBodyOpt = Some(
            """
                |{
                |    "eori": "GB1234567890",
                |    "actorId": "GB1234567890",
                |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                |    "traderRef": "BAN001001",
                |    "comcode": "104101000",
                |    "accreditationRequest": "Not requested",
                |    "goodsDescription": "Organic bananas",
                |    "countryOfOrigin": "EC",
                |    "category": 3,
                |    "assessments": null,
                |    "supplementaryUnit": 500,
                |    "measurementUnit": "square meters(m^2)",
                |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
                |    "comcodeEffectiveToDate": "",
                |    "version": 1,
                |    "active": true,
                |    "toReview": false,
                |    "reviewReason": null,
                |    "declarable": "IMMI declarable",
                |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                |    "nirmsNumber": "RMS-GB-123456",
                |    "niphlNumber": "6 S12345",
                |    "locked": false,
                |    "srcSystemName": "CDAP",
                |    "createdDateTime": "2024-11-18T23:20:19Z",
                |    "updatedDateTime": "2024-11-18T23:20:19Z"
                |}
                |""".stripMargin
          )
        )
        verifyThatDownstreamApiWasCalled()
      }
      "valid but the integration call fails with response:" - {
        "Internal Server Error" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("2021-12-17T09:30:47.456Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-ID", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(
                    s"""
                       |{
                       |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                       |    "code": "INTERNAL_SERVER_ERROR",
                       |    "message": "Internal Server Error"
                       |}
                       |""".stripMargin)
              )
          )

          val response = wsClient
            .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"),)
            .get()
            .futureValue

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "INTERNAL_SERVER_ERROR",
                |    "message": "Internal Server Error"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }

      }
    }
  }
}
