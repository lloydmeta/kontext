# kontext

`implicitly` but for _all_ context-bound typeclass instances, automatically.

Useful when you have methods that are bound by lots of typeclasses and you don't want to maintain a long list of implicit parameters. Possibly useful when you want to do Tagless Final style EDSLs.

## Usage

```scala
import kontext._

trait Maths[G[_]] {
  def int(i: Int): G[Int]
  def add(l: G[Int], r: G[Int]): G[Int]
}

// Simple usage
@boundly
def add[F[_]: Maths](x: Int, y: Int) = Maths.add(Maths.int(x), Maths.int(y))

// Example of renaming
@boundly('Maths -> 'M)
def addAliased[F[_]: Maths](x: Int, y: Int) = M.add(M.int(x), M.int(y))

// Write an interpreter
type Id[A] = A
implicit val interpreter = new Maths[Id] {
  def int(i: Int)                 = i
  def add(l: Id[Int], r: Id[Int]) = l + r
}

// Use
add[Id](3, 10)
// res0: Int = 13

addAliased[Id](3, 10)
// res1: Int = 13
```