package uk.gov.hmrc.tradergoodsprofilesrouter
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._

class GetRecordsSpec extends BaseIntegrationWithConnectorSpec {

  val eori                           = "GB123456789001"
  val recordId                       = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  override def connectorPath: String = s"/tgp/getrecords/v1"
  override def connectorName: String = "eis"

  "attempting to get records, when" - {
    "the request for a single record is" - {
      "valid, when" - {
        "requesting for a single record" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withHeader("X-Correlation-ID", "3e8dae97-b586-4cef-8511-68ac12da9028")
                  .withHeader("Date", "2021-12-17T09:30:47.456Z")
                  .withHeader("X-Forwarded-Host", "uk.gov.hmrc")
                  .withHeader("Accept", "application/json")
                  .withHeader("Authorization", "bearerToken")
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
            .withHttpHeaders(("Content-Type", "application/json"))
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
        "requesting for multiple records" in {
          stubFor(
            post(urlEqualTo(connectorPath))
              .withRequestBody(equalToJson(s"""
                   |{
                   |    "idType": "NINO",
                   |    "idNumber": "AA000000A",
                   |    "tradingName": "Harold Winter",
                   |    "gbUser": true,
                   |    "primaryContact": {
                   |        "individual": {
                   |            "firstName": "Patrick",
                   |            "middleName": "John",
                   |            "lastName": "Dyson"
                   |        },
                   |        "email": "Patrick.Dyson@example.com",
                   |        "mobile": "747663966",
                   |        "phone": "38390756243"
                   |    }
                   |}
                   |""".stripMargin))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(CREATED)
                  .withBody(s"""
                       |{
                       |  "success": {
                       |    "processingDate": "2001-12-17T09:30:47Z",
                       |    "dprsReference": "XSP1234567890"
                       |  }
                       |}
                       |""".stripMargin)
              )
          )

          val response = wsClient
            .url(fullUrl("/subscriptions"))
            .withHttpHeaders(("Content-Type", "application/json"))
            .post("""
                |{
                |    "id": {
                |        "type": "NINO",
                |        "value": "AA000000A"
                |    },
                |    "name": "Harold Winter",
                |    "contacts": [
                |        {
                |            "type": "I",
                |            "firstName": "Patrick",
                |            "middleName": "John",
                |            "lastName": "Dyson",
                |            "landline": "747663966",
                |            "mobile": "38390756243",
                |            "emailAddress": "Patrick.Dyson@example.com"
                |        }
                |    ]
                |}
                |""".stripMargin)
            .futureValue

          assertAsExpected(
            response = response,
            status = OK,
            jsonBodyOpt = Some(
              """
                |{
                |  "id": "XSP1234567890"
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
