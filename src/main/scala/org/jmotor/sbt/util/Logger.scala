package org.jmotor.sbt.util

import fansi.Color._
import org.apache.ivy.util.StringUtils
import org.jmotor.sbt.model.Status._
import org.jmotor.sbt.model.{ ModuleStatus, Status }

/**
 * Component:
 * Description:
 * Date: 2016/12/24
 *
 * @author AI
 */
object Logger {

  def log(module: ModuleStatus): Unit = {
    val status = module.status
    val (color, message) = status match {
      case Expired    ⇒ Yellow → s"${Blue("--->")} ${Red(module.lastVersion)}"
      case Unreleased ⇒ Yellow → s"${Blue("--->")} ${Red(module.lastVersion)}"
      case Success    ⇒ Green → Green("√")
      case Error      ⇒ Red → module.error.getOrElse("updates error, please retry!")
      case NotFound   ⇒ Red → Red("×")
      case s          ⇒ Red → Red(s"unknown status ${s.toString}")
    }
    val length = Status.values.foldLeft(0) { (l, s) ⇒
      val length = s.toString.length
      if (length > l) {
        length
      } else {
        l
      }
    }
    val level = status.toString + StringUtils.repeat(" ", length - status.toString.length)
    print(s"[${color(level)}] ${module.raw} $message \n")
  }

}
