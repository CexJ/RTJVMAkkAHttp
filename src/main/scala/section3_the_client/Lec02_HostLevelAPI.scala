package section3_the_client

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import section3_the_client.PaymentSystemDomain.PaymentRequest
import spray.json._

import scala.util.{Failure, Success}

object Lec02_HostLevelAPI extends App with PaymentJsonProtocol {

  implicit val system = ActorSystem("Lec01_ConnectionLevel")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val poolFlow = Http().cachedHostConnectionPool[Int]("www.google.com")

  Source(1 to 10)
    .map(i => (HttpRequest(), i))
    .via(poolFlow)
    .map {
      case (Success(response), value) =>
        response.discardEntityBytes()
        s"request $value: has received: $response"
      case (Failure(ex), value) =>
        s"request $value has fail: $ex"
    }.runWith(Sink.foreach[String](println))


  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniel-account")
  )

  val paymentRequest = creditCards.map(PaymentRequest(_, "rtjvm-store-account",99))
  val serverHttpRequests = paymentRequest.map(paymentRequest =>
    (HttpRequest(
      HttpMethods.POST,
      uri = Uri("/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    ), UUID.randomUUID().toString))

  Source(serverHttpRequests)
    .via(Http().cachedHostConnectionPool[String]("localhost",8080))
    .runForeach{
      case (Success(response), orderId) => println(s"the order id $orderId return: $response")
      case (Failure(ex), orderId) => println(s"the order id $orderId fail: $ex")
    }
}
