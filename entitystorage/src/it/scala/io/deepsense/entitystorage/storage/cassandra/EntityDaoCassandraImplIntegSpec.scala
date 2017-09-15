/**
 * Copyright (c) 2015, CodiLime Inc.
 *
 * Owner: Wojciech Jurczyk
 */

package io.deepsense.entitystorage.storage.cassandra

import scala.concurrent.{Await, Future}

import com.datastax.driver.core.querybuilder.QueryBuilder
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import io.deepsense.commons.StandardSpec
import io.deepsense.commons.datetime.DateTimeConverter
import io.deepsense.entitystorage.CassandraTestSupport
import io.deepsense.entitystorage.factories.EntityTestFactory
import io.deepsense.entitystorage.models.{DataObjectReference, Entity}

class EntityDaoCassandraImplIntegSpec
  extends StandardSpec
  with ScalaFutures
  with Matchers
  with BeforeAndAfter
  with CassandraTestSupport
  with EntityTestFactory {

  var entities: EntityDaoCassandraImpl = _

  before {
    session.execute(createTableCommand(table))
    entities = new EntityDaoCassandraImpl(table, session)
  }

  // Fixture
  val tenantId = "TestTenantId"
  val created = DateTimeConverter.now
  val updated = created.plusDays(5)
  val dataObjectReference = testDataObjectReference
  val dataObjectReport = testDataObjectReport

  val entity1 = testEntity(tenantId, 3, Some(dataObjectReference), Some(dataObjectReport))
  val entity2 = testEntity(tenantId, 14, None, Some(dataObjectReport))
  val entity3 = testEntity(tenantId + "otherTenant", 15, None, Some(dataObjectReport))
  val inDb = Set(entity1, entity2, entity3)

  "Entities" should {
    "select all rows from a tenant" in withStoredEntities(inDb) {
      whenReady(entities.getAll(tenantId)) { tenantEntities =>
        tenantEntities should contain theSameElementsAs Seq(entity1, entity2)
      }
    }
    "save and get an entity" that {
      val tenantId = "save_get_tenantId"
      "is a report" in {
        val entity = testEntity(tenantId, 0, None, Some(dataObjectReport))
        whenReady(entities.upsert(entity)) { _ =>
          whenReady(entities.get(tenantId, entity.id)) { getEntity =>
            getEntity shouldBe Some(entity)
          }
        }
      }
      "is an url" in {
        val entity = testEntity(tenantId, 0, Some(dataObjectReference), Some(dataObjectReport))
        whenReady(entities.upsert(entity)) { _ =>
          whenReady(entities.get(tenantId, entity.id)) { getEntity =>
            getEntity shouldBe Some(entity)
          }
        }
      }
    }
    "update an entity" in withStoredEntities(inDb) {
      val differentEntity = modifyEntity(entity1)
      whenReady(entities.upsert(differentEntity)) { _ =>
        whenReady(entities.get(tenantId, entity1.id)) { getEntity =>
          getEntity shouldBe Some(differentEntity)
        }
      }
    }
    "delete an entity" in {
      whenReady(entities.delete(tenantId, entity1.id)) { _ =>
        whenReady(entities.get(tenantId, entity1.id)) { getEntity =>
          getEntity shouldBe None
        }
      }
    }
  }

  private def modifyEntity(entity: Entity): Entity = {
    val s = "modified"
    entity.copy(
      name = entity.name + s,
      description = entity.description + s,
      dClass = entity.dClass + s,
      created = entity.created.plusDays(1),
      updated = entity.updated.plusDays(1),
      data = Some(DataObjectReference(entity.name + entity.description)),
      saved = !entity.saved)
  }

  private def withStoredEntities(storedEntities: Set[Entity])(testCode: => Any): Unit = {
    val s = Future.sequence(storedEntities.map(entities.upsert))
    Await.ready(s, operationDuration)
    try {
      testCode
    } finally {
      session.execute(QueryBuilder.truncate(table))
    }
  }

  private def createTableCommand(table: String): String = {
    s"create table if not exists $table (" +
    """
      tenantid text,
      id uuid,
      name text,
      description text,
      dclass text,
      created timestamp,
      updated timestamp,
      url text,
      saved boolean,
      primary key (tenantid, id)
      );
    """
  }
}