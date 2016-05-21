package com.clianz.cloudant

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.actor.Actor
import akka.event.Logging
import fommil.sjs.FamilyFormats._
import spray.client.pipelining._
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.marshalling.Marshaller
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

trait CloudantOps {
  this: Actor =>

  implicit val ec: ExecutionContext
  implicit val cloudantCred: CloudantCredential

  // -------------------- Logging and Pipelines -------------------- //

  val cloudantLog = Logging(context.system, this)
  val logRequest: HttpRequest => HttpRequest = { r => cloudantLog.debug(r.toString); r }
  val logResponse: HttpResponse => HttpResponse = { r => cloudantLog.debug(r.toString); r }

  lazy val pipeline: HttpRequest => Future[HttpResponse] = logRequest ~>
    addCredentials(BasicHttpCredentials(cloudantCred.accountName, cloudantCred.password)) ~>
    sendReceive ~>
    logResponse

  private def dbPipe(req: HttpRequest): Future[String] = pipeline(
    req
  ) map { res =>
    if (res.status.isFailure) throw new CloudantException(s"Cloudant responded with status: ${res.status}", res.entity.asString, res.status)
    res.entity.asString
  }

  private def dbBaseUrl = s"https://${cloudantCred.username}.cloudant.com"

  // -------------------- Convenience Unmarshaller -------------------- //

  class UnmarshalFuture(val s: Future[String]) {
    def unmarshalTo[T: JsonFormat]: Future[T] = s.map(_.parseJson.convertTo[T])

    def unmarshalSearchViewResult: Future[ViewResult] = s.map(_.parseJson.convertTo[ViewResult])

    // 'Field' type is required, this is because the value is any type and Spray-Json won't let us use Map(String, Any)
    def unmarshalSearchIndexResult[T: JsonFormat, F: JsonFormat](implicit ev: JsonFormat[IndexResult[T, F]]):
    Future[IndexResult[T, F]] =
      s.map(_.parseJson.convertTo[IndexResult[T, F]])
  }

  implicit def stringToJsonFormatObj(f: Future[String]) = new UnmarshalFuture(f)

  // -------------------- Simple Cloudant Ops - Always return Future[String] -------------------- //

  // -------- Database Ops -------- //

  def createDb(dbName: String): Future[String] = dbPipe(
    Put(s"$dbBaseUrl/$dbName"))

  def retrieveDb(dbName: String): Future[String] = dbPipe(
    Get(s"$dbBaseUrl/$dbName"))

  def retrieveAllDbs(): Future[String] = dbPipe(
    Get(s"$dbBaseUrl/_all_dbs"))

//  def retrieveAllDocs()(implicit dbName: String): Future[String] = dbPipe(  // TODO
//    Get(s"$dbBaseUrl/$dbName/_all_docs"))

//  def retrieveChanges()(implicit dbName: String): Future[String] = dbPipe(  // TODO
//    Get(s"$dbBaseUrl/$dbName/_changes"))

  def deleteDb()(implicit dbName: String): Future[String] = dbPipe(
    Delete(s"$dbBaseUrl/$dbName"))

  // -------- Document Ops -------- //

  def createOrUpdateDoc(doc: String)(implicit dbName: String): Future[String] = dbPipe(
    Post(s"$dbBaseUrl/$dbName", JsonContentType(doc)))

  def createOrUpdateDoc[T: JsonFormat](doc: T)(implicit dbName: String): Future[String] =
    createOrUpdateDoc(doc.toJson.compactPrint)

  def createOrUpdateOrDeleteBulkDoc[T: JsonFormat](docs: List[T])(implicit dbName: String, ev: JsonFormat[BulkDoc[T]]):
  Future[String] = dbPipe(
    Post(s"$dbBaseUrl/$dbName/_bulk_docs", JsonContentType(BulkDoc[T](docs).toJson.compactPrint)))

  def retrieveDoc(docId: String)(implicit dbName: String): Future[String] = dbPipe(
    Get(s"$dbBaseUrl/$dbName/$docId"))

  def deleteDoc(docId: String, rev: String)(implicit dbName: String): Future[String] = dbPipe(
    Delete(s"$dbBaseUrl/$dbName/$docId?rev=$rev"))

  // -------- Views and Indices Ops -------- //

  def createOrUpdateIndex(indexDoc: IndexDoc)(implicit dbName: String): Future[String] =
    createOrUpdateDoc(indexDoc.toJson.compactPrint)

  def createOrUpdateView(viewDoc: ViewDoc)(implicit dbName: String): Future[String] =
    createOrUpdateDoc(viewDoc.toJson.compactPrint)

  def searchIndex(query: SearchQuery, dbIndex: DbIndex)(implicit dbName: String): Future[String] = dbPipe(
    Post(s"$dbBaseUrl/$dbName/_design/${dbIndex.indexName}/_search/${dbIndex.indexDoc}",
      query.toJson.compactPrint))

  /** This doesn't support composite key query. ["ABC", 123] = NO!! We only support simple key query: "ABC-123". */
  def searchView(query: ViewQuery, dbIndex: DbView)(implicit dbName: String): Future[String] = {
    // Ref: http://docs.couchdb.org/en/1.6.1/api/ddoc/views.html
    var queries: Map[String, String] = Map()
    Map("conflicts" -> query.conflicts,
      "descending" -> query.descending,
      "endkey" -> query.endkey,
      "endkey_docid" -> query.endkey_docid,
      "group" -> query.group,
      "group_level" -> query.group_level,
      "include_docs" -> query.include_docs,
      "attachments" -> query.attachments,
      "att_encoding_info" -> query.att_encoding_info,
      "inclusive_end" -> query.inclusive_end,
      "key" -> query.key,
      "limit" -> query.limit,
      "reduce" -> query.reduce,
      "skip" -> query.skip,
      "stale" -> query.stale,
      "startkey" -> query.startkey,
      "startkey_docid" -> query.startkey_docid,
      "update_seq" -> query.update_seq
    ) foreach {
      case (k, Some(v: String)) if k.equals("startkey") || k.equals("endkey") =>
        queries += (k -> ("\"" + urlEncode(v) + "\""))
      case (k, Some(v: String)) => queries += (k -> v)
      case (k, Some(v: Long)) => queries += (k -> v.toString)
      case (k, Some(v: Boolean)) => queries += (k -> v.toString)
      case _ => // cloudantLog.debug(s"Nothing found for key $k")
    }
    val uri = Uri(s"$dbBaseUrl/$dbName/_design/${dbIndex.viewName}/_view/${dbIndex.viewDoc}").withQuery(queries)

    if (query.keys.isDefined) dbPipe(Post(uri, JsonContentType(ViewQueryKeys(query.keys.get).toJson.compactPrint)))
    else dbPipe(Get(uri))
  }

  private def urlEncode = URLEncoder.encode(_:String, StandardCharsets.UTF_8.toString)

  // -------------------- POJOs (POSOs) -------------------- //

  case class CloudantCredential(username: String, accountName: String, password: String) {
    def this(username: String, password: String) = this(username, username, password)
  }

  case class BulkDoc[T: JsonFormat](docs: List[T])

  case class IndexDoc(_id: Option[String] = None,
                      _rev: Option[String] = None,
                      indexes: Map[String, SearchIndex])

  case class SearchIndex(index: String, analyzer: String = "standard")

  case class ViewDoc(_id: Option[String] = None,
                     _rev: Option[String] = None,
                     views: Map[String, MapReduce],
                     language: String = "javascript")

  case class MapReduce(map: String, reduce: String)

  case class DbIndex(indexName: String, indexDoc: String)

  case class SearchQuery(q: String,
                         include_docs: Boolean = false,
                         sort: String = "[]",
                         limit: Int = 25)

  case class DbView(viewName: String, viewDoc: String)

  case class ViewQuery(conflicts: Option[Boolean] = None,
                       descending: Option[Boolean] = None,
                       endkey: Option[String] = None,
                       endkey_docid: Option[String] = None,
                       group: Option[Boolean] = None,
                       group_level: Option[Long] = None,
                       include_docs: Option[Boolean] = None,
                       attachments: Option[Boolean] = None,
                       att_encoding_info: Option[Boolean] = None,
                       inclusive_end: Option[Boolean] = None,
                       key: Option[String] = None,
                       keys: Option[List[String]] = None,
                       limit: Option[Long] = None,
                       reduce: Option[Boolean] = None,
                       skip: Option[Long] = None,
                       stale: Option[String] = None,
                       startkey: Option[String] = None,
                       startkey_docid: Option[String] = None,
                       update_seq: Option[Boolean] = None)

  case class ViewQueryKeys(keys: List[String])

  case class ViewResult(rows: List[Kvp],
                        total_rows: Option[Long] = None,
                        offset: Option[Long] = None,
                        update_seq : Option[Long] = None)

  case class IndexResultRows[T: JsonFormat, F: JsonFormat](id: String,
                                                           order: List[Double],
                                                           fields: F,
                                                           doc: Option[T])

  case class IndexResult[T: JsonFormat, F: JsonFormat](total_rows: Long,
                                                       bookmark: String,
                                                       rows: List[IndexResultRows[T, F]])

  case class Kvp(key: String, value: String)

  case class CloudantException(message: String, responseBody: String, statusCode: StatusCode) extends Exception(message)

  // Force the spray-client Content-Type header to JSON, otherwise it'll be plain text.
  implicit val docMarshaller = Marshaller.of[JsonContentType](`application/json`) {
    (value, ct, ctx) => ctx.marshalTo(HttpEntity(ct, value.content))
  }

  case class JsonContentType(content: String)

}