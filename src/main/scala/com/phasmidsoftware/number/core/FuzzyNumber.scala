package com.phasmidsoftware.number.core

import com.phasmidsoftware.number.core.Number.prepareWithSpecialize
import scala.util.Left

/**
  * This class is designed to model a fuzzy Number.
  * See Number for more details on the actual representation.
  *
  * TODO implement scale, atan.
  * TODO FuzzyNumber should be the "norm." ExactNumber is just a fuzzy number with None for fuzz.
  * TODO ensure that every Double calculation contributes fuzziness.
  *
  * @param value  the value of the Number, expressed as a nested Either type.
  * @param factor the scale factor of the Number: valid scales are: Scalar, Pi, and E.
  * @param fuzz   the fuzziness of this Number.
  */
case class FuzzyNumber(override val value: Value, override val factor: Factor, fuzz: Option[Fuzz[Double]]) extends Number(value, factor) with Fuzzy[Double] {

  /**
    * Auxiliary constructor for an exact number.
    *
    * @param v    the value for the new Number.
    * @param fuzz the fuzz for the new Number.
    */
  def this(v: Value, fuzz: Option[Fuzz[Double]]) = this(v, Scalar, fuzz)

  /**
    * Action to render this ExactNumber as a String.
    *
    * @return a String.
    */
  def render: String = toString

  /**
    * Action to materialize this Expression.
    *
    * @return this ExactNumber.
    */
  def materialize: Number = this

  /**
    * Add a Number to this FuzzyNumber.
    *
    * @param x the addend.
    * @return the sum.
    */
  override def add(x: Number): Number = FuzzyNumber.plus(this, x)

  /**
    * Multiply a Number by this FuzzyNumber.
    *
    * @param x the multiplicand.
    * @return the product.
    */
  override def multiply(x: Number): Number = FuzzyNumber.times(this, x)

  /**
    * Yields the square root of this FuzzyNumber.
    * If possible, the result will be exact.
    */
  override def sqrt: Number = FuzzyNumber.sqrt(this)

  /**
    * Method to determine the sine of this Number.
    * The result will be a Number with Scalar factor.
    */
  override def sin: Number = FuzzyNumber.sin(this)

  /**
    * Raise this Number to the power p.
    * CONSIDER inlining this method.
    *
    * @param p a Number.
    * @return this Number raised to power p.
    */
  override def power(p: Number): Number = FuzzyNumber.power(this, p)

  /**
    * Method to create a new version of this, but with factor f.
    * NOTE: the result will have the same absolute magnitude as this.
    * In other words,  in the case where f is not factor, the numerical value of the result's value will be different
    * from this value.
    *
    * FIXME: this does not work properly for FuzzyNumbers except Pi-related conversions with relative fuzz.
    *
    * @param f the new factor for the result.
    * @return a Number based on this and factor.
    */
  override def scale(f: Factor): Number = super.scale(f)

  /**
    * @return true if this Number is equivalent to zero with at least 50% confidence.
    */
  override lazy val isZero: Boolean = isProbablyZero(0.5)

  /**
    * @param p the confidence desired.
    * @return true if this Number is equivalent to zero with at least p confidence.
    */
  def isProbablyZero(p: Double): Boolean = super.isZero || (for (f <- fuzz; x <- toDouble) yield f.normalizeShape.likely(p) > math.abs(x)).getOrElse(false)

  /**
    * Method to determine the sense of this number: negative, zero, or positive.
    * If this FuzzyNumber cannot be distinguished from zero with better than evens confidence, then
    *
    * @return an Int which is negative, zero, or positive according to the magnitude of this.
    */
  override lazy val signum: Int = signum(0.5)

  /**
    * Method to determine the sense of this number: negative, zero, or positive.
    * If this FuzzyNumber cannot be distinguished from zero with p confidence, then
    *
    * @param p the confidence desired.
    * @return an Int which is negative, zero, or positive according to the magnitude of this.
    */
  def signum(p: Double): Int = if (isProbablyZero(p)) 0 else super.signum

  /**
    * This method is invoked by power so do NOT invoke sqrt or power in implementations.
    *
    * NOTE: we do not add any extra degree of imprecision here because it's assumed that
    * any existing fuzziness will outweigh the double-precision-induced fuzziness.
    *
    * @return a Number which is the square toot of this, possibly fuzzy, Number.
    */
  def makeFuzzyIfAppropriate(f: Number => Number): Number = f(this).asInstanceOf[FuzzyNumber].addFuzz(RelativeFuzz[Double](DoublePrecisionTolerance, Box))

  /**
    * Make a copy of this FuzzyNumber but with additional fuzz given by f.
    *
    * @param f the additional fuzz.
    * @return this but with fuzziness which is the convolution of fuzz and f.
    */
  def addFuzz(f: Fuzz[Double]): Number = FuzzyNumber.addFuzz(this, f)

  /**
    * Make a copy of this Number, but with different fuzziness.
    *
    * @param z the optional Fuzz.
    * @return either a Fuzzy or Exact Number.
    */
  def makeFuzzy(z: Option[Fuzz[Double]]): FuzzyNumber = FuzzyNumber(value, factor, z)

  /**
    * Evaluate a dyadic operator on this and other, using either plus, times, ... according to the value of op.
    * NOTE: this and other must have been aligned by type so that they have the same structure.
    *
    * @param other        the other operand, a Number.
    * @param f            the factor to apply to the result.
    * @param op           the appropriate DyadicOperation.
    * @param absolute     true if the convolution of Fuzz values should be absolute (addition) vs. relative (multiplication).
    * @param coefficients an optional Tuple representing the coefficients to scale the fuzz values by.
    *                     For a power operation such as x to the power of y, these will be y/x and ln x respectively.
    *                     For addition or multiplication, they will be 1 and 1.
    * @return a new Number which is result of applying the appropriate function to the operands this and other.
    */
  def composeDyadicFuzzy(other: Number, f: Factor)(op: DyadicOperation, absolute: Boolean, independent: Boolean, coefficients: Option[(Double, Double)]): Option[Number] =
    for (n <- composeDyadic(other, f)(op); t1 <- this.toDouble; t2 <- other.toDouble) yield
      FuzzyNumber(n.value, n.factor, Fuzz.combine(t1, t2, !absolute, independent)(Fuzz.applyCoefficients((fuzz, other.fuzz), coefficients)))

  /**
    * Evaluate a monadic operator on this, using either negate or... according to the value of op.
    *
    * @param f  the factor to apply to the result.
    * @param op the appropriate MonadicOperation.
    * @return a new Number which is result of applying the appropriate function to the operand this.
    */
  def transformMonadicFuzzy(f: Factor)(op: MonadicOperation, fuzzOp: Double => Double, absolute: Boolean): Option[Number] = {
    transformMonadic(f)(op).flatMap {
      case n: FuzzyNumber =>
        for (x <- n.toDouble) yield n.makeFuzzy(Fuzz.map(x, !absolute, fuzzOp, fuzz))
    }
  }

  /**
    * Render this FuzzyNumber in String form, including the factor, and the fuzz.
    *
    * @return
    */
  override def toString: String = {
    val sb = new StringBuilder()
    val w = fuzz match {
      case Some(f) => f.toString(toDouble.getOrElse(0.0))
      case None => valueToString
    }
    sb.append(w)
    sb.append(factor.toString)
    sb.toString
  }

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Both the value and the factor will be changed.
    *
    * @param v the value.
    * @param f the factor.
    * @return a FuzzyNumber.
    */
  protected def make(v: Value, f: Factor): Number = make(v, f, fuzz)

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Both the value and the factor will be changed.
    * CONSIDER: not entirely sure we need this method.
    *
    * @param v the value.
    * @param f the factor.
    * @param z the new fuzziness.
    * @return a FuzzyNumber.
    */
  protected def make(v: Value, f: Factor, z: Option[Fuzz[Double]]): Number = FuzzyNumber(v, f, z)
}

object FuzzyNumber {

  /**
    * Get the fuzz coefficients for calculating the fuzz on a power operation.
    * According to the "Generalized Power Rule," these coefficients should be y/x and ln x, respectively,
    * where x is the magnitude of n and y is the magnitude of p.
    *
    * @param n the number to be raised to power p.
    * @param p the power (exponent) with which to raise n.
    * @return the value of n to the power of p.
    */
  private def getPowerCoefficients(n: Number, p: Number): Option[(Double, Double)] =
    for (z <- n.toDouble; q <- p.toDouble) yield (q / z, math.log(z))

  def power(number: FuzzyNumber, p: Number): Number = composeDyadic(number, p, p, DyadicOperationPower, absolute = false, independent = false, getPowerCoefficients(number, p))

  def apply(): Number = Number.apply()

  def sin(x: FuzzyNumber): Number = transformMonadic(x.scale(Pi), Scalar, MonadicOperationSin, x => math.cos(x), absolute = false)

  def sqrt(x: FuzzyNumber): Number = transformMonadic(x, Scalar, MonadicOperationSqrt, x => x / 2, absolute = false)

  def exp(x: FuzzyNumber): Number = transformMonadic(x.scale(E), Scalar, MonadicOperationExp, identity, absolute = false)

  private def plus(x: FuzzyNumber, y: Number): Number = {
    val (a, b) = x.alignFactors(y)
    val (p, q) = a.alignTypes(b)
    (p, q) match {
      case (n: FuzzyNumber, _) => composeDyadic(n, p, q, DyadicOperationPlus, absolute = true, independent = true, None)
      case (_, n: FuzzyNumber) => composeDyadic(n, q, p, DyadicOperationPlus, absolute = true, independent = true, None)
      case (_, _) => p add q
    }
  }

  private def times(x: FuzzyNumber, y: Number): Number = {
    val (a, b) = x.alignFactors(y)
    val (p, q) = a.alignTypes(b)
    (p, q) match {
      case (n: FuzzyNumber, _) => composeDyadic(n, p, q, DyadicOperationTimes, absolute = false, independent = x != y, None)
      case (_, n: FuzzyNumber) => composeDyadic(n, q, p, DyadicOperationTimes, absolute = false, independent = x != y, None)
      case (_, _) => p multiply q
    }
  }

  private def addFuzz(n: Number, f: Fuzz[Double]): Number = (n.value, n.fuzz) match {
    case (v@Left(Left(Some(_))), fo) => addFuzz(n, v, fo, f)
    case _ => n
  }

  private def addFuzz(number: Number, v: Value, fo: Option[Fuzz[Double]], fAdditional: Fuzz[Double]) = {
    val combinedFuzz = for (f <- fo.orElse(Some(AbsoluteFuzz(0.0, Box))); p <- number.toDouble; g <- Fuzz.combine(p, 0, f.style, independent = false)((fo, fAdditional.normalize(p, f.style)))) yield g
    FuzzyNumber(v, number.factor, combinedFuzz)
  }

  /**
    * Evaluate a dyadic operator, defined by op, on n and q.
    * Parameters absolute and power relate to the calculation of the error bounds of the result.
    * CONSIDER changing the definition of p.
    *
    * @param n            the first operand.
    * @param p            an operand whose only purpose is to provide a Factor value.
    * @param q            the second operand.
    * @param op           the dyadic operation.
    * @param absolute     true if the dyadic operation is addition, false if it's multiplication or power.
    * @param coefficients an optional Tuple representing the coefficients to scale the fuzz values by.
    *                     For a power operation such as x to the power of y, these will be y/x and ln x respectively.
    *                     For addition or multiplication, they will be 1 and 1.
    * @return a new Number which is the result of operating on n and q as described above.
    */
  private def composeDyadic(n: Number, p: Number, q: Number, op: DyadicOperation, absolute: Boolean, independent: Boolean, coefficients: Option[(Double, Double)]) = n match {
    case x: FuzzyNumber => prepareWithSpecialize(x.composeDyadicFuzzy(q, p.factor)(op, absolute, independent, coefficients))
    case _: Number => prepareWithSpecialize(n.composeDyadic(n, p.factor)(op))
  }

  private def transformMonadic(n: Number, factor: Factor, op: MonadicOperation, fuzzOp: Double => Double, absolute: Boolean) = n match {
    case x: FuzzyNumber => prepareWithSpecialize(x.transformMonadicFuzzy(factor)(op, fuzzOp, absolute))
    case _: Number => prepareWithSpecialize(n.transformMonadic(factor)(op))
  }

}

case class FuzzyNumberException(str: String) extends Exception(str)
