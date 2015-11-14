package com.tribbloids.spookystuff.pipeline

import java.lang.reflect.Modifier

import com.tribbloids.spookystuff.row.DepthKey
import com.tribbloids.spookystuff.sparkbinding.PageRowRDD
import com.tribbloids.spookystuff.{PipelineException, SpookyContext}
import org.apache.spark.ml.param.{Param, ParamMap, ParamPair}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.language.dynamics

trait RemoteTransformer extends RemoteTransformerLike with Dynamic {

  import com.tribbloids.spookystuff.dsl._

  override def copy(extra: ParamMap): RemoteTransformer = this.defaultCopy(extra)

  def +> (another: RemoteTransformer): RemoteTransformerChain = new RemoteTransformerChain(Seq(this)) +> another

  /*
  This dynamic function automatically add a setter to any Param-typed property
   */
  def applyDynamic(methodName: String)(args: Any*): this.type = {
    assert(args.length == 1)
    val arg = args.head

    //TODO: there is no need, all parameters are already defined in paramMap, remove this to promote more simple API
    if (methodName.startsWith("set")) {
      val fieldName = methodName.stripPrefix("set")
      val param = this.params.find(_.name == fieldName)

      val field = this.getClass.getMethod(fieldName) //this gets all the getter generated by Scala
      val value = field.invoke(this).asInstanceOf[Param[Any]]

      set(value, arg)
      this
    }
    else throw new PipelineException(s"setter $methodName doesn't exist")
  }

  /*
  TODO: the original plan of using dynamic param definition to reduce pipeline code seems not to be supported by scala, suspended
  see http://stackoverflow.com/questions/33699836/in-scala-how-to-find-invocation-of-subroutines-defined-in-a-function for detail
   */
//  val dynamicParams: ArrayBuffer[Param[_]] = ArrayBuffer()

//  override lazy val params: Array[Param[_]] = {
//    val methods = this.getClass.getMethods
//    val fixedParams = methods.filter { m =>
//      Modifier.isPublic(m.getModifiers) &&
//        classOf[Param[_]].isAssignableFrom(m.getReturnType) &&
//        m.getParameterTypes.isEmpty
//    }.sortBy(_.getName)
//      .map(m => m.invoke(this).asInstanceOf[Param[_]])
//    fixedParams ++ dynamicParams
//  }

//  def param[T](
//                name: String,
//                doc: String = "(no documentation)",
//                default: T = null,
//                example: T = null,
//                defaultOption: Option[T] = None,
//                exampleOption: Option[T] = None
//              ): T = {
//
//    if (!params.exists(_.name == name)) {
//      val param = new Param[T](this, name, doc)
//      val effectiveDefault = Option(default).orElse(defaultOption)
//      if (effectiveDefault.nonEmpty) this.setDefault(param -> effectiveDefault.get)
//      val effectiveExample = Option(example).orElse(exampleOption)
//      if (effectiveExample.nonEmpty) this.setExample(param -> effectiveExample.get)
//      this.dynamicParams += param
//      this.getOrDefault(param)
//    }
//    else {
//      val param = params.filter(_.name == name).head.asInstanceOf[Param[T]]
//      assert(param.doc == doc, "documentation has to be consistent")
//      assert(this.getDefault(param) == defaultOption, "default value has to be consistent")
//      assert(this.getExample(param) == exampleOption, "default value has to be consistent")
//      this.getOrDefault[T](param)
//    }
//  }

  //example value of parameters used for testing
  val exampleParamMap: ParamMap = ParamMap.empty

  def exampleInput(spooky: SpookyContext): PageRowRDD = spooky

  protected final def setExample(paramPairs: ParamPair[_]*): this.type = {
    paramPairs.foreach { p =>
      setExample(p.param.asInstanceOf[Param[Any]], p.value)
    }
    this
  }

  protected final def setExample[T](param: Param[T], value: T): this.type = {
    exampleParamMap.put(param -> value)
    this
  }

  override def explainParam(param: Param[_]): String = {
    val str = super.explainParam(param)

    val exampleStr = if (isDefined(param)) {
      exampleParamMap.get(param).map("(e.g.: " + _ + " )")
    } else {
      ""
    }
    str + exampleStr
  }

  override def test(spooky: SpookyContext): Unit= {

    val names = this.params.map(_.name)
    assert(names.length == names.distinct.length) //ensure that there is no name duplicity

    this.exampleParamMap.toSeq.foreach {
      pair =>
        this.set(pair)
    }

    val result: PageRowRDD = this.transform(this.exampleInput(spooky)).persist()
    val keys = result.keySeq

    result.toDF(sort = true).show()

    keys.foreach{
      key =>
        val distinct = result.flatMap(_.get(key)).distinct()
        val values = distinct.take(2)
        assert(values.length >= 1)
        key match {
          case depthKey: DepthKey =>
            depthKey.maxOption.foreach {
              expectedMax =>
                assert(expectedMax == distinct.map(_.asInstanceOf[Int]).max())
            }
          case _ =>
        }
        LoggerFactory.getLogger(this.getClass).info(s"column '${key.name} has passed the test")
        result.unpersist()
    }

    assert(result.toObjectRDD(S_*).flatMap(v => v).count() >= 1)
  }
}