package com.tribbloids.spookystuff.graph

trait Algebra[T <: Domain] extends Algebra.TypeSugars[T] {

  implicit def algebra: Algebra[T] = this

  def idAlgebra: IDAlgebra[ID, NodeData, EdgeData]
  def nodeAlgebra: DataAlgebra[NodeData]
  def edgeAlgebra: DataAlgebra[EdgeData]

  trait _Sugars extends Algebra.Sugars[T] {
    override implicit val algebra = Algebra.this
  }
  object _Sugars extends _Sugars

  //  case class Dangling(info: Option[NodeInfo] = None) extends NodeLike
  object DANGLING extends Element.Node[T](nodeAlgebra.eye, idAlgebra.DANGLING) with _Sugars {

    assert(isDangling)

    override def toString = s"$idStr"
  }

  def createNode(
      info: NodeData = nodeAlgebra.eye,
      id: Option[ID] = None
  ): _Node = {
    val _id = id.getOrElse {
      Algebra.this.idAlgebra.fromNodeData(info)
    }

    if (_id != idAlgebra.DANGLING)
      Element.Node[T](info, _id)
    else
      DANGLING
  }

  def createEdge(
      info: EdgeData = edgeAlgebra.eye,
      qualifier: Seq[Any] = Nil,
      from_to: Option[(ID, ID)] = None
  ): _Edge = {

    val _from_to = from_to.getOrElse {
      idAlgebra.DANGLING -> idAlgebra.DANGLING
    }

    Element.Edge[T](info, qualifier, _from_to)
  }
}

object Algebra {

  trait TypeSugars[T <: Domain] {

    final type ID = T#ID
    final type NodeData = T#NodeData
    final type EdgeData = T#EdgeData

    final type _Mutator = Mutator[T]
    final type _Rotator = IDAlgebra.Rotator[ID]

    final type _Module = Module[T]
    final type _Heads = Module.Heads[T]
    final type _Tails = Module.Tails[T]

    final type _Element = Element[T]

    final type _NodeLike = Element.NodeLike[T]
    final type _Node = Element.Node[T]
    final type _LinkedNode = Element.LinkedNode[T]

    final type _Edge = Element.Edge[T]

    final type _ShowFormat = Visualisation.Format[T]
    final def _ShowFormat = Visualisation.Format
  }

  trait Sugars[T <: Domain] extends TypeSugars[T] {

    implicit def algebra: Algebra[T]

    def idAlgebra: IDAlgebra[ID, NodeData, EdgeData] = algebra.idAlgebra
    def nodeAlgebra: DataAlgebra[NodeData] = algebra.nodeAlgebra
    def edgeAlgebra: DataAlgebra[EdgeData] = algebra.edgeAlgebra
  }
}
