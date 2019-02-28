package com.tribbloids.spookystuff.graph
import com.tribbloids.spookystuff.graph.Element.Edge
import com.tribbloids.spookystuff.graph.IDAlgebra.Rotator

trait GraphComponent[T <: GraphSystem] extends GraphSystem.Sugars[T] {

  protected def _replicate(m: _Mutator)(implicit idRotator: Rotator[ID] = idAlgebra.createRotator()): _GraphComponent

  def replicate(m: _Mutator)(implicit idRotator: Rotator[ID] = idAlgebra.createRotator()): this.type =
    _replicate(m).asInstanceOf[this.type]
}

object GraphComponent {

  case class Heads[T <: GraphSystem](vs: Seq[Edge[T]] = Nil) extends GraphSystem.TypeSugers[T] {

    vs.foreach(_.isHead)

    def replicate(m: _Mutator)(implicit idRotator: Rotator[ID]) = {
      this.copy(
        vs.map(_.replicate(m))
      )
    }
  }

  case class Tails[T <: GraphSystem](vs: Seq[Edge[T]] = Nil) extends GraphSystem.TypeSugers[T] {

    vs.foreach(_.isTail)

    def replicate(m: _Mutator)(implicit idRotator: Rotator[ID]) = {
      this.copy(
        vs.map(_.replicate(m))
      )
    }
  }
}
