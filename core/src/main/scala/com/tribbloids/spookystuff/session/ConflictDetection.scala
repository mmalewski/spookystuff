package com.tribbloids.spookystuff.session

import com.tribbloids.spookystuff.utils.{SpookyUtils, TreeException}

import scala.util.Try

/**
  * Created by peng on 14/01/17.
  */
trait ConflictDetection extends Cleanable {

  def resourceIDs: Map[String, Set[_]]
  def prefix: String = this.getClass.getCanonicalName

  final def effectiveResourceIDs: Map[String, Set[Any]] = resourceIDs.map {
    tuple =>
      val rawK = if (tuple._1.isEmpty) null
      else tuple._1
      val k = SpookyUtils./:/(prefix, rawK)
      val v = tuple._2.map(_.asInstanceOf[Any])
      k -> v
  }
}

object ConflictDetection {

  def conflicts: Seq[Try[Unit]] = {
    val allResourceIDs: Map[String, Seq[Any]] = Cleanable.getTyped[ConflictDetection]
      .map {
        _.effectiveResourceIDs.mapValues(_.toSeq)
      }
      .reduceOption {
        (m1, m2) =>
          val keys = (m1.keys ++ m2.keys).toSeq.distinct
          val kvs = keys.map {
            k =>
              val v = m1.getOrElse(k, Nil) ++ m2.getOrElse(k, Nil)
              k -> v
          }
          Map(kvs: _*)
      }
      .getOrElse(Map.empty)
    allResourceIDs
      .toSeq
      .flatMap {
        tuple =>
          tuple._2.groupBy(identity).values
            .map {
              vs =>
                Try {
                  assert(vs.size == 1, s"${tuple._1}: resource ${vs.head} is used by ${vs.size} objects")
                }
            }
      }
  }

  def detectConflict(extra: Seq[Throwable] = Nil): Unit = {

    TreeException.&&&(conflicts, extra = extra)
  }
}