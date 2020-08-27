package section3_the_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import section3_the_client.PaymentSystemDomain.PaymentRequest
import spray.json._

import scala.util.{Failure, Success}

object Lec03_RequestLevelAPI extends App with PaymentJsonProtocol {

  implicit val system = ActorSystem("Lec03_RequestLevelAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val responseFuture = Http().singleRequest(HttpRequest(uri = "http://www.google.com"))

  responseFuture.onComplete{
    case Success(response) =>
      response.discardEntityBytes()
      println(s"The response: $response")
    case Failure(ex) =>
      println(s"the request fail: $ex")
  }

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniel-account")
  )

  val paymentRequest = creditCards.map(PaymentRequest(_, "rtjvm-store-account",99))
  val serverHttpRequests = paymentRequest.map(paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("http://localhost:8080/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  )

  Source(serverHttpRequests)
    .mapAsync(10)(request => Http().singleRequest(request))
    .runForeach(println)
}
