import java.util.concurrent.TimeUnit

import sangria.ast.{Document, OperationType}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.{QueryParser, SyntaxError}
import sangria.parser.DeliveryScheme.Try
import sangria.marshalling.circe._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.parser._
import com.typesafe.config.ConfigFactory

import scala.util.control.NonFatal
import scala.util.{Failure, Success}
import GraphQLRequestUnmarshaller._
import akka.event.Logging
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.stream.actor.{ActorPublisher, ActorSubscriber}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import de.heikoseeberger.akkasse.ServerSentEvent
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import models.{Event, UserEvent}
import mongodb.Mongo
import repository.UserRepository
import sangria.slowlog.SlowLog
import user.UserManagerAPI
import volskayaSystem.VolskayaActorSystem._
import volskayaSystem.VolskayaController

import scala.concurrent.duration.Duration

object Server extends App with CorsSupport {

  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val repository = new UserRepository(Mongo.usersCollection)
  val config  = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  val logger = Logging(system, getClass)

  val usersSink = Sink.fromSubscriber(ActorSubscriber[UserEvent](usersView))
  val eventStorePublisher = Source.fromPublisher(ActorPublisher[Event](eventStore))
    .runWith(Sink.asPublisher(fanout = true))
  // Connect event store to views
  Source.fromPublisher(eventStorePublisher).collect { case event: UserEvent => event }.to(usersSink).run()

  def executeGraphQL(query: Document, operationName: Option[String], variables: Option[Json], tracing: Boolean) = {
    val operation = query.operationType(operationName)
    operation match  {
      case Some(OperationType.Subscription) =>
        import sangria.execution.ExecutionScheme.Stream
        import sangria.streaming.akkaStreams._
        val executor = Executor(SchemaDefinition.createSchema(timeout,ec, materializer))
        complete(
          executor.prepare(query, VolskayaController(system, eventStorePublisher), (), operationName = operationName,
            variables = variables.getOrElse(Json.obj()))
            .map { preparedQuery =>
              ToResponseMarshallable(preparedQuery.execute()
                .map(result => ServerSentEvent(result.toString()))
                .recover{ case NonFatal(error) =>
                  logger.error(error, "Unexpected error during event stream processing.")
                  ServerSentEvent(error.getMessage)
                })
            }.recover {
            case error: QueryAnalysisError => ToResponseMarshallable(BadRequest -> error.resolveError)
            case error: ErrorWithResolver => ToResponseMarshallable(InternalServerError -> error.resolveError)
          })
      case _ =>
        complete(Executor.execute(SchemaDefinition.createSchema(timeout,ec, materializer), query, VolskayaController(system, eventStorePublisher),
          variables = variables.getOrElse(Json.obj()),
          operationName = operationName,
          middleware = if (tracing) SlowLog.apolloTracing :: Nil else Nil)
          .map(OK -> _)
          .recover {
            case error: QueryAnalysisError => BadRequest -> error.resolveError
            case error: ErrorWithResolver => InternalServerError -> error.resolveError
          })
    }
  }

  def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError =>
      Json.obj("errors" -> Json.arr(
        Json.obj(
          "message" -> Json.fromString(syntaxError.getMessage),
          "locations" -> Json.arr(Json.obj(
            "line" -> Json.fromBigInt(syntaxError.originalError.position.line),
            "column" -> Json.fromBigInt(syntaxError.originalError.position.column))))))
    case NonFatal(e) =>
      formatError(e.getMessage)
    case e =>
      throw e
  }

  def formatError(message: String): Json =
    Json.obj("errors" -> Json.arr(Json.obj("message" -> Json.fromString(message))))

  val route:Route =
    optionalHeaderValueByName("X-Apollo-Tracing") { tracing =>
      path("graphql") {
        get {
          explicitlyAccepts(`text/html`) {
            getFromResource("assets/playground.html")
          } ~
            parameters('query, 'operationName.?, 'variables.?) { (query, operationName, variables) =>
              QueryParser.parse(query) match {
                case Success(ast) =>
                  variables.map(parse) match {
                    case Some(Left(error)) => complete(BadRequest, formatError(error))
                    case Some(Right(json)) => executeGraphQL(ast, operationName, Some(json), tracing.isDefined)
                    case None => executeGraphQL(ast, operationName, Some(Json.obj()), tracing.isDefined)
                  }
                case Failure(error) ⇒ complete(BadRequest, formatError(error))
              }
            }
        } ~
        post {
          parameters('query.?, 'operationName.?, 'variables.?) { (queryParam, operationNameParam, variablesParam) ⇒
            entity(as[Json]) { body =>
              val query = queryParam orElse root.query.string.getOption(body)
              val operationName = operationNameParam orElse root.operationName.string.getOption(body)
              val variablesStr = variablesParam orElse root.variables.string.getOption(body)

              query.map(QueryParser.parse(_)) match {
                case Some(Success(ast)) =>
                  variablesStr.map(parse) match {
                    case Some(Left(error)) => complete(BadRequest, formatError(error))
                    case Some(Right(json)) => executeGraphQL(ast, operationName, Some(json), tracing.isDefined)
                    case None => executeGraphQL(ast, operationName, root.variables.json.getOption(body), tracing.isDefined)
                  }
                case Some(Failure(error)) => complete(BadRequest, formatError(error))
                case None => complete(BadRequest, formatError("No query to execute"))
              }
            } ~
              entity(as[Document]) { document =>
                variablesParam.map(parse) match {
                  case Some(Left(error)) => complete(BadRequest, formatError(error))
                  case Some(Right(json)) => executeGraphQL(document, operationNameParam, Some(json), tracing.isDefined)
                  case None ⇒ executeGraphQL(document, operationNameParam, Some(Json.obj()), tracing.isDefined)
                }
              }
          }
        }
      }
    } ~
    (get & pathEndOrSingleSlash) {
      redirect("/graphql", PermanentRedirect)
    } ~
    (get & path("client")) {
      explicitlyAccepts(`text/html`) {
        getFromResource("assets/client.html")
      }
    }

  Http().bindAndHandle(corsHandler(route), host, port)
}
