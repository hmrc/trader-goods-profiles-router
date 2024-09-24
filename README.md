
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

#### Start the services
* Open a terminal window, type the below command and press enter. This will load locally all the auth services and TGP services necessary for testing :

    ```
    sm2 --start TGP_API
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

## Postman collection

[postman collection](TGP_Router.postman_collection.json)


## Dev

Before pushing, you can run [precheck.sh](./precheck.sh) which will run all the tests, as well as check the format.

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