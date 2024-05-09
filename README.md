
# Router Service for TGP (Trader Goods Profile)

## About

This is the router for TGP; it mostly acts as a conduit between the frontend (web and API) and the integration layer: EIS (
Enterprise Integration Services).

By default, this service runs on port `10904`.

## Running

In order to run the following examples, ensure you first have [trader-goods-profiles-stubs](https://github.com/hmrc/trader-goods-profiles-stubs) running;


You can then run this service in a variety of ways.

For sbt:

``` 
sbt run
```


## API

### Get Single Record

Here's an example of a successful call to get a single record:

```bash
curl -X GET \
  'http://localhost:10904/trader-goods-profiles-router/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f' \
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
  "eori": "GB1234567890",
  "actorId": "GB1234567890",
  "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
  "traderRef": "BAN001001",
  "comcode": "104101000",
  "accreditationRequest": "Not requested",
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
  "srcSystemName": "CDAP",
  "createdDateTime": "2024-11-18T23:20:19Z",
  "updatedDateTime": "2024-11-18T23:20:19Z"
}
```

To get a sense of the various scenarios, you could look at the integration [tests](it/test/uk/gov/hmrc/tradergoodsprofilesrouter/GetSingleRecordSpec.scala) and one

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