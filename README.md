# kontext [![Build Status](https://travis-ci.org/lloydmeta/kontext.svg?branch=master)](https://travis-ci.org/lloydmeta/kontext) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.beachape/kontext_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.beachape/kontext_2.11) [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.15.svg)](https://www.scala-js.org)

`implicitly` but for _all_ context-bound typeclass instances, automatically.

Useful when you have methods that are bound by lots of typeclasses and you don't want to maintain a long list of implicit parameters. Possibly useful when you want to do Tagless Final style EDSLs.

Written in [scala.meta](http://scalameta.org/) for future compatibility and other nice things (e.g. free IDE support, like in IntelliJ).

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

For a more realistic usage scenario, check out [examples/FibApp](https://github.com/lloydmeta/kontext/blob/master/examples/src/main/scala/FibApp.scala#L16), where 3 Tagless Final DSLs are mixed together.

## Sbt

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.beachape/kontext_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.beachape/kontext_2.11)

```scala
libraryDependencies += "com.beachape" %% "kontext-core" % s"$latest_version"


// Additional ceremony for using Scalameta macro annotations

resolvers += Resolver.url(
  "scalameta",
  url("http://dl.bintray.com/scalameta/maven"))(Resolver.ivyStylePatterns)

// A dependency on macro paradise is required to both write and expand
// new-style macros.  This is similar to how it works for old-style macro
// annotations and a dependency on macro paradise 2.x.
addCompilerPlugin(
  "org.scalameta" % "paradise" % "3.0.0-M8" cross CrossVersion.full)

scalacOptions += "-Xplugin-require:macroparadise"

```