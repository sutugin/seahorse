/**
 * Copyright (c) 2015, CodiLime Inc.
 *
 * Owner: Wojciech Jurczyk
 */

package io.deepsense.graphjson

import spray.json._

import io.deepsense.deeplang.DOperation
import io.deepsense.deeplang.catalogs.doperations.DOperationsCatalog

object OperationJsonProtocol {

  val Operation = "operation"
  val Version = "version"
  val Name = "name"
  val Parameters = "parameters"

  implicit object DOperationWriter extends JsonWriter[DOperation] with DefaultJsonProtocol {
    override def write(operation: DOperation): JsValue = {
      JsObject(
        Operation -> JsObject(
          Name -> operation.name.toJson),
        Parameters -> operation.parameters.valueToJson)
    }
  }

  class DOperationReader(catalog: DOperationsCatalog)
    extends JsonReader[DOperation]
    with DefaultJsonProtocol {
    override def read(json: JsValue): DOperation = json match {
      case JsObject(fields) =>
        val operationJs = fields(Operation).asJsObject
        val operation = catalog
          .createDOperation(operationJs.fields(Name).convertTo[String])
        operation.parameters.fillValueWithJson(fields(Parameters))
        operation
      case x =>
        throw new DeserializationException(s"Expected JsObject with a node but got $x")
    }
  }
}
