package com.tribbloids.spookystuff.extractors

import com.tribbloids.spookystuff.SpookyEnvFixture
import com.tribbloids.spookystuff.actions.Wget
import org.apache.spark.ml.dsl.utils.MessageView
import org.apache.spark.sql.types.NullType

//case class Lit[T, +R](value: R, dataType: DataType = NullType)
/**
  * Created by peng on 15/06/16.
  */
class ExtractorsSuite extends SpookyEnvFixture {

  it("Literal can be converted to JSON") {
    val lit: Lit[FR, Int] = Lit(1)

    val json = lit.toJSON()
    json.shouldBe (
      "1"
    )
  }

  it("Action that use Literal can be converted to JSON") {
    val action = Wget(Lit("http://dummy.org"))

    val json = action.toJSON()
    json.shouldBe(
      """
        |{
        |  "className" : "com.tribbloids.spookystuff.actions.Wget",
        |  "params" : {
        |    "uri" : "http://dummy.org",
        |    "filter" : { }
        |  }
        |}
      """.stripMargin
    )
  }

  it("Literal has the correct toString form") {
    val str = Lit("lit").toString
    assert(str == "lit")
  }
}
