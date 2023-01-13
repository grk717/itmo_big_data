package org.apache.spark.ml.made

import breeze.linalg.{norm, sum, DenseVector => BDV}
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.regression.{RegressionModel, Regressor}
import org.apache.spark.ml.param.{DoubleParam, IntParam, ParamMap}
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsReader, DefaultParamsWritable, DefaultParamsWriter, Identifiable, MLReadable, MLReader, MLWritable, MLWriter, MetadataUtils}
import org.apache.spark.ml.PredictorParams
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.mllib
import org.apache.spark.mllib.stat.MultivariateOnlineSummarizer
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{Dataset, Encoder}



trait LinearRegressionParams extends PredictorParams {
  // learning rate param
  final val lr: DoubleParam = new DoubleParam(this, "lr", "learning rate")
  setDefault(lr, 1.0)
  def setLr(value: Double): this.type = set(lr, value)

  // number of iterations param
  final val nIter: IntParam = new IntParam(this, "nIter", "number of iterations")
  setDefault(nIter, 100)
  def setIters(value: Int): this.type = set(nIter, value)

  // validateAndTransformSchema is implemented in PredictorParams trait
}


class LinearRegression(override val uid: String)
  extends Regressor[Vector, LinearRegression, LinearRegressionModel] with LinearRegressionParams with DefaultParamsWritable {

  def this() = this(Identifiable.randomUID("linearRegression"))

  override def copy(extra: ParamMap): LinearRegression = defaultCopy(extra)

  // override train instead of fir (because we are Regressor's child)
  override def train(dataset: Dataset[_]): LinearRegressionModel = {
    // no need to worry about schema, copying params - it's all done in "fit method", which wraps this one
    // to resolve what's in dataset (we expect vectors)
    implicit val encoder: Encoder[Vector] = ExpressionEncoder()

    // add column of ones (intercept/bias) and assemble features / label into one vector
    val withIntercept = dataset.withColumn("intercept", lit(1))
    val assembler = new VectorAssembler()
      .setInputCols(Array($(featuresCol), "intercept", $(labelCol)))
      .setOutputCol("trainData")
    val trainData: Dataset[Vector] = assembler
      .transform(withIntercept)
      .select(col = "trainData")
      .as[Vector]

    // get features amount w/o interception size
    val numCols = MetadataUtils.getNumFeatures(dataset, $(featuresCol))
    // create weights vector
    var weights : BDV[Double] = BDV.ones(numCols + 1)
    var converged = false
    var i = 0
    // perform iterations
    while (!converged & i < $(nIter)) {
      // get sum of
      val summary = trainData.rdd.mapPartitions((data: Iterator[Vector]) => {
        val summarizer = new MultivariateOnlineSummarizer()
        // vectors of
        data.foreach(v => {
          val X = v.asBreeze(0 until weights.size).toDenseVector
          val y = v.asBreeze(-1)
          // gradient values
          val pred = sum(X * weights)
          val residuals = pred - y
          val dw = (X * residuals)
          summarizer.add(mllib.linalg.Vectors.fromBreeze(dw))
        })
        Iterator(summarizer)
      }).reduce(_ merge _)
      // update weights
      weights -= ($(lr) / numCols) * summary.mean.asBreeze
      // check if changes in gradient are too small
      converged = norm(($(lr) / numCols)) < 0.000001
      i += 1
    }
    // return model
    val params = Vectors.fromBreeze(weights)
    copyValues(new LinearRegressionModel(params).setParent(this))
  }
}


object LinearRegression extends DefaultParamsReadable[LinearRegression]


class LinearRegressionModel protected[made](override val uid: String, weights: Vector)
  extends RegressionModel[Vector, LinearRegressionModel] with PredictorParams with MLWritable {

  def this(weights: Vector) = this(Identifiable.randomUID("linearRegressionModel"), weights)

  // transform method calls predict method for all the features in df
  // so I suppose we can do like this:
  override def predict(features: Vector): Double = {
    val x: BDV[Double] = BDV.vertcat(features.asBreeze.toDenseVector, BDV(1.0))
    sum(weights.asBreeze.toDenseVector * x)
  }

  override def copy(extra: ParamMap): LinearRegressionModel = copyValues(new LinearRegressionModel(weights))

  def getWeights: BDV[Double] = {
    weights.asBreeze.toDenseVector
  }

  override def write: MLWriter = new DefaultParamsWriter(this) {
    override protected def saveImpl(path: String): Unit = {
      super.saveImpl(path)

      val params = Tuple1(weights.asInstanceOf[Vector])

      sqlContext
        .createDataFrame(Seq(params))
        .write
        .parquet(path + "/vectors")
    }
  }
}


object LinearRegressionModel extends MLReadable[LinearRegressionModel] {
  override def read: MLReader[LinearRegressionModel] = new MLReader[LinearRegressionModel] {
    override def load(path: String): LinearRegressionModel = {
      val metadata = DefaultParamsReader.loadMetadata(path, sc)

      val vectors = sqlContext
        .read
        .parquet(path + "/vectors")

      // to resolve what's in dataset (we expect vectors)
      implicit val encoder: Encoder[Vector] = ExpressionEncoder()

      val (params) = vectors
        .select(vectors("_1").as[Vector])
        .first()

      val model = new LinearRegressionModel(params)
      metadata.getAndSetParams(model)

      model
    }
  }
}