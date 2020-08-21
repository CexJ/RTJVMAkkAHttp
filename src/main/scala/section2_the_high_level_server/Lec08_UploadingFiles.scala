package section2_the_high_level_server

import java.io.File

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}

import scala.concurrent.Future
import scala.util.{Failure, Success}


object Lec08_UploadingFiles extends App {

  implicit val system = ActorSystem("Lec08_UploadingFiles")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  val filesRoute = {
    (pathEndOrSingleSlash & get){
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    <form action="http://localhost:8080/upload" method="post" enctype="multipart/form-data">
            |      <input type="file" name="myFile">
            |      <button type="submit">Upload</button>
            |    </form>
            |  </body>
            |</html>
          """.stripMargin
        )
      )
    } ~
    (path("upload") & extractLog) { log =>
      // handle uploading files
      // multipart/form-data
      entity(as[Multipart.FormData]){ formData =>
        // handle file payload
        val partsSource = formData.parts
        val filePartsSink: Sink[Multipart.FormData.BodyPart, Future[Done]] =
          Sink.foreach{ bodyPart =>
            if(bodyPart.name == "rtjvm") {
              val fileName = s"src/main/resources/download/${bodyPart.filename.getOrElse("tempFile_"+System.currentTimeMillis())}"
              val file = new File(fileName)
              log.info(s"Writing on file $fileName")
              val fileContentsSource = bodyPart.entity.dataBytes
              val fileContentsSink = FileIO.toPath(file.toPath)
              fileContentsSource.runWith(fileContentsSink)
            }
          }

        val writeOperationFuture = partsSource.runWith(filePartsSink)

        onComplete(writeOperationFuture) { // onComplete directive!
          case Success(_) => complete("File uploaded")
          case Failure(ex) => complete(s"Upload failed because: $ex")
        }
      }
    }
  }

  Http().bindAndHandle(filesRoute, "localhost", 8080)
}
