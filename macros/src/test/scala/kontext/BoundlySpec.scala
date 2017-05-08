package kontext

import org.scalatest.{FunSpec, Matchers}

import scala.language.higherKinds

class BoundlySpec extends FunSpec with Matchers {

  trait First[A[_]] {
    def hello: String
  }
  trait Second[A[_]] {
    def hello: String
  }
  trait Third[A[_]] {
    def hello: String
  }

  implicit val OptFirst = new First[Option] {
    def hello: String = "first option"
  }

  implicit val OptSecond = new Second[Option] {
    def hello: String = "second option"
  }

  implicit val OptThird = new Third[Option] {
    def hello: String = "third option"
  }

  trait Eh[A]

  describe("boundly-annotated method") {

    describe("when there is just one bounded type parameter") {

      @boundly
      def singleTypeParam[F[_]: First: Second: Third]: Seq[String] = {
        Seq(First.hello, Second.hello, Third.hello)
      }

      it("should work") {
        singleTypeParam[Option] shouldBe Seq("first option",
                                             "second option",
                                             "third option")
      }

    }
    describe("when there are renamings") {

      @boundly('First -> 'Erste)
      def singleTypeParam[F[_]: First: Second: Third]: Seq[String] = {
        Seq(Erste.hello, Second.hello, Third.hello)
      }

      it("should work") {
        singleTypeParam[Option] shouldBe Seq("first option",
                                             "second option",
                                             "third option")
      }

    }

    describe("when there are more than one bounded type parameters") {

      @boundly
      def multipleTypeParams[F[_]: First: Second: Third, G[_]: First]
        : Seq[String] = {
        Seq(FirstF.hello, SecondF.hello, ThirdF.hello, FirstG.hello)
      }

      it("should work") {
        multipleTypeParams[Option, Option] shouldBe Seq("first option",
                                                        "second option",
                                                        "third option",
                                                        "first option")
      }

    }

  }

}
