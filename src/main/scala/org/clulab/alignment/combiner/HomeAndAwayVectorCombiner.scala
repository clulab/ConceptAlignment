package org.clulab.alignment.combiner

class HomeAndAwayVectorCombiner(val homeWeight: Float, val awayWeight: Float,
    val conceptWeight: Float, val conceptPropertyWeight: Float, val processWeight: Float, val processPropertyWeight: Float) {
  require(homeWeight > 0)
  require(conceptWeight > 0)

  val homeAwaySum: Float = homeWeight + awayWeight
  val homeFactor: Float = homeWeight / homeAwaySum
  // This will be further divided by the number of away values so that they don't overwhelm the home value.
  val awayFactor: Float = awayWeight / homeAwaySum
  val weightSum: Float = conceptWeight + conceptPropertyWeight + processWeight + processPropertyWeight

  def norm(array: Array[Float]): Array[Float] = {
    val len = mul(array, array).sum
    val result =
      if (len == 0f) array
      else mul(array, math.sqrt(1f / len).toFloat)

    result
  }

  def mul(array: Array[Float], factor: Float): Array[Float] = array.map(_ * factor)

  def mulOpt(arrayOpt: Option[Array[Float]], factor: Float): Option[Array[Float]] =
    arrayOpt.map(array => mul(array, factor))

  def mul(left: Array[Float], right: Array[Float]): Array[Float] =
    left.zip(right).map {
      case (left, right) => left * right
    }

  def add(left: Array[Float], right: Array[Float]): Array[Float] =
    left.zip(right).map {
      case (left, right) => left + right
    }

  def addOpt(left: Array[Float], rightOpt: Option[Array[Float]]): Array[Float] = {
    rightOpt.map { right =>
      left.zip(right).map {
        case (left, right) => left + right
      }
    }.getOrElse(left)
  }

  def combine(conceptVector: Array[Float], conceptPropertyVectorOpt: Option[Array[Float]],
              processVectorOpt: Option[Array[Float]], processPropertyVectorOpt: Option[Array[Float]]): Array[Float] = {
    val weightSum1 = conceptWeight
    val weightSum2 = weightSum1 + conceptPropertyVectorOpt.map(_ => conceptPropertyWeight).getOrElse(0f)
    val weightSum3 = weightSum2 +         processVectorOpt.map(_ =>         processWeight).getOrElse(0f)
    val weightSum4 = weightSum3 + processPropertyVectorOpt.map(_ => processPropertyWeight).getOrElse(0f)
    val weightSum = weightSum4

    val step1 = mul(conceptVector, conceptWeight / weightSum)
    val step2 = addOpt(step1, mulOpt(conceptPropertyVectorOpt, conceptPropertyWeight / weightSum))
    val step3 = addOpt(step2, mulOpt(        processVectorOpt,         processWeight / weightSum))
    val step4 = addOpt(step3, mulOpt(processPropertyVectorOpt, processPropertyWeight / weightSum))
    val combined = step4
    val normalized = norm(combined)

    normalized
  }

  def combine(homeVector: Array[Float], awayVectors: Array[Array[Float]]): Array[Float] = {
    if (awayVectors.nonEmpty) {
      val totalAwayVector = awayVectors.tail.foldLeft(awayVectors.head) { (sum, score) => add(sum, score) }
      val awayVector = mul(totalAwayVector, 1f / awayVectors.length)
      val homeAndAwayVector = add(mul(homeVector, homeFactor), mul(awayVector, awayFactor))
      val normalized = norm(homeAndAwayVector)

      normalized
    }
    else
      homeVector
  }
}
