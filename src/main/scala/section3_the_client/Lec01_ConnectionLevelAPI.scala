package section3_the_client

import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import section3_the_client.PaymentSystemDomain.PaymentRequest
import spray.json._

object Lec01_ConnectionLevelAPI extends App with PaymentJsonProtocol {

  implicit val system = ActorSystem("Lec01_ConnectionLevelAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val connectionFlow = Http().outgoingConnection("www.google.com") // materialize in a Future of OutgoingConnection

  def oneOfRequest(request: HttpRequest) =
    Source.single(request).via(connectionFlow).runWith(Sink.head)

  oneOfRequest(HttpRequest()).onComplete {
    case Success(response) => println(s"Got successful response: $response")
    case Failure(ex) => println(s"Sending the request failed: $ex")
  }

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniel-account")
  )

  val paymentRequest = creditCards.map(PaymentRequest(_, "rtjvm-store-account",99))
  val serverHttpRequests = paymentRequest.map(paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  )
  Source(serverHttpRequests)
    .via(Http().outgoingConnection("localhost", 8080))
    .to(Sink.foreach[HttpResponse](println))
    .run()
}
