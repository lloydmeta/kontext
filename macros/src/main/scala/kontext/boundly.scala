package kontext

import scala.annotation.compileTimeOnly
import scala.meta._

/**
  * Annotation for methods that adds value declarations pointing to instances of all the context-bound typeclasses
  * that you have declared.
  *
  * tl;dr: like `implicitly` but for _all_ context-bound typeclass instances, automatically.
  *
  * If there are more than 1 type parameters with context bounds, then the vals generated will have the
  * type parameter names suffixed to their typeclass names.
  *
  * You can also pass a variable argument list for renaming the members generated (see example).
  *
  * Example:
  *
  * {{{
  * scala> import kontext._
  *
  * scala> trait Maths[G[_]] {
  *      |   def int(i: Int): G[Int]
  *      |   def add(l: G[Int], r: G[Int]): G[Int]
  *      | }
  *
  * // Write an interpreter
  * scala> type Id[A] = A
  * scala> implicit val interpreter = new Maths[Id] {
  *      |   def int(i: Int)                 = i
  *      |   def add(l: Id[Int], r: Id[Int]) = l + r
  *      | }
  *
  * // This Program wrapper is only for the sake of sbt-doctest and is unnecessary in real-life usage.
  * scala> object Program {
  *      |   // Example of using @boundly annotation
  *      |   @boundly
  *      |   def add[F[_]: Maths](x: Int, y: Int) = {
  *      |     Maths.add(Maths.int(x), Maths.int(y))
  *      |   }
  *      |   // Example of renaming
  *      |   @boundly('Maths -> 'M)
  *      |   def addAliased[F[_]: Maths](x: Int, y: Int) = {
  *      |     M.add(M.int(x), M.int(y))
  *      |   }
  *      | }
  *
  * scala> Program.add[Id](3, 10)
  * res0: Int = 13
  *
  * scala> Program.addAliased[Id](3, 10)
  * res1: Int = 13
  * }}}
  *
  * @param renames variable argument list of symbols for modifying the names of the members generated.
  *                e.g. `'Monad -> 'M` will cause a val member called `M` to be prepended into the
  *                method body.
  */
@compileTimeOnly("Enable macro paradise to expand macro annotations")
class boundly(renames: (scala.Symbol, scala.Symbol) *) extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn match {
      case m: Defn.Def => {
        val builder = new internal.BoundsImporter(this, m)
        val r = builder.build()
//        println(r.syntax)
        r
      }
      case other => {
        val errMsg =
          s"""
             |
             | This annotation currently only supports methods, but you provided:
             |
             |   ${other.structure}
           """.stripMargin
        abort(errMsg)
      }
    }
  }
}

