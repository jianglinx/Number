package com.phasmidsoftware.number.core

import java.util.NoSuchElementException

import com.phasmidsoftware.number.core.FP._
import com.phasmidsoftware.number.core.Number.{negate, prepare}
import com.phasmidsoftware.number.core.Rational.{RationalHelper, toInts}
import com.phasmidsoftware.number.core.Render.renderValue
import com.phasmidsoftware.number.core.Value._
import com.phasmidsoftware.number.parse.NumberParser

import scala.language.implicitConversions
import scala.math.BigInt
import scala.util._

/**
  * This class is designed to model an exact Numeral.
  * See Number for more details on the actual representation.
  *
  * TODO implement scientific notation by having factors such os 10^3, 10^6, etc. (alternatively, add a separate parameter)
  *
  * @param value  the value of the Number, expressed as a nested Either type.
  * @param factor the scale factor of the Number: valid scales are: Scalar, Pi, and E.
  */
case class ExactNumber(override val value: Value, override val factor: Factor) extends Number(value, factor) {

  /**
    * Auxiliary constructor for the usual situation with the default factor.
    *
    * @param v the value for the new Number.
    */
  def this(v: Value) = this(v, Scalar)

  /**
    * Method to get this fuzziness of this Number.
    *
    * @return None
    */
  def fuzz: Option[Fuzz[Double]] = None

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Both the value and the factor will be changed.
    *
    * @param v the value.
    * @param f the factor.
    * @return an ExactNumber.
    */
  def make(v: Value, f: Factor): Number = ExactNumber(v, f)

  /**
    * If the result of invoking f on this is a Double, then there will inevitably be some loss of precision.
    *
    * CONSIDER passing in the fuzziness value as it may differ for different functions.
    *
    * @return a Number which is the square toot of this, possibly fuzzy, Number.
    */
  def makeFuzzyIfAppropriate(f: Number => Number): Number = {
    val z: Number = f(this)
    z.value match {
      case v@Left(Left(Some(_))) => FuzzyNumber(v, z.factor, Some(RelativeFuzz(DoublePrecisionTolerance, Box)))
      case v => z.make(v)
    }
  }

  /**
    * Method to determine the sense of this number: negative, zero, or positive.
    * If this FuzzyNumber cannot be distinguished from zero with p confidence, then
    * the result will be zero.
    *
    * TEST me
    *
    * @param p the confidence desired (ignored).
    * @return an Int which is negative, zero, or positive according to the magnitude of this.
    */
  def signum(p: Double): Int = signum

  /**
    * Action to render this ExactNumber as a String.
    *
    * TEST me
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
}

/**
  * This class is designed to model a Numerical value of various possible different types.
  * These types are: Int, BigInt, Rational, Double.
  *
  * CONSIDER including the fuzziness in Number and simply having ExactNumber always have fuzz of None.
  *
  * @param value  the value of the Number, expressed as a nested Either type.
  * @param factor the scale factor of the Number: valid scales are: Scalar, Pi, and E.
  */
abstract class Number(val value: Value, val factor: Factor) extends Expression with Ordered[Number] {

  self =>

  /**
    * Auxiliary constructor for the usual situation with the default factor.
    *
    * @param v the value for the new Number.
    */
  def this(v: Value) = this(v, Scalar)

  /**
    * Numbers cannot (for now) be simplified.
    *
    * @return this.
    */
  def simplify: Expression = this

  /**
    * Method to determine if this is a valid Number.
    * An invalid number is of the has a value of form Left(Left(Left(None)))
    *
    * @return true if this is a valid Number
    */
  def isValid: Boolean = maybeDouble.isDefined

  /**
    * Method to get this fuzziness of this Number.
    *
    * @return for an Exact number, this will be None, otherwise the actual fuzz.
    */
  def fuzz: Option[Fuzz[Double]]

  /**
    * Method to get the value of this Number as an optional Double.
    *
    * @return an Some(Double) which is the closest possible value to the nominal value, otherwise None if this is invalid.
    */

  def toDouble: Option[Double] = maybeDouble

  /**
    * Method to get the value of this Number as a Rational.
    * If this is actually a Double, it will be converted to a Rational according to the implicit conversion from Double to Rational.
    * See Rational.convertDouble(x).
    *
    * @return an Option of Rational.
    */
  def toRational: Option[Rational] = maybeRational

  /**
    * Method to get the value of this Number as an Int.
    *
    * @return an Option of Int. If this Number cannot be converted to an Int, then None will be returned.
    */
  def toInt: Option[Int] = maybeInt

  /**
    * Add x to this Number and return the result.
    * See Number.plus for more detail.
    * CONSIDER inlining this method.
    *
    * @param x the addend.
    * @return the sum.
    */
  def add(x: Number): Number = Number.plus(this, x)

  /**
    * Subtract x from this Number and return the result.
    * See + and unary_ for more detail.
    * CONSIDER inlining this method.
    *
    * @param x the subtrahend.
    * @return the difference.
    */
  def subtract(x: Number): Number = this add -x

  /**
    * Change the sign of this Number.
    */
  lazy val unary_- : Number = Number.negate(this)

  /**
    * Multiply this Number by x and return the result.
    * See Number.times for more detail.
    * CONSIDER inlining this method.
    *
    * * @param x the multiplicand.
    * * @return the product.
    */
  def multiply(x: Number): Number = Number.times(this, x)

  /**
    * Eagerly multiply this Number by an Int.
    * CONSIDER inlining this method.
    *
    * TEST me
    *
    * @param x the scale factor (an Int).
    * @return this * x.
    */
  def multiply(x: Int): Number = Number.scale(this, x)

  /**
    * Divide this Number by x and return the result.
    * See * and invert for more detail.
    * CONSIDER inlining this method.
    *
    * @param x the divisor.
    * @return the quotient.
    */
  def divide(x: Number): Number = this multiply x.invert

  /**
    * Divide this Number by a scalar x and return the result.
    * CONSIDER inlining this method.
    *
    * @param x the divisor (an Int).
    * @return the quotient.
    */
  def divide(x: Int): Number = this divide Number(x)

  /**
    * Raise this Number to the power p.
    * CONSIDER inlining this method.
    *
    * @param p a Number.
    * @return this Number raised to power p.
    */
  def power(p: Number): Number = Number.power(this, p)

  /**
    * Raise this Number to the power p.
    * CONSIDER inlining this method.
    *
    * @param p an integer.
    * @return this Number raised to power p.
    */
  def power(p: Int): Number = Number.power(this, p)

  /**
    * Yields the inverse of this Number.
    * This Number is first normalized so that its factor is Scalar, since we cannot directly invert Numbers with other
    * factors.
    */
  def invert: Number = Number.inverse(normalize)

  /**
    * Yields the square root of this Number.
    * If possible, the result will be exact.
    */
  def sqrt: Number = Number.power(this, Number(r"1/2"))

  /**
    * Method to determine the sine of this Number.
    * The result will be a Number with Scalar factor.
    *
    * @return the sine of this.
    */
  def sin: Number = makeFuzzyIfAppropriate(Number.sin)

  /**
    * Method to determine the cosine of this Number.
    * The result will be a Number with Scalar factor.
    *
    * @return the cosine.
    */
  def cos: Number = negate(scale(Pi) subtract Number(Rational.half, Pi)).sin

  /**
    * The tangent of this Number.
    *
    * @return the tangent.
    */
  def tan: Number = sin divide cos

  /**
    * Calculate the angle whose opposite length is y and whose adjacent length is this.
    *
    * @param y the opposite length
    * @return the angle defined by x = this, y = y
    */
  def atan(y: Number): Number = makeFuzzyIfAppropriate(x => Number.atan(x, y))

  /**
    * Method to determine the sense of this number: negative, zero, or positive.
    *
    * @return an Int which is negative, zero, or positive according to the magnitude of this.
    */
  def signum: Int = Number.signum(this)

  /**
    * Method to determine the sense of this number: negative, zero, or positive.
    * If this FuzzyNumber cannot be distinguished from zero with p confidence, then
    *
    * @param p the confidence desired.
    * @return an Int which is negative, zero, or positive according to the magnitude of this.
    */
  def signum(p: Double): Int

  /**
    * Method to create a new version of this, but with factor f.
    * NOTE: the result will have the same absolute magnitude as this.
    * In other words,  in the case where f is not factor, the numerical value of the result's value will be different
    * from this value.
    *
    * @param f the new factor for the result.
    * @return a Number based on this and factor.
    */
  def scale(f: Factor): Number = makeFuzzyIfAppropriate(x => Number.scale(x, f))

  /**
    * @return true if this Number is equal to zero.
    */
  def isZero: Boolean = Number.isZero(this)

  /**
    * @return true if this Number is equal to zero.
    */
  def isInfinite: Boolean = Number.isInfinite(this)

  /**
    * Method to compare this with another Number.
    * The difference between this method and that of ExactNumber is that the signum method is implemented differently.
    *
    * @param other the other Number.
    * @return -1, 0, or 1 according to whether x is <, =, or > y.
    */
  override def compare(other: Number): Int = Number.compare(this, other)

  /**
    * Perform a fuzzy comparison where we only require p confidence to know that this and other are effectively the same.
    *
    * @param other the Number to be compared with.
    * @param p     the confidence expressed as a fraction of 1 (0.5 would be a typical value).
    * @return -1, 0, 1 as usual.
    */
  def fuzzyCompare(other: Number, p: Double): Int = Number.fuzzyCompare(this, other, p)

  /**
    * Be careful when implementing this method that you do not invoke a method recursively.
    *
    * @return a Number which is the the result, possibly fuzzy, of invoking f on this.
    */
  def makeFuzzyIfAppropriate(f: Number => Number): Number

  /**
    * Evaluate a dyadic operator on this and other, using either plus, times, ... according to the value of op.
    * NOTE: this and other must have been aligned by type so that they have the same structure.
    *
    * @param other the other operand, a Number.
    * @param f     the factor to apply to the result.
    * @param op    the appropriate DyadicOperation.
    * @return a new Number which is result of applying the appropriate function to the operands this and other.
    */
  def composeDyadic(other: Number, f: Factor)(op: DyadicOperation): Option[Number] = doComposeDyadic(other, f)(op.getFunctions)

  /**
    * Evaluate a monadic operator on this.
    *
    * @param f  the factor to apply to the result.
    * @param op the appropriate MonadicOperation.
    * @return a new Number which is result of applying the appropriate function to the operand this.
    */
  def transformMonadic(f: Factor)(op: MonadicOperation): Option[Number] = doTransformMonadic(f)(op.getFunctions)

  /**
    * Evaluate a query operator on this.
    *
    * @param op the appropriate QueryOperation.
    * @return a Boolean.
    */
  def query(op: QueryOperation): Boolean = doQuery(op.getFunctions).getOrElse(false)

  /**
    * Render this Number in String form, including the factor.
    *
    * @return
    */
  override def toString: String = {
    val sb = new StringBuilder()
    sb.append(valueToString)
    sb.append(factor.toString)
    sb.toString
  }

  protected def valueToString: String = renderValue(value) match {
    case (x, true) => x
    case (x, false) => x + "..."
  }

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Both the value and the factor will be changed.
    *
    * @param v the value.
    * @param f the factor.
    * @return either a Fuzzy or Exact Number.
    */
  protected def make(v: Value, f: Factor): Number

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Only the factor will change.
    * This method does not need to be followed by a call to specialize.
    *
    * TEST me
    *
    * @param f the factor.
    * @return either a Fuzzy or Exact Number.
    */
  protected def make(f: Factor): Number = make(value, f)

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Only the value will change.
    * This method should be followed by a call to specialize.
    *
    * @param v the value.
    * @return either a Fuzzy or Exact Number.
    */
  def make(v: Value): Number = make(v, factor)

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Only the value will change.
    * This method should be followed by a call to specialize.
    *
    * @param v the value.
    * @param f Factor.
    * @return either a Fuzzy or Exact Number.
    */
  protected def make(v: Int, f: Factor): Number = make(Value.fromInt(v), f)

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Only the value will change.
    * This method should be followed by a call to specialize.
    *
    * @param v the value.
    * @return either a Fuzzy or Exact Number.
    */
  protected def make(v: Int): Number = make(v, factor)

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Only the value and the factor will change.
    * This method should be followed by a call to specialize.
    *
    * @param r a Rational.
    * @param f Factor.
    * @return either a Fuzzy or Exact Number.
    */
  protected def make(r: Rational, f: Factor): Number = make(Value.fromRational(r), f)

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Only the value will change.
    * This method should be followed by a call to specialize.
    *
    * @param v the value.
    * @return either a Fuzzy or Exact Number.
    */
  protected def make(v: Rational): Number = make(v, factor)

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Only the value and factor will change.
    * This method should be followed by a call to specialize.
    *
    * @param v the value (a Double).
    * @param f Factor.
    * @return either a Fuzzy or Exact Number.
    */
  protected def make(v: Double, f: Factor): Number = makeFuzzyIfAppropriate(x => x.make(Value.fromDouble(Some(v)), f))

  /**
    * Make a copy of this Number, given the same degree of fuzziness as the original.
    * Only the value will change.
    * This method should be followed by a call to specialize.
    *
    * TEST me
    *
    * @param v the value (a Double).
    * @return either a Fuzzy or Exact Number.
    */
  protected def make(v: Double): Number = make(v, factor)

  /**
    * Method to "normalize" a number, that's to say make it a Scalar.
    *
    * @return a new Number with factor of Scalar but with the same magnitude as this.
    */
  def normalize: Number = factor match {
    case Scalar => this
      // TEST me
    case Pi | E => prepare(maybeDouble map (x => self.make(x * factor.value).specialize.make(Scalar)))
  }

  /**
    * Return a Number which uses the most restricted type possible.
    * A Number based on a Double will yield a Number based on a Rational (if the conversion is exact).
    * A Number based on a Rational will yield a Number based on a BigInt (if there is a unit denominator).
    * A Number based on a BigInt will yield a Number based on a Int (if it is sufficiently small).
    *
    * @return a Number with the same magnitude as this.
    */
  //protected
  lazy val specialize: Number = value match {
    // Int case
    case Right(_) => this
    // Rational case
    case Left(Right(r)) =>
      Try(r.toInt) match {
        case Success(b) => make(b).specialize
        case _ => this
      }
    // Double case
    case d@Left(Left(Some(x))) =>
      // NOTE: here we attempt to deal with Doubles.
      // If a double can be represented by a BigDecimal with scale 0, 1, or 2 then we treat it as exact.
      // Otherwise, we will give it appropriate fuzziness.
      // In general, if you wish to have more control over this, then define your input using a String.
      // CONSIDER will this handle numbers correctly which are not close to 1?
      val r = Rational(x)
      r.toBigDecimal.scale match {
        case 0 | 1 | 2 => make(r).specialize
        // CONSIDER in following line adding fuzz only if this Number is exact.
        case n => FuzzyNumber(d, factor, fuzz).addFuzz(AbsoluteFuzz(Fuzz.toDecimalPower(5, -n), Box))
      }
    // Invalid case
    case _ => this
  }

  /**
    * Method to align the factors of this and x such that the resulting Numbers (in the tuple) each have the same factor.
    *
    * @param x the Number to be aligned with this.
    * @return a tuple of two Numbers with the same factor.
    */
  //protected
  def alignFactors(x: Number): (Number, Number) = factor match {
    case Scalar => (this, x.scale(factor))
    case _ => (scale(x.factor), x)
  }

  /**
    * Method to align the types of this and x such that the resulting Numbers (in the tuple) each have the same structure.
    *
    * @param x the Number to be aligned with this.
    * @return a tuple of two Numbers, the first of which will be the more general type:
    *         (Invalid vs. Double, Double vs. Rational, Rational vs. BigInt, BigInt vs. Int).
    */
  //protected
  def alignTypes(x: Number): (Number, Number) = value match {
    // this is an invalid Number: return a pair of invalid numbers
    case Left(Left(None)) => (this, this)
    // this value is a real Number: convert x to a Number based on real.
    case Left(Left(Some(_))) => x.value match {
      // x's value is invalid: swap the order so the the first element is invalid
      case Left(Left(None)) => x.alignTypes(this)
      // otherwise: return this and x re-cast as a Double
      case _ => (this, prepare(x.maybeDouble.map(y => make(y, x.factor).specialize)))
    }
    // this value is a Rational:
    case Left(Right(_)) => x.value match {
      // x's value is a real Number: swap the order so that the first element is the real number
      case Left(Left(_)) => x.alignTypes(this)
      // otherwise: return this and x re-cast as a Rational
      case _ => (this, x.make(x.maybeRational.getOrElse(Rational.NaN), x.factor).specialize)
    }
    // this value is an Int:
    case Right(_) => x.value match {
      // x's value is a BigInt, Rational or real Number: swap the order so that the first element is the BigInt/Rational/real number
      case Left(_) => x.alignTypes(this)
      // otherwise: return this and x re-cast as an Int
      case _ => (this, x.make(x.maybeInt.getOrElse(0), x.factor).specialize)
    }
  }

  /**
    * Method to ensure that the value is within some factor-specific range.
    * In particular, Pi=based numbers are modulated to the range 0..2
    *
    * @return this or an equivalent Number.
    */
  def modulate: Number = Number.modulate(this)

  /**
    * Evaluate a dyadic operator on this and other, using the various functions passed in.
    * NOTE: this and other must have been aligned by type so that they have the same structure.
    *
    * @param other     the other operand, a Number.
    * @param f         the factor to apply to the result.
    * @param functions the tuple of four conversion functions.
    * @return a new Number which is result of applying the appropriate function to the operands this and other.
    */
  private def doComposeDyadic(other: Number, f: Factor)(functions: DyadicFunctions): Option[Number] = {
    val (fInt, fRational, fDouble) = functions

    def tryDouble(xo: Option[Double]): Try[Number] = xo match {
      case Some(n) => toTryWithThrowable(for (y <- other.maybeDouble) yield fDouble(n, y) map (x => make(x, f)), NumberException("other is not a Double")).flatten
      case None => Failure(NumberException("number is invalid"))
    }

    def tryConvert[X](x: X, msg: String)(extract: Number => Option[X], func: (X, X) => Try[X], g: (X, Factor) => Number): Try[Number] =
      toTryWithThrowable(for (y <- extract(other)) yield func(x, y) map (g(_, f)), NumberException(s"other is not a $msg")).flatten

    def tryRational(x: Rational): Try[Number] = tryConvert(x, "Rational")(n => n.maybeRational, fRational, make)

    def tryInt(x: Int): Try[Number] = tryConvert(x, "Int")(n => n.maybeInt, fInt, make)

    import Converters._
    val xToZy1: Either[Option[Double], Rational] => Try[Number] = y => tryMap(y)(tryRational, tryDouble)

    tryMap(value)(tryInt, xToZy1).toOption
  }

  /**
    * Evaluate a monadic operator on this, using the various functions passed in.
    *
    * @param f         the factor to be used for the result.
    * @param functions the tuple of four conversion functions.
    * @return a new Number which is result of applying the appropriate function to the operand this.
    */
  private def doTransformMonadic(f: Factor)(functions: MonadicFunctions): Option[Number] =
    Operations.doTransformValueMonadic(value)(functions) map (make(_, f))

  /**
    * Evaluate a query operator on this, using the various functions passed in.
    *
    * @param functions the tuple of four conversion functions.
    * @return a new Number which is result of applying the appropriate function to the operand this.
    */
  private def doQuery(functions: QueryFunctions): Option[Boolean] = Operations.doQuery(value, functions)

  /**
    * An optional Rational that corresponds to the value of this Number (but ignoring the factor).
    * A Double value is not converted to a Rational since, if it could be done exactly, it already would have been.
    * CONSIDER using MonadicTransformations
    */
  private lazy val maybeRational: Option[Rational] = {
    import Converters._
    val ry = tryMap(value)(tryF(Rational.apply), x => tryMap(x)(identityTry, fail("no Double=>Rational conversion")))
    ry.toOption
  }

  /**
    * An optional Double that corresponds to the value of this Number (but ignoring the factor).
    */
  private lazy val maybeDouble: Option[Double] = optionMap(value)(_.toDouble, x => optionMap(x)(_.toDouble, identity))

  /**
    * An optional Int that corresponds to the value of this Number (but ignoring the factor).
    *
    * CONSIDER using MonadicTransformations
    */
  private lazy val maybeInt: Option[Int] = {
    val xToZy0: Option[Double] => Try[Int] = {
      case Some(n) if Math.round(n) == n => if (n <= Int.MaxValue && n >= Int.MinValue) Try(n.toInt)
      else Failure(NumberException(s"double $n cannot be represented as an Int"))
      case Some(n) => Failure(NumberException(s"toInt: $n is not integral"))
      case None => Failure(new NoSuchElementException())
    }
    import Converters._
    val xToZy1: Either[Option[Double], Rational] => Try[Int] = y => tryMap(y)(tryF(y => y.toInt), xToZy0)
    tryMap(value)(identityTry, xToZy1).toOption
  }
}

object Number {
  /**
    * Exact value of 1
    */
  val zero: Number = ExactNumber(Right(0), Scalar)
  /**
    * Exact value of 1
    */
  val one: Number = ExactNumber(Right(1), Scalar)
  /**
    * Exact value of pi
    */
  val pi: Number = ExactNumber(Right(1), Pi)
  /**
    * Exact value of e
    */
  val e: Number = ExactNumber(Right(1), E)

  implicit class NumberOps(x: Int) {

    /**
      * Add this x (a Number) and yield a Number.
      *
      * @param y the addend, a Number.
      * @return a Number whose value is x + y.
      */
    def +(y: Number): Number = (Number(x) add y).materialize

    /**
      * Multiply x by y (a Number) and yield a Number.
      *
      * @param y the multiplicand, a Number.
      * @return a Number whose value is x * y.
      */
    def *(y: Number): Number = (Number(x) multiply y).materialize

    /**
      * Divide x by y (a Number) and yield a Number.
      *
      * @param y the divisor, a Number.
      * @return a Number whose value is x / y.
      */
    def /(y: Number): Number = (Number(x) / y).materialize

    /**
      * Divide x by y (a Number) and yield a Number.
      * NOTE: the colon is necessary in order to coerce the left hand operand to be a Number.
      *
      * @param y the divisor, an Int.
      * @return a Number whose value is x / y.
      */
    def :/(y: Int): Number = /(Number(y))
  }

  /**
    * Method to construct a new Number from value, factor and fuzz, according to whether there is any fuzziness.
    *
    * CONSIDER modulate the result so that, in the case of a multiple of Pi, we restrict the range to 0 to 2pi immediately.
    * However, note that this will change the behavior such that it is no longer possible to have the constant 2pi.
    *
    * @param value  the value of the Number, expressed as a nested Either type.
    * @param factor the scale factor of the Number: valid scales are: Scalar, Pi, and E.
    * @param fuzz   the fuzziness of this Number, wrapped in Option.
    * @return a Number.
    */
  def create(value: Value, factor: Factor, fuzz: Option[Fuzz[Double]]): Number = (fuzz match {
    case None => ExactNumber(value, factor)
    case _ => FuzzyNumber(value, factor, fuzz)
  }).specialize

  /**
    * Method to construct a new Number from value, factor and fuzz, according to whether there is any fuzziness.
    *
    * @param value  the value of the Number, expressed as a nested Either type.
    * @param factor the scale factor of the Number: valid scales are: Scalar, Pi, and E.
    * @return a Number.
    */
  def create(value: Value, factor: Factor): Number = create(value, factor, None)

  /**
    * Method to construct a new Number from value, factor and fuzz, according to whether there is any fuzziness.
    *
    * @param value      the value of the Number, expressed as a nested Either type.
    * @param actualFuzz the fuzziness of this Number.
    * @return a Number.
    */
  def create(value: Value, actualFuzz: Fuzz[Double]): Number = create(value, Scalar, Some(actualFuzz))

  /**
    * Method to construct a new Number from value, factor and fuzz, according to whether there is any fuzziness.
    *
    * @param value the value of the Number, expressed as a nested Either type.
    * @return a Number.
    */
  def create(value: Value): Number = create(value, Scalar)

  /**
    * Method to construct a Number from a String.
    * This is by far the best way of creating the number that you really want.
    *
    * @param x the String representation of the value.
    * @return a Number based on x.
    */
  def apply(x: String): Number = parse(x) match {
    // CONSIDER we should perhaps process n (e.g. to modulate a Pi value)
    case Success(n) => n
    case Failure(e) => throw NumberExceptionWithCause(s"apply(String, Factor): unable to parse $x", e)
  }

  /**
    * Method to construct a Number from an Int.
    *
    * @param x      the Int value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: Int, factor: Factor, fuzz: Option[Fuzz[Double]]): Number = create(fromInt(x), factor, fuzz)

  /**
    * Method to construct a Number from a BigInt.
    *
    * @param x      the BigInt value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: BigInt, factor: Factor, fuzz: Option[Fuzz[Double]]): Number = apply(Rational(x), factor, fuzz)

  /**
    * Method to construct a Number from a Rational.
    * NOTE: this method is invoked indirectly by parse(String).
    *
    * @param x      the BigInt value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: Rational, factor: Factor, fuzz: Option[Fuzz[Double]]): Number = create(fromRational(x), factor, fuzz)

  /**
    * Method to construct a Number from a BigDecimal.
    *
    * @param x      the BigDecimal value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: BigDecimal, factor: Factor, fuzz: Option[Fuzz[Double]]): Number = Number(Rational(x), factor, fuzz)

  /**
    * Method to construct a Number from an optional Double.
    *
    * @param xo     an optional Double.
    * @param factor the appropriate factor
    * @return a Number based on xo.
    */
  def apply(xo: Option[Double], factor: Factor, fuzz: Option[Fuzz[Double]]): Number = create(fromDouble(xo), factor, fuzz)

  /**
    * Method to construct a Number from a Double.
    *
    * @param x      the Double value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: Double, factor: Factor, fuzz: Option[Fuzz[Double]]): Number = x match {
    case Double.NaN => Number(None, factor, fuzz)
    case _ => Number(Some(x), factor, fuzz)
  }

  /**
    * Method to construct a Number from an Int.
    *
    * @param x      the Int value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: Int, factor: Factor): Number = Number(x, factor, None)

  /**
    * Method to construct a Number from a BigInt.
    *
    * @param x      the BigInt value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: BigInt, factor: Factor): Number = Number(x, factor, None)

  /**
    * Method to construct a Number from a Rational.
    *
    * @param x      the BigInt value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: Rational, factor: Factor): Number = Number(x, factor, None)

  /**
    * Method to construct a Number from a BigDecimal.
    *
    * @param x      the BigDecimal value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: BigDecimal, factor: Factor): Number = Number(x, factor, None)

  /**
    * Method to construct a Number from an optional Double.
    *
    * @param xo     an optional Double.
    * @param factor the appropriate factor
    * @return a Number based on xo.
    */
  def apply(xo: Option[Double], factor: Factor): Number = Number(xo, factor, None)

  /**
    * Method to construct a Number from a Double.
    *
    * @param x      the Double value.
    * @param factor the appropriate factor
    * @return a Number based on x.
    */
  def apply(x: Double, factor: Factor): Number = Number(x, factor, None)

  /**
    * Method to construct a unit Number with explicit factor.
    *
    * @param factor the appropriate factor
    * @return a unit Number with the given factor.
    */
  def apply(factor: Factor): Number = Number(1, factor)

  /**
    * Method to construct a Number from an Int.
    *
    * @param x the Int value.
    * @return a Number based on x.
    */
  def apply(x: Int): Number = Number(x, Scalar)

  /**
    * Method to construct a Number from a BigInt.
    *
    * @param x a BigInt value.
    * @return a Number based on x.
    */
  def apply(x: BigInt): Number = Number(x, Scalar)

  /**
    * Method to construct a Number from a Rational.
    *
    * @param x a Rational value.
    * @return a Number based on x.
    */
  def apply(x: Rational): Number = Number(x, Scalar)

  /**
    * Method to construct a Number from a BigDecimal.
    *
    * @param x the BigDecimal value.
    * @return a Number based on x.
    */
  def apply(x: BigDecimal): Number = Number(x, Scalar)

  /**
    * Method to construct a Number from an optional Double.
    *
    * @param xo an optional Double.
    * @return a Number based on xo.
    */
  def apply(xo: Option[Double]): Number = Number(xo, Scalar)

  /**
    * Method to construct a Number from a Double.
    *
    * @param x the Double value.
    * @return a Number based on x.
    */
  def apply(x: Double): Number = Number(x, Scalar)

  /**
    * Method to construct an invalid Number.
    *
    * @return a invalid Number.
    */
  def apply(): Number = NaN

  /**
    * Invalid number.
    */
  val NaN: Number = Number(None)

  private val numberParser = new NumberParser()

  /**
    * Method to parse a String and yield a Try[Number].
    *
    * NOTE: this method indirectly invokes apply(Rational, Factor, Option of Fuzz[Double] )
    *
    * @param w the String to be parsed.
    * @return a Number.
    */
  def parse(w: String): Try[Number] = {
    val ny: Try[Number] = numberParser.parseNumber(w) map (_.specialize)
    ny flatMap (n => if (n.isValid) Success(n) else Failure(NumberException(s"parse: cannot parse $w as a Number")))
  }

  /**
    * Following are the definitions required by Ordering[Number]
    */
  trait NumberIsOrdering extends Ordering[Number] {
    def compare(x: Number, y: Number): Int = plus(x, negate(y)).signum
  }

  implicit object NumberIsOrdering extends NumberIsOrdering

  /**
    * Following are the definitions required by Numeric[Number]
    */
  trait NumberIsNumeric extends Numeric[Number] {
    def plus(x: Number, y: Number): Number = Number.plus(x, y)

    def minus(x: Number, y: Number): Number = Number.plus(x, negate(y))

    def times(x: Number, y: Number): Number = Number.times(x, y)

    def negate(x: Number): Number = Number.negate(x)

    def fromInt(x: Int): Number = Number(x)

    def parseString(str: String): Option[Number] = parse(str).toOption

    def toInt(x: Number): Int = toLong(x).toInt

    def toLong(x: Number): Long = x.maybeRational match {
      case Some(r) => r.toLong
      case None => x.maybeDouble match {
        case Some(z) => Math.round(z)
        case None => throw NumberException("toLong: this is invalid")
      }
    }

    def toDouble(x: Number): Double = x.maybeDouble match {
      case Some(y) => y
      case None => throw NumberException("toDouble: this is invalid")
    }

    def toFloat(x: Number): Float = toDouble(x).toFloat
  }

  def compare(x: Number, y: Number): Int = NumberIsOrdering.compare(x, y)

  def fuzzyCompare(x: Number, y: Number, p: Double): Int = Number.plus(x, Number.negate(y)).signum(p)

  /**
    * Following are the definitions required by Fractional[Number]
    */
  trait NumberIsFractional extends Fractional[Number] {
    def div(x: Number, y: Number): Number = Number.times(x, inverse(y))
  }

  implicit object NumberIsFractional extends NumberIsFractional with NumberIsNumeric with NumberIsOrdering

  private def plus(x: Number, y: Number): Number = y match {
    case n@FuzzyNumber(_, _, _) => n add x
    case _ =>
      val (a, b) = x.alignFactors(y)
      val (p, q) = a.alignTypes(b)
      prepareWithSpecialize(p.composeDyadic(q, p.factor)(DyadicOperationPlus))
  }

  private def times(x: Number, y: Number): Number =
    y match {
      case n@FuzzyNumber(_, _, _) => n multiply x
      case _ =>
        val (p, q) = x.alignTypes(y)
        val factor = p.factor + q.factor
        prepareWithSpecialize(p.composeDyadic(q, factor)(DyadicOperationTimes))
    }

  def prepare(no: Option[Number]): Number = no.getOrElse(Number())

  def prepareWithSpecialize(no: Option[Number]): Number = prepare(no).specialize

  private def sqrt(n: Number): Number = prepareWithSpecialize(n.scale(Scalar).transformMonadic(Scalar)(MonadicOperationSqrt))

  // CONSIDER dealing with non-Scalar x values up-front
  private def power(x: Number, y: Number): Number = y.normalize.toInt match {
    case Some(i) => power(x, i)
    case _ => y.toRational match {
      case Some(r) =>
        toInts(r) match {
          case Some((n, d)) =>
            root(power(x, n), d) match {
              case Some(q) => q
              case None => y.toDouble map x.make getOrElse Number()
            }
          case _ =>
            throw NumberException("rational power cannot be represented as two Ints")
        }
      case None => y.toDouble match {
        case Some(d) => power(x, d)
        case _ => throw NumberException("invalid power")
      }
    }
  }

  private def power(n: Number, i: Int) = i match {
    case x if x > 0 => LazyList.continually(n).take(x).product
    case x => LazyList.continually(inverse(n)).take(-x).product
  }

  private def power(n: Number, y: Double): Number = prepare(n.toDouble.map(x => math.pow(x, y)).map(n.make))

  private def root(n: Number, i: Int): Option[Number] = i match {
    case 2 => Some(n.makeFuzzyIfAppropriate(Number.sqrt))
    case _ => None
  }

  /**
    * Method to deal with a Scale factor change.
    *
    * NOTE: currently, direct conversion between E and Pi is not supported.
    * CONSIDER: re-implementing the Pi/Scalar and Scalar/Pi cases using MonadicOperationScale.
    * TODO: this will work for FuzzyNumber but only if the fuzz is relative, and even then not for E conversions.
    *
    * @param n the Number to be scaled.
    * @param factor the factor to which it should be converted.
    * @return the resulting Number (equivalent in value, but with a potentially different scale factor).
    */
  def scale(n: Number, factor: Factor): Number = (n.factor, factor) match {
    case (a, b) if a == b => n
    case (Pi, Scalar) | (Scalar, Pi) => prepare(n.maybeDouble.map(x => n.make(scaleDouble(x, n.factor, factor), factor)))
    case (E, Scalar) => prepare(n.transformMonadic(factor)(MonadicOperationExp))
    case (Scalar, E) => prepare(n.transformMonadic(factor)(MonadicOperationLog))
    case _ => throw NumberException("scaling between e and Pi factors is not supported")
  }

  // TEST me
  private def scale(x: Number, f: Int): Number = prepare(x.transformMonadic(x.factor)(MonadicOperationScale(f)))

  def negate(x: Number): Number = prepare(x.transformMonadic(x.factor)(MonadicOperationNegate))

  def inverse(x: Number): Number = prepare(x.transformMonadic(x.factor)(MonadicOperationInvert))

  private def isZero(x: Number): Boolean = x.query(QueryOperationIsZero)

  private def isInfinite(x: Number): Boolean = x.query(QueryOperationIsInfinite)

  private def signum(x: Number): Int = x.doTransformMonadic(x.factor)(identityTry, tryF(x => x.signum), tryF(math.signum)).flatMap(_.toInt).getOrElse(0)

  private def sin(x: Number): Number = prepareWithSpecialize(x.scale(Pi).transformMonadic(Scalar)(MonadicOperationSin))

  private def atan(x: Number, y: Number): Number = prepareWithSpecialize((y divide x).transformMonadic(Pi)(MonadicOperationAtan(x.signum))).modulate

  private def scaleDouble(x: Double, fThis: Factor, fResult: Factor) = x * fThis.value / fResult.value

  /**
    * This method returns a Number equivalent to x but with the value in an explicit factor-dependent range.
    * Only Pi is currently fixed within a range (0 -> 2).
    *
    * @param x the Number to operate on.
    * @return either x or a number equivalent to x with value in defined range.
    */
  private def modulate(x: Number): Number = x.factor match {
    case f@Pi => prepare(x.transformMonadic(f)(MonadicOperationModulate))
    case _ => x
  }
}

case class NumberException(str: String) extends Exception(str)

case class NumberExceptionWithCause(str: String, e: Throwable) extends Exception(str, e)

