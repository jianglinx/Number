package com.phasmidsoftware.number.core

import com.phasmidsoftware.number.core.Number.negate

trait Expression {

  /**
    * If it is possible to simplify this Expression, then we do so.
    *
    * @return an Expression tree which is the equivalent of this.
    */
  def simplify: Expression

  /**
    * Action to materialize this Expression as a Number,
    * that is to say we eagerly evaluate this Expression as a Number.
    *
    * @return the materialized Number.
    */
  def materialize: Number

  /**
    * Action to materialize this Expression and render it as a String,
    * that is to say we eagerly evaluate this Expression as a String.
    *
    * @return a String representing the value of this expression.
    */
  def render: String

  /**
    * Eagerly compare this Expression with comparand.
    *
    * @param comparand the expression to be compared.
    * @return the result of comparing materialized this with materialized comparand.
    */
  def compare(comparand: Expression): Int = materialize.compare(comparand.materialize)
}

object Expression {

  /**
    * The following method is helpful in getting an expression started.
    */
  def apply(x: Number): Expression = x match {
    case Number.zero => Zero
    case Number.one => One
    case _ => Literal(x)
  }

  /**
    * The following method is helpful in getting an expression started.
    */
  def apply(x: Int): Expression = x match {
    case 0 => Zero
    case 1 => One
    case _ => Expression(Number(x))
  }

  /**
    * The following method is helpful in getting an expression started.
    */
  def apply(x: String): Expression = Expression(Number(x))

  /**
    * The following constants are helpful in getting an expression started.
    */
  val zero: Expression = Zero
  val one: Expression = One
  val pi: Expression = Expression(Number.pi)
  val e: Expression = Expression(Number.e)

  implicit class ExpressionOps(x: Expression) {
    /**
      * Method to lazily multiply the Number x by y.
      *
      * @param y another Expression.
      * @return an Expression which is the lazy product of x and y.
      */
    def +(y: Expression): Expression = BiFunction(x, y, Sum)

    /**
      * Method to lazily multiply the Number x by y.
      *
      * @param y another Number.
      * @return an Expression which is the lazy product of x and y.
      */
    def +(y: Number): Expression = this.+(Expression(y))

    /**
      * Method to lazily multiply the Number x by y.
      *
      * @param y another Number.
      * @return an Expression which is the lazy product of x and y.
      */
    def +(y: Int): Expression = this.+(Number(y))

    /**
      * Method to lazily subtract the Number y from x.
      *
      * @param y another Number.
      * @return an Expression which is the lazy product of x and y.
      */
    def -(y: Number): Expression = BiFunction(x, Expression(y).unary_-, Sum)

    /**
      * Method to lazily change the sign of this expression.
      *
      * @return an Expression which is this negated.
      */
    def unary_- : Expression = BiFunction(x, MinusOne, Product)

    /**
      * Method to lazily subtract the Number y from x.
      *
      * @param y another Number.
      * @return an Expression which is the lazy product of x and y.
      */
    def -(y: Int): Expression = this.-(Number(y))

    /**
      * Method to lazily multiply the Number x by y.
      *
      * @param y another Expression.
      * @return an Expression which is the lazy product of x and y.
      */
    def *(y: Expression): Expression = BiFunction(x, y, Product)

    /**
      * Method to lazily multiply the Number x by y.
      *
      * @param y another Number.
      * @return an Expression which is the lazy product of x and y.
      */
    def *(y: Number): Expression = *(Expression(y))

    /**
      * Method to lazily multiply the Number x by y.
      *
      * @param y another Number.
      * @return an Expression which is the lazy product of x and y.
      */
    def *(y: Int): Expression = *(Number(y))

    /**
      * Method to lazily yield the reciprocal of this Expression.
      *
      * @return an Expression representing the reciprocal of this.
      */
    def reciprocal: Expression = BiFunction(x, MinusOne, Power)

    /**
      * Method to lazily divide the Number x by y.
      *
      * @param y another Number.
      * @return an Expression which is the lazy quotient of x and y.
      */
    def /(y: Number): Expression = *(Expression(y).reciprocal)

    /**
      * Method to lazily multiply the Number x by y.
      *
      * @param y another Number.
      * @return an Expression which is the lazy product of x and y.
      */
    def /(y: Int): Expression = /(Number(y))

    /**
      * Method to lazily raise x to the power of y.
      *
      * @param y the power to which x should be raised.
      * @return an Expression representing x to the power of y.
      */
    def ^(y: Expression): Expression = BiFunction(x, y, Power)

    /**
      * Method to lazily raise x to the power of y.
      *
      * @param y the power to which x should be raised.
      * @return an Expression representing x to the power of y.
      */
    def ^(y: Number): Expression = ^(Expression(y))

    /**
      * Method to lazily raise the Number x to the power of y.
      *
      * @param y the power.
      * @return an Expression which is the lazy power of x to the y.
      */
    def ^(y: Int): Expression = ^(Number(y))

    /**
      * Method to lazily get the square root of x.
      *
      * @return an Expression representing the square root of x.
      */
    def sqrt: Expression = this ^ Number(2).reciprocal

    /**
      * Method to lazily get the sin of x.
      *
      * @return an Expression representing the sin of x.
      */
    def sin: Expression = Function(x, Sine)

    /**
      * Eagerly compare this expression with y.
      *
      * @param comparand the number to be compared.
      * @return the result of the comparison.
      */
    def compare(comparand: Number): Int = x compare comparand
  }
}

case class Literal(x: Number) extends Expression {
  /**
    * Action to materialize this Expression as a Number,
    * that is to say we eagerly evaluate this Expression as a Number.
    *
    * @return the materialized Number.
    */
  def materialize: Number = x

  /**
    * Action to materialize this Expression and render it as a String,
    * that is to say we eagerly evaluate this Expression as a String.
    *
    * @return a String representing the value of this expression.
    */
  def render: String = x.render

  /**
    * Generate a String for debugging purposes.
    *
    * @return a String representation of this Literal.
    */
  override def toString: String = s"$x"

  /**
    * If it is possible to simplify this Expression, then we do so.
    *
    * @return an Expression tree which is the equivalent of this.
    */
  def simplify: Expression = this
}

abstract class Constant extends Expression {
  /**
    * If it is possible to simplify this Expression, then we do so.
    *
    * @return this.
    */
  def simplify: Expression = this

  override def toString: String = render
}

case object Zero extends Constant {
  /**
    * Action to materialize this Expression as a Number,
    * that is to say we eagerly evaluate this Expression as a Number.
    *
    * @return Number.zero
    */
  def materialize: Number = Number.zero

  /**
    * Action to materialize this Expression and render it as a String,
    * that is to say we eagerly evaluate this Expression as a String.
    *
    * @return "0"".
    */
  def render: String = "0"
}

case object One extends Constant {
  /**
    * Action to materialize this Expression as a Number,
    * that is to say we eagerly evaluate this Expression as a Number.
    *
    * @return the materialized Number.
    */
  def materialize: Number = Number.one

  /**
    * Action to materialize this Expression and render it as a String,
    * that is to say we eagerly evaluate this Expression as a String.
    *
    * @return "1".
    */
  def render: String = "1"
}

case object MinusOne extends Constant {
  /**
    * Action to materialize this Expression as a Number,
    * that is to say we eagerly evaluate this Expression as a Number.
    *
    * @return the materialized Number.
    */
  def materialize: Number = negate(Number.one)

  /**
    * Action to materialize this Expression and render it as a String,
    * that is to say we eagerly evaluate this Expression as a String.
    *
    * @return "1".
    */
  def render: String = "-1"
}

/**
  * This class represents a monadic function of the given expression.
  *
  * @param x the expression being operated on.
  * @param f the function to be applied to x.
  */
case class Function(x: Expression, f: ExpressionFunction) extends Expression {
  /**
    * Action to materialize this Expression as a Number.
    *
    * @return the materialized Number.
    */
  def materialize: Number = f(x.simplify.materialize)

  /**
    * Action to materialize this Expression and render it as a String.
    *
    * @return a String representing the value of this expression.
    */
  def render: String = materialize.toString

  /**
    * If it is possible to simplify this Expression, then we do so.
    *
    * NOTE: currently, we do not do any simplification of monadic functions.
    * Possibilities: swapping the order of commutative operators (are there any?)
    * Also, pairing inverse trigonometric functions like atan (a dyadic operator) with sin/cos.
    * Also, distributing monadic functions to the branches of an addition, etc.
    *
    * @return an Expression tree which is the equivalent of this.
    */
  def simplify: Expression = this

  override def toString: String = s"$f($x)"
}

/**
  * This class represents a dyadic function of the two given expressions.
  *
  * @param a the first expression being operated on.
  * @param b the second expression being operated on.
  * @param f the function to be applied to a and b.
  */
case class BiFunction(a: Expression, b: Expression, f: ExpressionBiFunction) extends Expression {
  /**
    * Action to materialize this Expression as a Number.
    *
    * @return the materialized Number.
    */
  def materialize: Number = f(a.simplify.materialize, b.simplify.materialize)

  /**
    * Action to materialize this Expression and render it as a String.
    *
    * @return a String representing the value of this expression.
    */
  def render: String = materialize.toString

  /**
    * Render this BiFunction for debugging purposes.
    *
    * @return a String showing a, f, and b in parentheses.
    */
  override def toString: String = s"($a $f $b)"

  private def inverter(name: String): Option[(ExpressionBiFunction, Expression, Expression)] = name match {
    case "+" => Some((Product, MinusOne, Zero))
    case "*" => Some((Power, MinusOne, One))
    case _ => None
  }

  private def cancel(l: Expression, r: Expression, name: String): Option[Expression] = inverter(name) match {
    case Some((op, exp, result)) => r match {
      case BiFunction(a, b, f) if same(l, a) && b == exp && f == op => Some(result)
      case BiFunction(a, b, f) if a == exp && same(l, b) && f == op => Some(result)
      case _ => None
    }
    case None => None
  }

  /**
    * NOTE: we do a materialize here, which isn't right.
    * TODO fix it.
    */
  private def same(l: Expression, a: Expression) = a == l || a == l.materialize

  private def grouper(name: String): Option[(ExpressionBiFunction, ExpressionBiFunction)] = name match {
    case "^" => Some((Power, Product))
    case _ => None
  }

  def gatherer(l: Expression, r: Expression, name: String): Option[Expression] = grouper(name) match {
    case Some((op, f)) => l match {
      case BiFunction(a, b, `op`) =>
        val function1 = BiFunction(b, r, f)
        val simplify1 = function1.simplify
        val function2 = BiFunction(a, simplify1, op)
        val simplify2 = function2.simplify
        Some(simplify2)
      case _ => None
    }
    case None => None
  }

  def cancelOperands(l: Expression, r: Expression, name: String): Option[Expression] = (l, r, name) match {
    case (MinusOne, One, "+") | (One, MinusOne, "+") => Some(Zero)
    case _ => None
  }

  /**
    * If it is possible to simplify this Expression, then we do so.
    *
    * @return an Expression tree which is the equivalent of this.
    */
  def simplify: Expression = {
    val left = a.simplify
    val right = b.simplify
    val result = f.name match {
      case "+" if left == Zero => right
      case "*" if left == One => right
      case "+" if right == Zero => left
      case "*" if right == One => left
      case "^" if right == One => left
      case "^" if right == Zero => One
      case _ =>
        // NOTE: we check only for gatherer left, right
        cancelOperands(left, right, f.name) orElse
                cancel(left, right, f.name) orElse
                cancel(right, left, f.name) orElse
                gatherer(left, right, f.name) getOrElse
                BiFunction(left, right, f)
    }
    // TODO remove this debugging aid
    if (result != this) println(s"simplified $this to $result")
    result
  }
}

case object Sine extends ExpressionFunction(x => x.sin, "sin")

case object Sum extends ExpressionBiFunction((x, y) => x add y, "+")

case object Product extends ExpressionBiFunction((x, y) => x multiply y, "*")

case object Power extends ExpressionBiFunction((x, y) => x.power(y), "^")

/**
  * A lazy monadic expression function.
  * TODO need to mark whether this function is exact or not.
  *
  * @param f    the function Number => Number
  * @param name the name of this function.
  */
class ExpressionFunction(f: Number => Number, name: String) extends (Number => Number) {
  /**
    * Evaluate this function on x.
    *
    * @param x the parameter to the function.
    * @return the result of f(x).
    */
  override def apply(x: Number): Number = f(x)

  /**
    * Generate helpful debugging information about this ExpressionFunction.
    *
    * @return a String.
    */
  override def toString: String = s"$name"
}

/**
  * A lazy dyadic expression function.
  * TODO need to mark whether this function is exact or not.
  *
  * @param f    the function (Number, Number) => Number
  * @param name the name of this function.
  */
class ExpressionBiFunction(val f: (Number, Number) => Number, val name: String) extends ((Number, Number) => Number) {
  /**
    * Evaluate this function on x.
    *
    * @param a the first parameter to the function.
    * @param b the second parameter to the function.
    * @return the result of f(x).
    */
  override def apply(a: Number, b: Number): Number = f(a, b)

  /**
    * Generate helpful debugging information about this ExpressionFunction.
    *
    * @return a String.
    */
  override def toString: String = s"$name"
}