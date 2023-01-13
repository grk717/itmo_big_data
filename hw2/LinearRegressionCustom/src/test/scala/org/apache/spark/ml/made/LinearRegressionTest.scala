package org.apache.spark.ml.made

import com.google.common.io.Files
import org.scalatest._
import flatspec._
import matchers._
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.DataFrame

import scala.util.Random


class LinearRegressionTest extends AnyFlatSpec with should.Matchers with WithSpark {

  val paramsDelta = 0.001
  val mseDelta = 0.0001
  val delta = 0.0000001
  lazy val data: DataFrame = LinearRegressionTest._data

  "Estimator" should "get correct params" in {
    val lr = new LinearRegression()
      .setFeaturesCol("features")
      .setLabelCol("label")
      .setPredictionCol("prediction")
      .setLr(0.5)
      .setIters(2000)

    val model = lr.fit(data)
    val params = model.getWeights

    params(0) should be(2d +- paramsDelta)
    params(1) should be(3d +- paramsDelta)
    params(2) should be(4d +- paramsDelta)
    params(3) should be(1d +- paramsDelta)
  }

  "Model" should "predict data correctly" in {
    val lr = new LinearRegression()
      .setFeaturesCol("features")
      .setLabelCol("label")
      .setPredictionCol("prediction")
      .setLr(0.5)
      .setIters(2000)

    val model = lr.fit(data)
    validateModel(model, data)
  }

  "Estimator" should "work after re-read" in {

    val pipeline = new Pipeline().setStages(Array(
      new LinearRegression()
        .setFeaturesCol("features")
        .setLabelCol("label")
        .setPredictionCol("prediction")
        .setLr(0.5)
        .setIters(2000)
    ))

    val tmpFolder = Files.createTempDir()

    pipeline.write.overwrite().save(tmpFolder.getAbsolutePath)

    val model = Pipeline
      .load(tmpFolder.getAbsolutePath)
      .fit(data)
      .stages(0)
      .asInstanceOf[LinearRegressionModel]

    validateModel(model, data)
  }


  "Model" should "work after re-read" in {

    val pipeline = new Pipeline().setStages(Array(
      new LinearRegression()
        .setFeaturesCol("features")
        .setLabelCol("label")
        .setPredictionCol("prediction")
        .setLr(0.5)
        .setIters(2000)
    ))

    val model = pipeline.fit(data)
    val tmpFolder = Files.createTempDir()
    model.write.overwrite().save(tmpFolder.getAbsolutePath)

    val reRead = PipelineModel.load(tmpFolder.getAbsolutePath)

    validateModel(reRead.stages(0).asInstanceOf[LinearRegressionModel], data)
  }

  private def validateModel(model: LinearRegressionModel, data: DataFrame): Unit = {
    val df_result = model.transform(data)

    val evaluator = new RegressionEvaluator()
      .setLabelCol("label")
      .setPredictionCol("prediction")
      .setMetricName("mse")

    val mse = evaluator.evaluate(df_result)
    mse should be < mseDelta
  }
}

object LinearRegressionTest extends WithSpark {

  lazy val _x = (1 to 1000).map(x => Vectors.dense(Random.nextDouble(),
                                                         Random.nextDouble(),
                                                         Random.nextDouble()))
  lazy val _y = _x.map(v => 2*v(0) + 3*v(1) + 4*v(2) + 1)
  lazy val zipped = _x zip _y
  lazy val _data: DataFrame = {
    import sqlc.implicits._
    zipped.toDF("features", "label")
  }

}