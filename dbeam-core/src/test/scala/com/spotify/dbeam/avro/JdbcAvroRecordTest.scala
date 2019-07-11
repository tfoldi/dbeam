/*
 * Copyright 2017 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.dbeam.avro

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID

import com.spotify.dbeam.JdbcTestFixtures
import org.apache.avro.Schema
import org.apache.avro.file.{DataFileReader, DataFileWriter, SeekableByteArrayInput}
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import slick.jdbc.H2Profile.api._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class JdbcAvroRecordTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  private val connectionUrl: String =
    "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
  private val db: Database = Database.forURL(connectionUrl, driver = "org.h2.Driver")
  private val record1 = JdbcTestFixtures.record1

  override def beforeAll(): Unit = {
    JdbcTestFixtures.createFixtures(db, Seq(JdbcTestFixtures.record1, JdbcTestFixtures.record2))
  }

  it should "create schema" in {
    val fieldCount = 12
    val actual: Schema = JdbcAvroSchema.createSchemaByReadingOneRow(
      db.source.createConnection(),
      "COFFEES", "dbeam_generated",
      "Generate schema from JDBC ResultSet from COFFEES jdbc:h2:mem:test", false)

    actual shouldNot be (null)
    actual.getNamespace should be ("dbeam_generated")
    actual.getProp("tableName") should be ("COFFEES")
    actual.getProp("connectionUrl") should be ("jdbc:h2:mem:test")
    actual.getFields.size() should be (fieldCount)
    actual.getFields.asScala.map(_.name()) should
      be (List("COF_NAME", "SUP_ID", "PRICE", "TEMPERATURE", "SIZE",
               "IS_ARABIC", "SALES", "TOTAL", "CREATED", "UPDATED", "UID", "ROWNUM"))
    actual.getFields.asScala.map(_.schema().getType) should
      be (List.fill(fieldCount)(Schema.Type.UNION))
    actual.getFields.asScala.map(_.schema().getTypes.get(0).getType) should
      be (List.fill(fieldCount)(Schema.Type.NULL))
    actual.getFields.asScala.map(_.schema().getTypes.size()) should be (List.fill(fieldCount)(2))
    actual.getField("COF_NAME").schema().getTypes.get(1).getType should be (Schema.Type.STRING)
    actual.getField("SUP_ID").schema().getTypes.get(1).getType should be (Schema.Type.INT)
    actual.getField("PRICE").schema().getTypes.get(1).getType should be (Schema.Type.STRING)
    actual.getField("TEMPERATURE").schema().getTypes.get(1).getType should be (Schema.Type.FLOAT)
    actual.getField("SIZE").schema().getTypes.get(1).getType should be (Schema.Type.DOUBLE)
    actual.getField("IS_ARABIC").schema().getTypes.get(1).getType should be (Schema.Type.BOOLEAN)
    actual.getField("SALES").schema().getTypes.get(1).getType should be (Schema.Type.INT)
    actual.getField("TOTAL").schema().getTypes.get(1).getType should be (Schema.Type.LONG)
    actual.getField("CREATED").schema().getTypes.get(1).getType should be (Schema.Type.LONG)
    actual.getField("UPDATED").schema().getTypes.get(1).getType should be (Schema.Type.LONG)
    actual.getField("UPDATED").schema().getTypes.get(1).getProp("logicalType") should be (null)
    actual.getField("UID").schema().getTypes.get(1).getType should be (Schema.Type.BYTES)
    actual.getField("ROWNUM").schema().getTypes.get(1).getType should be (Schema.Type.LONG)
    actual.toString shouldNot be (null)
    actual.getDoc should be ("Generate schema from JDBC ResultSet from COFFEES jdbc:h2:mem:test")
  }

  it should "create schema with logical types" in {
    val fieldCount = 12
    val actual: Schema = JdbcAvroSchema.createSchemaByReadingOneRow(
      db.source.createConnection(),
      "COFFEES", "dbeam_generated",
      "Generate schema from JDBC ResultSet from COFFEES jdbc:h2:mem:test",
      true)

    actual shouldNot be (null)
    actual.getNamespace should be ("dbeam_generated")
    actual.getProp("tableName") should be ("COFFEES")
    actual.getProp("connectionUrl") should be ("jdbc:h2:mem:test")
    actual.getFields.size() should be (fieldCount)
    actual.getFields.asScala.map(_.name()) should
      be (List("COF_NAME", "SUP_ID", "PRICE", "TEMPERATURE", "SIZE",
        "IS_ARABIC", "SALES", "TOTAL", "CREATED", "UPDATED", "UID", "ROWNUM"))
    actual.getFields.asScala.map(_.schema().getType) should
      be (List.fill(fieldCount)(Schema.Type.UNION))
    actual.getFields.asScala.map(_.schema().getTypes.get(0).getType) should
      be (List.fill(fieldCount)(Schema.Type.NULL))
    actual.getFields.asScala.map(_.schema().getTypes.size()) should be (List.fill(fieldCount)(2))
    actual.getField("COF_NAME").schema().getTypes.get(1).getType should be (Schema.Type.STRING)
    actual.getField("SUP_ID").schema().getTypes.get(1).getType should be (Schema.Type.INT)
    actual.getField("PRICE").schema().getTypes.get(1).getType should be (Schema.Type.STRING)
    actual.getField("TEMPERATURE").schema().getTypes.get(1).getType should be (Schema.Type.FLOAT)
    actual.getField("SIZE").schema().getTypes.get(1).getType should be (Schema.Type.DOUBLE)
    actual.getField("IS_ARABIC").schema().getTypes.get(1).getType should be (Schema.Type.BOOLEAN)
    actual.getField("SALES").schema().getTypes.get(1).getType should be (Schema.Type.INT)
    actual.getField("TOTAL").schema().getTypes.get(1).getType should be (Schema.Type.LONG)
    actual.getField("CREATED").schema().getTypes.get(1).getType should be (Schema.Type.LONG)
    actual.getField("UPDATED").schema().getTypes.get(1).getType should be (Schema.Type.LONG)
    actual.getField("UPDATED").schema().getTypes.get(1).getProp("logicalType") should
      be ("timestamp-millis")
    actual.getField("UID").schema().getTypes.get(1).getType should be (Schema.Type.BYTES)
    actual.getField("ROWNUM").schema().getTypes.get(1).getType should be (Schema.Type.LONG)
    actual.toString shouldNot be (null)
    actual.getDoc should be ("Generate schema from JDBC ResultSet from COFFEES jdbc:h2:mem:test")
  }

  it should "create schema with specified namespace and doc string" in {
    val actual: Schema = JdbcAvroSchema.createSchemaByReadingOneRow(
      db.source.createConnection(), "COFFEES", "ns", "doc", false)

    actual shouldNot be (null)
    actual.getNamespace should be ("ns")
    actual.getDoc should be ("doc")
  }

  def toByteBuffer(uuid: UUID): ByteBuffer = {
    val bf = ByteBuffer.allocate(16)
      .putLong(uuid.getMostSignificantBits)
      .putLong(uuid.getLeastSignificantBits)
    bf.clear()
    bf
  }

  it should "encode jdbc result set to valid avro" in {
    val rs = db.source.createConnection().createStatement().executeQuery(s"SELECT * FROM COFFEES")
    val schema = JdbcAvroSchema.createAvroSchema(rs, "dbeam_generated","connection", "doc", false)
    val converter = JdbcAvroRecordConverter.create(rs)
    val dataFileWriter = new DataFileWriter(new GenericDatumWriter[GenericRecord](schema))
    val outputStream = new ByteArrayOutputStream()
    dataFileWriter.create(schema, outputStream)
    // convert and write
    while (rs.next()) {
      dataFileWriter.appendEncoded(converter.convertResultSetIntoAvroBytes())
    }
    dataFileWriter.flush()
    outputStream.close()
    // transform to generic record
    val inputStream = new SeekableByteArrayInput(outputStream.toByteArray)
    val dataFileReader = new DataFileReader[GenericRecord](inputStream,
      new GenericDatumReader[GenericRecord](schema))
    val records: Seq[GenericRecord] =
      dataFileReader.asInstanceOf[java.lang.Iterable[GenericRecord]].asScala.toSeq
    records.size should be (2)
    val record: GenericRecord = records.filter(r => r.get(0).toString == record1._1).head

    record shouldNot be (null)
    record.getSchema should be (schema)
    record.getSchema.getFields.size() should be (12)
    record.get(0).toString should be (record1._1)
    record.get(1) should be (record1._2.map(x => x : java.lang.Integer).orNull)
    record.get(2).toString should be (record1._3.toString)
    record.get(3) should be (record1._4)
    record.get(4) should be (record1._5)
    record.get(5) should be (new java.lang.Boolean(record1._6))
    record.get(6) should be (record1._7)
    record.get(7) should be (record1._8)
    record.get(8) should be (record1._9.getTime)
    record.get(9) should be (record1._10.map(_.getTime).map(x => x : java.lang.Long).orNull)
    record.get(10) should be (toByteBuffer(record1._11))
    record.get(11) should be (record1._12)
  }

}
