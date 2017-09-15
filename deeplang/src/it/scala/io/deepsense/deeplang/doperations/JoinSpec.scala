/**
 * Copyright (c) 2015, CodiLime Inc.
 */

package io.deepsense.deeplang.doperations

import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.scalatest.Ignore

import io.deepsense.deeplang._
import io.deepsense.deeplang.doperables.dataframe.DataFrame
import io.deepsense.deeplang.doperables.dataframe.types.categorical.CategoricalMapper
import io.deepsense.deeplang.doperations.exceptions.{ColumnsDoNotExistException, WrongColumnTypeException}
import io.deepsense.deeplang.parameters._

@Ignore
class JoinSpec extends DeeplangIntegTestSupport {
  "Join operation" should {
    "LEFT JOIN two DataFrames" when {
      "based upon a single column selection" in {
        val (ldf, rdf, expected, joinColumns) = oneColumnFixture()

        val join = joinWithMultipleColumnSelection(joinColumns, Set.empty)
        val joinDF = executeOperation(join, ldf, rdf)

        assertDataFramesEqual(joinDF, expected)
      }
      "based upon two columns" in {
        val (ldf, rdf, expected, joinColumns) = twoColumnsFixture()

        val join = joinWithMultipleColumnSelection(joinColumns, Set.empty)
        val joinDF = executeOperation(join, ldf, rdf)

        assertDataFramesEqual(joinDF, expected)
      }
      "some rows from left dataframe have no corresponding values in the right one" in {
        val (ldf, rdf, expected, joinColumns) = noSomeRightValuesFixture()

        val join = joinWithMultipleColumnSelection(joinColumns, Set.empty)
        val joinDF = executeOperation(join, ldf, rdf)

        assertDataFramesEqual(joinDF, expected)
      }
      "dataframes have no matching values" in {
        val (ldf, rdf, expected, joinColumns) = noMatchingValuesFixture()

        val join = joinWithMultipleColumnSelection(joinColumns, Set.empty)
        val joinDF = executeOperation(join, ldf, rdf)

        assertDataFramesEqual(joinDF, expected)
      }
      "some column values are null" in {
        val (ldf, rdf, expected, joinColumns) = nullFixture()

        val join = joinWithMultipleColumnSelection(joinColumns, Set.empty)
        val joinDF = executeOperation(join, ldf, rdf)

        assertDataFramesEqual(joinDF, expected)
      }
      "using categorical column" in {
        val (ldf, rdf, expected, joinColumns) = categoricalFixture()

        val join = joinWithMultipleColumnSelection(joinColumns, Set.empty)
        val joinDF = executeOperation(join, ldf, rdf)

        assertDataFramesEqual(joinDF, expected)
      }
      "with null values only in left DataFrame" in {
        val (ldf, rdf, expected, joinColumns) = nullValuesInLeftDataFrameFixture()

        val join = joinWithMultipleColumnSelection(joinColumns, Set.empty)
        val joinDF = executeOperation(join, ldf, rdf)

        assertDataFramesEqual(joinDF, expected)
      }
      "with columns of the same name in both and no join on them" in {
        val (ldf, rdf, expected, joinColumns) = sameColumnNamesFixture()

        val join = joinWithMultipleColumnSelection(joinColumns, Set.empty)
        val joinDF = executeOperation(join, ldf, rdf)

        assertDataFramesEqual(joinDF, expected)
      }
      "with null values only in both DataFrames" is pending
      "with empty join column selection" is pending
    }
    "throw an exception" when {
      "the columns selected by name does not exist" in {
        an[ColumnsDoNotExistException] should be thrownBy {
          val nonExistingColumnName = "thisColumnDoesNotExist"
          val join = joinWithMultipleColumnSelection(
            Set(nonExistingColumnName),
            Set.empty
          )
          val (ldf, rdf, _, _) = oneColumnFixture()
          executeOperation(join, ldf, rdf)
        }
      }
      "the columns selected by index does not exist" in {
        an[ColumnsDoNotExistException] should be thrownBy {
          val nonExistingColumnIndex = 1000
          val join = joinWithMultipleColumnSelection(
            Set.empty,
            Set(nonExistingColumnIndex)
          )
          val (ldf, rdf, _, _) = oneColumnFixture()
          executeOperation(join, ldf, rdf)
        }
      }
      "the columns selected by name are of different types" in {
        an[WrongColumnTypeException] should be thrownBy {
          val (ldf, rdf, _, wrongTypeColumnNames) = differentTypesFixture()
          val join = joinWithMultipleColumnSelection(
            wrongTypeColumnNames,
            Set.empty
          )
          executeOperation(join, ldf, rdf)
        }
      }
      "the joinColumns MultipleColumnSelector is empty" in {
        an[ColumnsDoNotExistException] should be thrownBy {
          val (ldf, rdf, _, wrongTypeColumnNames) = joinColumnsIsEmptyFixture()
          val join = joinWithMultipleColumnSelection(
            wrongTypeColumnNames,
            Set.empty
          )
          executeOperation(join, ldf, rdf)
        }
      }
    }
  }

  def oneColumnFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "column1"
    val joinColumns = Set(column1)

    // Left dataframe
    val colsL = Vector("column2", "column3", column1, "column4")
    val schemaL = StructType(Seq(
      StructField(colsL(0), DoubleType),
      StructField(colsL(1), StringType),
      StructField(colsL(2), DoubleType),
      StructField(colsL(3), LongType)
    ))
    val rowsL = Seq(
      (3.5, "a", 1.5, 5),
      (3.6, "b", 1.6, 6),
      (3.7, "c", 1.7, 10),
      (4.6, "d", 1.6, 9),
      (4.5, "e", 1.5, 11)
    ).map(Row.fromTuple)
    val ldf = createDataFrame(rowsL, schemaL)

    // Right dataframe
    val colsR = Vector(column1, "column22", "column5")
    val schemaR = StructType(Seq(
      StructField(colsR(0), DoubleType),
      StructField(colsR(1), DoubleType),
      StructField(colsR(2), StringType)
    ))
    val rowsR = Seq(
      (1.6, 2.6, "two"),
      (1.7, 2.7, "three"),
      (1.5, 2.5, "one"),
      (1.5, 3.5, "four")
    ).map(Row.fromTuple)
    val rdf = createDataFrame(rowsR, schemaR)

    // join dataframe
    val joinRows = Seq(
      (3.5, "a", 1.5, 5, 2.5, "one"),
      (3.5, "a", 1.5, 5, 3.5, "four"),
      (3.6, "b", 1.6, 6, 2.6, "two"),
      (3.7, "c", 1.7, 10, 2.7, "three"),
      (4.6, "d", 1.6, 9, 2.6, "two"),
      (4.5, "e", 1.5, 11, 2.5, "one"),
      (4.5, "e", 1.5, 11, 3.5, "four")
    ).map(Row.fromTuple)
    val joinSchema = StructType(schemaL.fields ++ Seq(
      StructField(colsR(1), DoubleType),
      StructField(colsR(2), StringType)
    ))
    val edf = createDataFrame(joinRows, joinSchema)

    (ldf, rdf, edf, joinColumns)
  }

  def twoColumnsFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "column1"
    val column2 = "column2"
    val joinColumns = Set(column1, column2)

    // Left dataframe
    val colsL = Vector(column2, "column3", column1, "column4")
    val schemaL = StructType(Seq(
      StructField(colsL(0), DoubleType),
      StructField(colsL(1), StringType),
      StructField(colsL(2), DoubleType),
      StructField(colsL(3), LongType)
    ))
    val rowsL = Seq(
      (2.5, "a", 1.5, 5),
      (3.6, "b", 1.6, 6),
      (3.7, "c", 1.7, 10)
    ).map(Row.fromTuple)
    val ldf = createDataFrame(rowsL, schemaL)

    // Right dataframe
    val colsR = Vector(column1, column2, "column5")
    val schemaR = StructType(Seq(
      StructField(colsR(0), DoubleType),
      StructField(colsR(1), DoubleType),
      StructField(colsR(2), StringType)
    ))
    val rowsR = Seq(
      (1.5, 2.5, "one"),
      (1.5, 3.6, "two"),
      (1.7, 3.6, "c")
    ).map(Row.fromTuple)
    val rdf = createDataFrame(rowsR, schemaR)

    // join dataframe
    val joinRows = Seq(
      (2.5, "a", 1.5, 5, "one"),
      (3.6, "b", 1.6, 6, null),
      (3.7, "c", 1.7, 10, null)
    ).map(Row.fromTuple)
    val joinSchema = StructType(schemaL.fields ++ Seq(
      StructField(colsR(2), StringType)
    ))
    val edf = createDataFrame(joinRows, joinSchema)

    (ldf, rdf, edf, joinColumns)
  }

  def differentTypesFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "column1"
    val joinColumns = Set(column1)

    // Left dataframe
    val colsL = Vector(column1, "column2", "column3", "column4")
    val rowsL = Seq(
      (1.5, 2.5, "a", 3.5),
      (1.6, 2.6, "b", 3.6),
      (1.7, 2.7, "c", 3.7)
    ).map(Row.fromTuple)
    val schemaL = StructType(Seq(
      StructField(colsL(0), DoubleType),
      StructField(colsL(1), DoubleType),
      StructField(colsL(2), StringType),
      StructField(colsL(3), DoubleType)
    ))
    val ldf = createDataFrame(rowsL, schemaL)

    // Right dataframe
    val colsR = Vector(column1, "column22")
    val rowsR = Seq(
      ("1.5", 0.5),
      ("1.6", 0.6),
      ("1.7", 0.7)
    ).map(Row.fromTuple)
    val schemaR = StructType(Seq(
      StructField(colsR(0), StringType),
      StructField(colsR(1), DoubleType)
    ))
    val rdf = createDataFrame(rowsR, schemaR)

    val ignored = DataFrame(None)

    (ldf, rdf, ignored, joinColumns)
  }

  def joinColumnsIsEmptyFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val joinColumns = Set.empty[String]

    // Left dataframe
    val colsL = Vector("column1", "column2", "column3", "column4")
    val rowsL = Seq(
      (1.5, 2.5, "a", 3.5),
      (1.6, 2.6, "b", 3.6),
      (1.7, 2.7, "c", 3.7)
    ).map(Row.fromTuple)
    val schemaL = StructType(Seq(
      StructField(colsL(0), DoubleType),
      StructField(colsL(1), DoubleType),
      StructField(colsL(2), StringType),
      StructField(colsL(3), DoubleType)
    ))
    val ldf = createDataFrame(rowsL, schemaL)

    // Right dataframe
    val colsR = Vector("column1", "column22")
    val rowsR = Seq(
      ("1.5", 0.5),
      ("1.6", 0.6),
      ("1.7", 0.7)
    ).map(Row.fromTuple)
    val schemaR = StructType(Seq(
      StructField(colsR(0), StringType),
      StructField(colsR(1), DoubleType)
    ))
    val rdf = createDataFrame(rowsR, schemaR)

    val ignored = DataFrame(None)

    (ldf, rdf, ignored, joinColumns)
  }

  def noSomeRightValuesFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "column1"
    val joinColumns = Set(column1)

    // Left dataframe
    val colsL = Vector(column1)
    val rowsL = Seq(
      1.5,
      1.6,
      1.7
    ).map(Seq(_)).map(Row.fromSeq)
    val schemaL = StructType(Seq(
      StructField(colsL(0), DoubleType)
    ))
    val ldf = createDataFrame(rowsL, schemaL)

    // Right dataframe
    val colsR = Vector(column1, "column2")
    val rowsR = Seq(
      (1.4, 0.5),
      (1.7, 2.7)
    ).map(Row.fromTuple)
    val schemaR = StructType(Seq(
      StructField(colsR(0), DoubleType),
      StructField(colsR(1), DoubleType)
    ))
    val rdf = createDataFrame(rowsR, schemaR)

    // join dataframe
    val joinRows = Seq(
      (1.5, null),
      (1.6, null),
      (1.7, 2.7)
    ).map(Row.fromTuple)
    val joinSchema = StructType(schemaL.fields ++ schemaR.fields.filterNot(_.name == column1))
    val edf = createDataFrame(joinRows, joinSchema)

    (ldf, rdf, edf, joinColumns)
  }

  def noMatchingValuesFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "column1"
    val joinColumns = Set(column1)

    // Left dataframe
    val colsL = Vector(column1)
    val schemaL = StructType(Seq(
      StructField(column1, DoubleType)
    ))
    val rowsL = Seq(
      1.5,
      1.6,
      1.7
    ).map(Seq(_)).map(Row.fromSeq)
    val ldf = createDataFrame(rowsL, schemaL)

    // Right dataframe
    val colsR = Vector(column1, "column2")
    val schemaR = StructType(Seq(
      StructField(colsR(0), DoubleType),
      StructField(colsR(1), DoubleType)
    ))
    val rowsR = Seq(
      (1.4, 0.5),
      (1.8, 1.7)
    ).map(Row.fromTuple)
    val rdf = createDataFrame(rowsR, schemaR)

    // join dataframe
    val joinRows = Seq(
      (1.5, null),
      (1.6, null),
      (1.7, null)
    ).map(Row.fromTuple)
    val joinSchema = StructType(schemaL.fields ++ schemaR.fields.tail)
    val edf = createDataFrame(joinRows, joinSchema)

    (ldf, rdf, edf, joinColumns)
  }

  def nullFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "column1"
    val joinColumns = Set(column1)

    // Left dataframe
    val colsL = Vector(column1)
    val rowsL = Seq(
      1.5,
      1.6,
      null
    ).map(Seq(_)).map(Row.fromSeq)
    val schemaL = StructType(Seq(
      StructField(colsL(0), DoubleType)
    ))
    val ldf = createDataFrame(rowsL, schemaL)

    // Right dataframe
    val colsR = Vector(column1, "column2")
    val rowsR = Seq(
      (null, 0.5),
      (null, 0.7),
      (1.6, null)
    ).map(Row.fromTuple)
    val schemaR = StructType(Seq(
      StructField(colsR(0), DoubleType),
      StructField(colsR(1), DoubleType)
    ))
    val rdf = createDataFrame(rowsR, schemaR)

    // join dataframe
    val joinRows = Seq(
      (1.5, null),
      (1.6, null)
    ).map(Row.fromTuple)
    val joinSchema = StructType(schemaL.fields ++ schemaR.fields.filterNot(_.name == column1))
    val edf = createDataFrame(joinRows, joinSchema)

    (ldf, rdf, edf, joinColumns)
  }

  def categoricalFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "categorical"
    val joinColumns = Set(column1)

    // Left dataframe
    val colsL = Vector(column1, "age")
    val schemaL = StructType(Seq(
      StructField(colsL(0), StringType),
      StructField(colsL(1), LongType)
    ))
    val rowsL = Seq(
      ("pies", 3),
      ("kot", 5),
      ("krowa", 7),
      ("pies", 1)
    ).map(Row.fromTuple)
    val ldf = createDataFrame(rowsL, schemaL)
    val ldfCategorized =
      CategoricalMapper(ldf, executionContext.dataFrameBuilder).categorized(column1)

    // Right dataframe
    val colsR = Vector(column1, "owner")
    val schemaR = StructType(Seq(
      StructField(colsL(0), StringType),
      StructField(colsR(1), DoubleType)
    ))
    val rowsR = Seq(
      ("kot", "Wojtek"),
      ("wiewiorka", "Jacek"),
      ("pies", "Rafal")
    ).map(Row.fromTuple)
    val rdf = createDataFrame(rowsR, schemaR)
    val rdfCategorized =
      CategoricalMapper(rdf, executionContext.dataFrameBuilder).categorized(column1)

    // join dataframe
    val joinRows = Seq(
      ("pies", 3, "Rafal"),
      ("kot", 5, "Wojtek"),
      ("krowa", 7, null),
      ("pies", 1, "Rafal")
    ).map(Row.fromTuple)
    val joinSchema = StructType(schemaL.fields ++ schemaR.fields.filterNot(_.name == column1))
    val edf = createDataFrame(joinRows, joinSchema)

    (ldfCategorized, rdfCategorized, edf, joinColumns)
  }

  def nullValuesInLeftDataFrameFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "nulls"
    val joinColumns = Set(column1)

    // Left dataframe
    val colsL = Vector(column1)
    val schemaL = StructType(Seq(
      StructField(colsL(0), StringType)
    ))
    val rowsL = Seq(
      null
    ).map(Seq(_)).map(Row.fromSeq)
    val ldf = createDataFrame(rowsL, schemaL)
    val ldfCategorized =
      CategoricalMapper(ldf, executionContext.dataFrameBuilder).categorized(column1)

    // Right dataframe
    val colsR = Vector(column1, "owner")
    val schemaR = StructType(Seq(
      StructField(colsL(0), StringType),
      StructField(colsR(1), DoubleType)
    ))
    val rowsR = Seq(
      ("kot", "Wojtek"),
      ("wiewiorka", "Jacek"),
      ("pies", "Rafal")
    ).map(Row.fromTuple)
    val rdf = createDataFrame(rowsR, schemaR)
    val rdfCategorized =
      CategoricalMapper(rdf, executionContext.dataFrameBuilder).categorized(column1)

    // join dataframe
    val joinRows = Seq.empty[Row]
    val joinSchema = StructType(schemaL.fields ++ schemaR.fields.filterNot(_.name == column1))
    val edf = createDataFrame(joinRows, joinSchema)

    (ldfCategorized, rdfCategorized, edf, joinColumns)
  }

  def sameColumnNamesFixture(): (DataFrame, DataFrame, DataFrame, Set[String]) = {
    val column1 = "nulls"
    val joinColumns = Set(column1)

    val sameNameColumns = Seq(
      ("A", StringType),
      ("B", LongType)
    )

    object Gen {
      def generate(dt: DataType) = dt match {
        case StringType => "s"
        case LongType => 1L
      }
    }
    import Gen._

    // Left dataframe
    val colsL = Vector(column1) ++ sameNameColumns.map { case (name, _) => name }
    val schemaL = StructType(Seq(
      StructField(colsL(0), StringType)
    ) ++ sameNameColumns.map { case (name, tpe) => StructField(name, tpe) })
    val rowsL = Seq(
      null +: sameNameColumns.map { case (_, t) => generate(t) }
    ).map(Row.fromSeq)
    val ldf = createDataFrame(rowsL, schemaL)

    // Right dataframe
    val colsR = Vector(column1, "owner") ++ sameNameColumns.map { case (name, _) => name }
    val schemaR = StructType(Seq(
      StructField(colsL(0), StringType),
      StructField(colsR(1), DoubleType)
    ) ++ sameNameColumns.map { case (name, tpe) => StructField(name, tpe) })
    val rowsR = Seq(
      Seq("kot", "Wojtek") ++ sameNameColumns.map { case (_, t) => generate(t) },
      Seq("wiewiorka", "Jacek") ++ sameNameColumns.map { case (_, t) => generate(t) },
      Seq("pies", "Rafal") ++ sameNameColumns.map { case (_, t) => generate(t) }
    ).map(Row.fromSeq)
    val rdf = createDataFrame(rowsR, schemaR)

    // join dataframe
    val joinRows = Seq.empty[Row]
    val joinSchema = StructType(schemaL.fields ++
      schemaR.fields.filterNot(_.name == column1).map {
        case s@StructField(name, dataType, _, _) if sameNameColumns.contains((name, dataType)) =>
          s.copy(name = name + "_1")
        case s => s
      }
    )
    val edf = createDataFrame(joinRows, joinSchema)

    (ldf, rdf, edf, joinColumns)
  }

  private def joinWithMultipleColumnSelection(names: Set[String], ids: Set[Int]): Join = {
    val operation = new Join
    val valueParam = operation.parameters.getColumnSelectorParameter(operation.joinColumns)
    valueParam.value = Some(MultipleColumnSelection(Vector(
      NameColumnSelection(names),
      IndexColumnSelection(ids)
    )))
    operation
  }
}