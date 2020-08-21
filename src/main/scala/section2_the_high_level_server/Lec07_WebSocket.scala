package section2_the_high_level_server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.CompactByteString

object Lec07_WebSocket extends App {

  implicit val system = ActorSystem("Lec07_WebSocket")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  val textMessage = TextMessage(Source.single("Hello there"))
  val binaryMessage = BinaryMessage(Source.single(CompactByteString("Hello binary")))

  val html =
    """
      |<html>
      |    <head>
      |        <script>
      |            var exampleSocket = new WebSocket("ws://localhost:8080/greeter");
      |            console.log("starting websocket...");
      |
      |            exampleSocket.onmessage = function(event) {
      |                var newChild = document.createElement("div");
      |                newChild.innerText = event.data;
      |                document.getElementById("1").appendChild(newChild);
      |            };
      |
      |            exampleSocket.onopen = function(event) {
      |                exampleSocket.send("socket seems to be open...");
      |            };
      |
      |            exampleSocket.send("socket says: hello, server!");
      |        </script>
      |    </head>
      |
      |    <body>
      |        Starting websocket...
      |        <div id="1">
      |        </div>
      |    </body>
      |</html>
      |""".stripMargin

  import akka.http.scaladsl.server.directives._

  def websocketFlow: Flow[Message, Message, Any] =
    Flow[Message].map{
      case tm: TextMessage =>
        TextMessage(Source.single("Server serve back") ++ tm.textStream ++ Source.single("!"))
      case bm: BinaryMessage =>
        bm.dataStream.runWith(Sink.ignore)
        TextMessage(Source.single("Server receive binary message"))

    }

  val websocketRoute =
    (pathEndOrSingleSlash & get) {
      complete(
        HttpEntity(ContentTypes.`text/html(UTF-8)`,html))
    } ~ path("greeter"){
      handleWebSocketMessages(websocketFlow)
    }


  case class SocialPost(owner: String, content: String)
  val socialFeed = Source(List(
    SocialPost("martin", "scala 3 has been announced"),
    SocialPost("daniel", "new rtjvm has been announced"),
    SocialPost("martin", "I killed java")
  ))

  import scala.concurrent.duration._
  val socialMessages =
    socialFeed.throttle(1, 1 second)
    .map(sp => TextMessage(s"${sp.owner}: ${sp.content}"))
  val socialFlow: Flow[Message, Message, Any] =
    Flow.fromSinkAndSource(Sink.foreach[Message](println), socialMessages)
}
