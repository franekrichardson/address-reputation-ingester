/*
 *
 *  *
 *  *  * Copyright 2016 HM Revenue & Customs
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 *
 */

package controllers

import config.ApplicationGlobal
import controllers.SimpleValidator._
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.mvc.{Action, AnyContent}
import services.exec.WorkerFactory
import uk.co.hmrc.address.services.mongo.CasbahMongoConnection
import uk.co.hmrc.logging.{LoggerFacade, SimpleLogger}
import uk.gov.hmrc.play.microservice.controller.BaseController


object CollectionController extends CollectionController(
  new WorkerFactory,
  new LoggerFacade(Logger.logger),
  ApplicationGlobal.mongoConnection,
  ApplicationGlobal.metadataStore
)


class CollectionController(workerFactory: WorkerFactory,
                           logger: SimpleLogger,
                           mongoDbConnection: CasbahMongoConnection,
                           systemMetadata: SystemMetadataStore) extends BaseController {

  private lazy val db = mongoDbConnection.getConfiguredDb

  def listCollections: Action[AnyContent] = Action {
    request =>
      val disallowed = protectedCollections

      val names = db.collectionNames().toList.sorted
      val result = for (name <- names) yield {
        val collection = db(name)
        val canDrop = !disallowed.contains(name)
        val metadata = collection.findOneByID("metadata")
        if (metadata.isDefined) {
          val completedAt = Option(metadata.get.get("completedAt").toString)
          CollectionInfo(name, collection.size, canDrop, completedAt)
        } else {
          CollectionInfo(name, collection.size, canDrop)
        }
      }
      Ok(Json.toJson(ListCI(result)))
  }

  def dropCollection(name: String): Action[AnyContent] = Action {
    request =>
      require(isAlphaNumeric(name))
      if (!db.collectionExists(name)) {
        NotFound
      } else {
        if (protectedCollections.contains(name)) {
          BadRequest
        } else {
          db(name).dropCollection()
          //          TODO SeeOther(routes.CollectionController.listCollections())
          SeeOther("/collections/list")
        }
      }
  }

  private val systemCollections = Set("system.indexes", "admin")

  private def protectedCollections = {
    val currentAbp = systemMetadata.addressBaseCollectionItem("abp").get
    val currentAbi = systemMetadata.addressBaseCollectionItem("abi").get
    systemCollections + currentAbp + currentAbi
  }

  implicit val CollectionInfoReads: Reads[CollectionInfo] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "size").read[Int] and
      (JsPath \ "canDrop").read[Boolean] and
      (JsPath \ "completedAt").readNullable[String]) (CollectionInfo.apply _)

  implicit val CollectionInfoWrites: Writes[CollectionInfo] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "size").write[Int] and
      (JsPath \ "canDrop").write[Boolean] and
      (JsPath \ "completedAt").writeNullable[String]) (unlift(CollectionInfo.unapply))

  implicit val ListCollectionInfoWrites: Writes[ListCI] = Json.format[ListCI]
}


case class CollectionInfo(name: String, size: Int, canDrop: Boolean, completedAt: Option[String] = None)

case class ListCI(collections: List[CollectionInfo])