
# Router Service for TGP (Trader Goods Profile)

## About

This is the router for TGP; it acts as a conduit between the frontend (web and API) and the integration layer: EIS (
Enterprise Integration Services).

By default, this service runs on port `10904`.

## Running

In order to run the following examples, ensure you first have [trader-goods-profiles-stubs](https://github.com/hmrc/trader-goods-profiles-stubs) running;


You can then run this service in a variety of ways.

For sbt:

``` 
sbt run
```

### Test the API locally

Notice: You can use the run_local.sh script file to load all needed services and start the trader-goods-profiles service.
#### Start the services
* Open a terminal window, type the below command and press enter. This will load locally all the auth services necessary for testing :

    ```
    sm2 --start AUTH_ALL
    ```

#### Generate an access token
* Use the [Auth wizard](http://localhost:9949/auth-login-stub/gg-sign-in)
    * Fill the following details:
      <br><br>

      **Redirect Url**: http://localhost:9949/auth-login-stub/session <br>
      **Affinity Group**: Organisation or Individual<br>
      **Enrolment Key**: HMRC-CUS-ORG <br>
      **Identifier Name**: EORINumber <br>
      **Identifier Value**: GB123456789001 (or anything else similar). Refer to the service guide to get a list of EROI
      number suitable for test or look at the stubs [README.md file](https://github.com/hmrc/trader-goods-profiles-stubs/blob/main/README.md)
      <br><br>
* Press submit. This will redirect to a new page showing an access token.
* Copy the Bearer token

* In the Authorization tab select **Bearer Token** as **Auth Type**
* Add the access token create on [this step](#generate-an-access-token)
* Add the right url
* Send the request.

## API

### Get Single Record

Here's an example of a successful call to get a single record:

```bash
curl --location 'http://localhost:10904/trader-goods-profiles-router/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f' \
--header 'X-Correlation-ID: 3e8dae97-b586-4cef-8511-68ac12da9028' \
--header 'Date: 2021-12-17T09:30:47.456Z' \
--header 'X-Forwarded-Host: uk.gov.hmrc' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--header 'Authorization: bearerToken' \
--header 'X-Client-ID: tss'
```

```json
{
  "eori": "GB1234567890",
  "actorId": "GB1234567890",
  "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
  "traderRef": "BAN001001",
  "comcode": "10410100",
  "adviceStatus": "Not requested",
  "goodsDescription": "Organic bananas",
  "countryOfOrigin": "EC",
  "category": 3,
  "assessments": null,
  "supplementaryUnit": 500,
  "measurementUnit": "square meters(m^2)",
  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
  "comcodeEffectiveToDate": "",
  "version": 1,
  "active": true,
  "toReview": false,
  "reviewReason": null,
  "declarable": "IMMI declarable",
  "ukimsNumber": "XIUKIM47699357400020231115081800",
  "nirmsNumber": "RMS-GB-123456",
  "niphlNumber": "6 S12345",
  "locked": false,
  "createdDateTime": "2024-11-18T23:20:19Z",
  "updatedDateTime": "2024-11-18T23:20:19Z"
}
```

To get a sense of the various scenarios, you could look at the integration [tests](it/test/uk/gov/hmrc/tradergoodsprofilesrouter/GetSingleRecordIntegrationSpec.scala)

### Get Multiple Records

Here's an example of a successful call to get a multiple records:

```bash
curl -X GET \
  'http://localhost:10904/trader-goods-profiles-router/GB123456789001?lastUpdatedDate=2021-12-17T09:30:47.456Z&page=1&size=1' \
  -H 'X-Correlation-ID:3e8dae97-b586-4cef-8511-68ac12da9028' \
  -H 'Date:2021-12-17T09:30:47.456Z' \
  -H 'X-Forwarded-Host:uk.gov.hmrc' \
  -H 'Content-Type:application/json' \
  -H 'Accept:application/json' \
  -H 'Authorization:bearerToken' \
  -H 'X-Client-ID:tss'
```

```json
{
  "goodsItemRecords":
  [
    {
      "eori": "GB1234567890",
      "actorId": "GB1234567890",
      "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      "traderRef": "BAN001001",
      "comcode": "10410100",
      "adviceStatus": "Not requested",
      "goodsDescription": "Organic bananas",
      "countryOfOrigin": "EC",
      "category": 3,
      "assessments": [
        {
          "assessmentId": "abc123",
          "primaryCategory": "1",
          "condition": {
            "type": "abc123",
            "conditionId": "Y923",
            "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
            "conditionTraderText": "Excluded product"
          }
        }
      ],
      "supplementaryUnit": 500,
      "measurementUnit": "square meters(m^2)",
      "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
      "comcodeEffectiveToDate": "",
      "version": 1,
      "active": true,
      "toReview": false,
      "reviewReason": null,
      "declarable": "IMMI declarable",
      "ukimsNumber": "XIUKIM47699357400020231115081800",
      "nirmsNumber": "RMS-GB-123456",
      "niphlNumber": "6 S12345",
      "locked": false,
      "createdDateTime": "2024-11-18T23:20:19Z",
      "updatedDateTime": "2024-11-18T23:20:19Z"
    }
  ],
  "pagination":
  {
    "totalRecords": 1,
    "currentPage": 0,
    "totalPages": 1,
    "nextPage": null,
    "prevPage": null
  }
}
```

To get a sense of the various scenarios, you could look at the integration [tests](it/test/uk/gov/hmrc/tradergoodsprofilesrouter/GetMultipleRecordsIntegrationSpec.scala)


### Create Record

Here's an example of a successful call to create a record:

```bash
curl --location 'http://localhost:10904/trader-goods-profiles-router/records' \
--header 'X-Correlation-ID: 3e8dae97-b586-4cef-8511-68ac12da9028' \
--header 'Date: 2021-12-17T09:30:47.456Z' \
--header 'X-Forwarded-Host: uk.gov.hmrc' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--header 'Authorization: bearerToken' \
--header 'X-Client-ID: tss' \
--data '{
    "eori": "GB123456789012",
    "actorId": "GB098765432112",
    "traderRef": "BAN001001",
    "comcode": "10410100",
    "goodsDescription": "Organic bananas",
    "countryOfOrigin": "EC",
    "category": 1,
    "assessments": [
        {
            "assessmentId": "abc123",
            "primaryCategory": 1,
            "condition": {
                "type": "abc123",
                "conditionId": "Y923",
                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                "conditionTraderText": "Excluded product"
            }
        }
    ],
    "supplementaryUnit": 500,
    "measurementUnit": "Square metre (m2)",
    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
}'

```

```json
{
  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
  "eori": "GB123456789012",
  "actorId": "GB098765432112",
  "traderRef": "BAN001001",
  "comcode": "10410100",
  "adviceStatus": "Not Requested",
  "goodsDescription": "Organic bananas",
  "countryOfOrigin": "EC",
  "category": 1,
  "supplementaryUnit": 500,
  "measurementUnit": "Square metre (m2)",
  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
  "version": 1,
  "active": true,
  "toReview": false,
  "reviewReason": "Commodity code change",
  "declarable": "SPIMM",
  "ukimsNumber": "XIUKIM47699357400020231115081800",
  "nirmsNumber": "RMS-GB-123456",
  "niphlNumber": "6 S12345",
  "createdDateTime": "2024-11-18T23->20->19Z",
  "updatedDateTime": "2024-11-18T23->20->19Z",
  "assessments": [
    {
      "assessmentId": "abc123",
      "primaryCategory": 1,
      "condition": {
        "type": "abc123",
        "conditionId": "Y923",
        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        "conditionTraderText": "Excluded product"
      }
    }
  ]
}
```

To get a sense of the various scenarios, you could look at the integration [tests](it/test/uk/gov/hmrc/tradergoodsprofilesrouter/CreateRecordIntegrationSpec.scala)

### Remove Record

Here's an example of a successful call to remove a record:

```bash
curl --location 'http://localhost:10904/trader-goods-profiles-router/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f' \
--header 'X-Correlation-ID: 3e8dae97-b586-4cef-8511-68ac12da9028' \
--header 'Date: 2021-12-17T09:30:47.456Z' \
--header 'X-Forwarded-Host: uk.gov.hmrc' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--header 'Authorization: bearerToken' \
--header 'X-Client-ID: tss' \
--data '{
    "actorId": "GB123456789001"
}'

```
Returns successful response with 200 status code and with no payload  

To get a sense of the various scenarios, you could look at the integration [tests](it/test/uk/gov/hmrc/tradergoodsprofilesrouter/RemoveRecordIntegrationSpec.scala)

### Update Record

Here's an example of a successful call to update a record:

```bash
curl --location --request PUT 'http://localhost:10904/trader-goods-profiles-router/records' \
--header 'X-Correlation-ID: 3e8dae97-b586-4cef-8511-68ac12da9028' \
--header 'Date: 2021-12-17T09:30:47.456Z' \
--header 'X-Forwarded-Host: uk.gov.hmrc' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--header 'Authorization: bearerToken' \
--header 'X-Client-ID: tss' \
--data '{
    "eori": "GB123456789001",
    "actorId": "GB098765432112",
    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    "traderRef": "BAN001001",
    "comcode": "10410100",
    "goodsDescription": "Organic bananas",
    "countryOfOrigin": "EC",
    "category": 1,
    "assessments": [
        {
            "assessmentId": "abc123",
            "primaryCategory": 1,
            "condition": {
                "type": "abc123",
                "conditionId": "Y923",
                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                "conditionTraderText": "Excluded product"
            }
        }
    ],
    "supplementaryUnit": 500,
    "measurementUnit": "Square metre (m2)",
    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
}'

```

```json
{
  "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
  "eori": "GB123456789001",
  "actorId": "GB098765432112",
  "traderRef": "BAN001001",
  "comcode": "10410100",
  "adviceStatus": "Not Requested",
  "goodsDescription": "Organic bananas",
  "countryOfOrigin": "EC",
  "category": 1,
  "assessments": [
    {
      "assessmentId": "abc123",
      "primaryCategory": 1,
      "condition": {
        "type": "abc123",
        "conditionId": "Y923",
        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        "conditionTraderText": "Excluded product"
      }
    }
  ],
  "supplementaryUnit": 500,
  "measurementUnit": "Square metre (m2)",
  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
  "version": 1,
  "active": true,
  "toReview": false,
  "reviewReason": "Commodity code change",
  "declarable": "SPIMM",
  "ukimsNumber": "XIUKIM47699357400020231115081800",
  "nirmsNumber": "RMS-GB-123456",
  "niphlNumber": "6 S12345",
  "createdDateTime": "2024-11-18T23->20->19Z",
  "updatedDateTime": "2024-11-18T23->20->19Z"
}
```

To get a sense of the various scenarios, you could look at the integration [tests](it/test/uk/gov/hmrc/tradergoodsprofilesrouter/UpdateRecordIntegrationSpec.scala)


## Dev

Before pushing, you can run [verify.sh](./verify.sh) which will run all the tests, as well as check the format.

### Tests

#### Unit

```
sbt test
```

#### Integration

``` 
sbt "it/test"
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").