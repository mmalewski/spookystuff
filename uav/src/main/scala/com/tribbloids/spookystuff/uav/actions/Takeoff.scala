package com.tribbloids.spookystuff.uav.actions

import com.tribbloids.spookystuff.SpookyContext
import com.tribbloids.spookystuff.actions.Trace
import com.tribbloids.spookystuff.extractors.Col
import com.tribbloids.spookystuff.session.Session
import com.tribbloids.spookystuff.uav.UAVConf
import com.tribbloids.spookystuff.uav.actions.mixin.HasStartEndLocations
import com.tribbloids.spookystuff.uav.spatial.{Anchors, Location, NED}
import org.slf4j.LoggerFactory

/**
  * won't do a thing if already in the air.
  */
case class Takeoff(
                    altitude: Col[Double] = 0.0
                  ) extends UAVNavigation {

  override def getSessionView(session: Session) = new this.SessionView(session)

  implicit class SessionView(session: Session) extends super.SessionView(session) {

    val minAlt = getMinAlt(uavConf)

    override def engage(): Unit = {

      LoggerFactory.getLogger(this.getClass)
        .info(s"taking off and climbing to $minAlt")

      link.synch.clearanceAlt(minAlt)
    }
  }

  def getMinAlt(uavConf: UAVConf) = {
    if (altitude.value > 0) altitude.value
    else uavConf.clearanceAltitudeMin
  }

  override def getStart(trace: Trace, spooky: SpookyContext) = {
    val uavConf = spooky.getConf[UAVConf]
    val minAlt = getMinAlt(uavConf)

    def fallbackLocation = Location.fromTuple(NED.V(0,0,-minAlt) -> Anchors.HomeLevelProjection)

    val hasStartEndLocations = trace.collect {
      case v: HasStartEndLocations => v
    }
    val i = hasStartEndLocations.indexWhere(_ eq this)
    if (i < 0) throw new UnsupportedOperationException(s"$this is not in the trace")
    else if (i == 0) fallbackLocation
    else {
      val previous = hasStartEndLocations(i - 1)
      val previousLocaion = previous.getEnd(trace, spooky)
      val previousCoordOpt = previousLocaion.getCoordinate(NED, uavConf.home)
      val result = previousCoordOpt match {
        case Some(coord) =>
          val alt = -coord.down
          val objAlt = Math.max(alt, minAlt)
          Location.fromTuple(coord.copy(down = -objAlt) -> uavConf.home)
        case None =>
          fallbackLocation
      }
      result
    }
  }
}
