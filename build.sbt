lazy val theVersion = "0.1.2-SNAPSHOT"

lazy val theScalaVersion  = "2.11.11"
lazy val scalaVersions    = Seq("2.11.11", "2.12.2")
lazy val scalaTestVersion = "3.2.0-SNAP4"
lazy val catsVersion      = "0.9.0"

scalaVersion in ThisBuild := theScalaVersion

lazy val root = Project(id = "kontext-root", base = file("."))
  .settings(
    name := "kontext-root",
    crossScalaVersions := scalaVersions,
    crossVersion := CrossVersion.binary,
    publishSettings,
    // Do not publish the root project (it just serves as an aggregate)
    publishArtifact := false,
    publishLocal := {}
  )
  .aggregate(macrosJs, macrosJvm, examples)

// Define macros in this project.
lazy val macros = crossProject
  .crossType(CrossType.Pure)
  .settings(
    name := "kontext",
    metaMacroSettings,
    testSettings,
    publishSettings,
    commonSettings,
    // A dependency on scala.meta is required to write new-style macros, but not
    // to expand such macros.  This is similar to how it works for old-style
    // macros and a dependency on scala.reflect.
    libraryDependencies += "org.scalameta" %%% "scalameta" % "1.7.0"
  )
lazy val macrosJs  = macros.js
lazy val macrosJvm = macros.jvm

lazy val examples = Project(id = "examples", base = file("examples"))
  .settings(
    name := "kontext-examples",
    commonSettings,
    testSettings,
    metaMacroSettings,
    publishSettings,
    publishArtifact := false,
    publishLocal := {},
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion
  )
  .dependsOn(macrosJvm)

lazy val testSettings = {
  Seq(
    libraryDependencies ++=
      Seq(
        "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
      ),
    doctestGenTests := {
      if (isScalaJSProject.value)
        Seq.empty
      else
        doctestGenTests.value
    },
    doctestTestFramework := DoctestTestFramework.ScalaTest,
    doctestWithDependencies := false,
    scalaJSStage in Test := FastOptStage
  )
}

lazy val commonSettings = Seq(
  organization := "com.beachape",
  version := theVersion,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused-import"
  ),
  wartremoverErrors in (Compile, compile) ++= Warts.unsafe
)

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  // A dependency on macro paradise 3.x is required to both write and expand
  // new-style macros.  This is similar to how it works for old-style macro
  // annotations and a dependency on macro paradise 2.x.
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M8" cross CrossVersion.full),
  scalacOptions += "-Xplugin-require:macroparadise",
  // temporary workaround for https://github.com/scalameta/paradise/issues/10
  scalacOptions in (Compile, console) := Seq() // macroparadise plugin doesn't work in repl yet.
)

// Settings for publishing to Maven Central
lazy val publishSettings: Seq[Def.Setting[_]] = Seq(
  pomExtra :=
    <url>https://github.com/lloydmeta/kontext</url>
      <licenses>
        <license>
          <name>MIT</name>
          <url>http://opensource.org/licenses/MIT</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:lloydmeta/kontext.git</url>
        <connection>scm:git:git@github.com:lloydmeta/kontext.git</connection>
      </scm>
      <developers>
        <developer>
          <id>lloydmeta</id>
          <name>Lloyd Chan</name>
          <url>https://beachape.com</url>
        </developer>
      </developers>,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  }
)
