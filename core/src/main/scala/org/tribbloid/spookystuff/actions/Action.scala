package org.tribbloid.spookystuff.actions

import org.tribbloid.spookystuff.ActionException
import org.tribbloid.spookystuff.entity.{Page, PageRow}
import org.tribbloid.spookystuff.factory.PageBuilder
import org.tribbloid.spookystuff.utils.{Const, Utils}

import scala.concurrent.duration.Duration

/**
 * Created by peng on 04/06/14.
 */

object Action {

  def mayExport(actions: Seq[Action]): Boolean = {
    for (action <- actions) {
      if (action.mayExport()) return true
    }
    false
  }

  //  def snapshotNotOmitted(actions: Action*): Boolean = {
  //    if (actions.isEmpty) {
  //      true //indicating a dead action chain
  //    }
  //    else {
  //      mayExport(actions: _*)
  //    }
  //  }
}

/**
 * These are the same actions a human would do to get to the data page,
 * their order of execution is identical to that they are defined.
 * Many supports **Cell Interpolation**: you can embed cell reference in their constructor
 * by inserting keys enclosed by `#{}`, in execution they will be replaced with values they map to.
 * This is used almost exclusively in typing into an url bar or textbox, but it's flexible enough to be used anywhere.
 * extends Product to make sure all subclasses are case classes
 */
trait Action extends Serializable with Product {

  private var timeElapsed: Long = -1 //only set once

  //  val optional: Boolean

  final def interpolate(pr: PageRow): Option[this.type] = {
    val result = Option[this.type](this.doInterpolate(pr))
    result.foreach(_.inject(this))
    result
  }
  
  def doInterpolate(pageRow: PageRow): this.type = this //TODO: return Option as well

  def inject(same: this.type ): Unit = {
  }

  //this should handle autoSave, cache and errorDump
  def apply(session: PageBuilder): Seq[Page] = {

    val errorDump: Boolean = session.spooky.errorDump
    val errorDumpScreenshot: Boolean = session.spooky.errorDumpScreenshot

    val results = try {
      this match { //temporarily disabled as we assume that DFS is the culprit for causing deadlock
        case tt: Timed =>

          Utils.withDeadline(tt.hardTerminateTimeout(session)) {
            doExe(session)
          }
        case _ =>
          doExe(session)
      }
    }
    catch {
      case e: Throwable =>

        var isInBacktrace = false

        var message: String = ""

        message += session.backtrace.map{
          action =>{
            if (action eq this) {
              isInBacktrace = true
              "+>"+action.toString
            }
            else "| "+action.toString
          }
        }.mkString("\n")+"\n"

        if (!isInBacktrace) message += "+>" + this

        if (!this.isInstanceOf[Sessionless] && session.existingDriver.nonEmpty ) {
          if (errorDump) {
            var page = DefaultSnapshot.doExe(session).toList(0)
            try {
              page = page.errorDump(session.spooky)
              message += "\n"+"snapshot saved to: " + page.saved
            }
            catch {
              case e: Throwable =>
                try {
                  page = page.localErrorDump(session.spooky)
                  message += "\n"+"distributed file system inaccessible.........snapshot saved to: " + page.saved
                }
                catch {
                  case e: Throwable =>
                    message += "\n"+"all file systems inaccessible.........snapshot not saved"
                }
            }
          }
          if (errorDumpScreenshot) {
            var page = DefaultScreenshot.doExe(session).toList(0)
            try {
              page = page.errorDump(session.spooky)
              message += "\n"+"screenshot saved to: " + page.saved
            }
            catch {
              case e: Throwable =>
                try {
                  page = page.localErrorDump(session.spooky)
                  message += "\n"+"distributed file system inaccessible.........screenshot saved to: " + page.saved
                }
                catch {
                  case e: Throwable =>
                    message += "\n"+"all file systems inaccessible.........screenshot not saved"
                }
            }
          }
        }
        else{
          message += "\n"+"driver not initialize, snapshot/screenshot not available"
        }

        val ex = new ActionException(message, e)
        ex.setStackTrace(e.getStackTrace)
        throw ex
    }

    this.timeElapsed = System.currentTimeMillis() - session.startTime

    results
  }

  def doExe(session: PageBuilder): Seq[Page]

  //used to determine if snapshot needs to be appended or if possible to be executed lazily
  def mayExport(): Boolean

  //the minimal equivalent action that can be put into backtrace
  def trunk(): Option[Action]
}

trait Timed extends Action{

  private var _timeout: Duration = null

  //TODO: implement inject to enable it!
  def in(deadline: Duration): this.type = {
    this._timeout = deadline
    this
  }

  def timeout(session: PageBuilder): Duration = {
    val base = if (this._timeout == null) session.spooky.remoteResourceTimeout
    else this._timeout

    base
  }

  def hardTerminateTimeout(session: PageBuilder): Duration = timeout(session) + Const.hardTerminateOverhead

  override def inject(same: this.type): Unit = {
    super.inject(same)

    same match {
      case same: Timed =>
        this._timeout = same._timeout
    }
  }
}

trait Named extends Action {

  private var _name: String = "-" //can only set once

  def as(name: String): this.type = if (name != "-" && !this.mayExport)
    throw new UnsupportedOperationException("cannot set name for action with no export")
  else if (name == null)
    throw new UnsupportedOperationException("cannot set name = null")
  else {
    this._name = name
    this
  }

  def name: String = _name

  override def inject(same: this.type): Unit = {
    super.inject(same)

    same match {
      case same: Named =>
        this._name = same.name
    }
  }
}

trait Sessionless extends Action