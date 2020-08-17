package section1_the_low_level_server

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import section1_the_low_level_server.Lec03_LowLevelHttps.getClass

object HttpsContext {


  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keyStoreFile: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")
  val password = "akka-https".toCharArray
  ks.load(keyStoreFile,password)

  val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  trustManagerFactory.init(ks)

  val sslContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers,trustManagerFactory.getTrustManagers, new SecureRandom())

  val httpsConnectionContext = ConnectionContext.https(sslContext)
}

object Lec03_LowLevelHttps extends App {

  implicit val system = ActorSystem("Lec03_LowLevelHttps")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val requestHandler: Function[HttpRequest, HttpResponse] = {
    case HttpRequest(HttpMethods.GET,Uri.Path("/home"),_,_,_) => // method, URI, HTTP header, content, protocol
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka Http!
            | </body>
            |</html>
            |""".stripMargin
        )
      )

    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Not found!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  Http().bindAndHandleSync(requestHandler, "localhost", 8080, HttpsContext.httpsConnectionContext)

}
