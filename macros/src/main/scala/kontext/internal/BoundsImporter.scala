package kontext.internal

import scala.collection.immutable._
import scala.meta._

class BoundsImporter(annotationNew: Stat, methodTree: Defn.Def) {

  val renamingAst: Seq[Term.Arg] = annotationNew match {
    case q"new $_(..$mappings)" => mappings
    case _ => Seq.empty
  }

  val boundRenamings: Map[String, String] =
    BoundsImporter.toCompileTimeMap(renamingAst)

  val Defn.Def(_, name, tparams, _, _, body) =
    methodTree

  val tparamsWithCBounds: Seq[Type.Param] = tparams.collect {
    case tpWithC: Type.Param if tpWithC.cbounds.nonEmpty => tpWithC
  }

  val implicitImports: Seq[Defn.Val] = {
    val shouldSuffix = tparamsWithCBounds.size > 1
    for {
      tParamWithCBound <- tparamsWithCBounds
      cbound <- tParamWithCBound.cbounds
      cboundString <- BoundsImporter.typeToName.lift(cbound)
      renamed = boundRenamings.getOrElse(cboundString, cboundString)
      valNameString = if (shouldSuffix)
        renamed ++ tParamWithCBound.name.value
      else renamed
      valName = Pat.Var.Term(Term.Name(valNameString))
      tp = Type.Name(tParamWithCBound.name.value)
    } yield q"""val $valName: $cbound[$tp] = implicitly[$cbound[$tp]]"""
  }

  def build(): Defn.Def = {
    val errs = detectErrors
    if (errs.nonEmpty) {
      val errMsg =
        s"""
           |
           |  Detected the following errors:
           |
           |${errs.mkString("\n\n")}
         """.stripMargin
      abort(errMsg)
    }

    val newBody = body match {
      case b: Term.Block => b.copy(stats = implicitImports ++ b.stats)
      case other => Term.Block(implicitImports :+ other)
    }
    methodTree.copy(body = newBody)
  }

  private def detectErrors: Seq[String] = {
    val unsupportedTypes =
      BoundsImporter.findUnsupportedTypes(tparams.flatMap(_.cbounds))
    val unsupportedTypeErrMsg = if (unsupportedTypes.nonEmpty) {
      val tpeString = unsupportedTypes.map(tpe => s"    * $tpe").mkString("\n")
      Some(s"""
              |
              |  The following typeclass bounds are not supported by this macro, maybe try importing them fully
              |  (perhaps with qualified names)?
              |
              |  $tpeString
         """.stripMargin)
    } else None
    val unsupportedMappings =
      BoundsImporter.findUnsupportedMappingPairs(renamingAst)
    val unsupportedMappingsErr = if (unsupportedMappings.nonEmpty) {
      val mappingString =
        unsupportedMappings.map(m => s"    * ${m.syntax}").mkString("\n")
      Some(s"""
              |
              |  The following mappings are not supported for renaming. Please try using simple literal symbol
              |  syntax (e.g. `'Monad -> 'M `)
              |
              |  $mappingString
           """.stripMargin)
    } else None
    Seq(unsupportedTypeErrMsg, unsupportedMappingsErr).flatten
  }

}

object BoundsImporter {

  def findUnsupportedMappingPairs(mappings: Seq[Term.Arg]): Seq[Term.Arg] =
    for {
      mapping <- mappings
      if !astToCompileTimePair.isDefinedAt(mapping)
    } yield mapping

  def findUnsupportedTypes(types: Seq[Type]): Seq[Type] =
    for {
      tpe <- types
      if !typeToName.isDefinedAt(tpe)
    } yield tpe

  def toCompileTimeMap(mappings: Seq[Term.Arg]): Map[String, String] =
    mappings.collect(BoundsImporter.astToCompileTimePair).toMap

  val typeToName: PartialFunction[Type, String] = {
    case Type.Name(s) => s
    case Type.Apply(tpInner, _) => typeToName(tpInner)
    case tp: Type.Param => tp.name.value
    case Type.Apply(tpe, _) => typeToName(tpe)
  }

  val astToCompileTimePair: PartialFunction[Term.Arg, (String, String)] = {
    case q"""${Lit.Symbol(nameFrom)} -> ${Lit.Symbol(nameTo)}""" =>
      nameFrom.toString -> nameTo.toString
    case q"""(${Lit.Symbol(nameFrom)}, ${Lit.Symbol(nameTo)})""" =>
      nameFrom.toString -> nameTo.toString

    // variations of the same thing
    case q"""scala.Symbol(${Lit.String(nameFrom)}) -> scala.Symbol(${Lit
          .String(nameTo)})""" =>
      nameFrom -> nameTo
    case q"""(scala.Symbol(${Lit.String(nameFrom)}), scala.Symbol(${Lit
          .String(nameTo)}))""" =>
      nameFrom -> nameTo

    case q"""_root_.scala.Symbol(${Lit.String(nameFrom)}) -> _root_.scala.Symbol(${Lit
          .String(nameTo)})""" =>
      nameFrom -> nameTo
    case q"""(_root_.scala.Symbol(${Lit.String(nameFrom)}), _root_.scala.Symbol(${Lit
          .String(nameTo)}))""" =>
      nameFrom -> nameTo

    case q"""Symbol(${Lit.String(nameFrom)}) -> Symbol(${Lit
          .String(nameTo)})""" =>
      nameFrom -> nameTo
    case q"""(Symbol(${Lit.String(nameFrom)}), Symbol(${Lit
          .String(nameTo)}))""" =>
      nameFrom -> nameTo
  }
}
