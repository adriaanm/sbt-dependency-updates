package org.jmotor.sbt.plugin

import org.jmotor.sbt.plugin.ComponentSorter.ComponentSorter
import sbt._

/**
 * Component: Description: Date: 2018/2/27
 *
 * @author
 *   AI
 */
trait DependencyUpdatesKeys {

  lazy val dependencyUpdates: TaskKey[Unit] = taskKey[Unit]("Check for updates")

  lazy val dependencyUpgrade: TaskKey[Unit] = taskKey[Unit]("Check for updates and upgrade [Experimental]")

  lazy val dependencyUpgradeComponentSorter: SettingKey[ComponentSorter] =
    settingKey[ComponentSorter]("Component sorter")

  lazy val dependencyUpgradeModuleNames: SettingKey[Map[String, String]] =
    settingKey[Map[String, String]]("Module name mappings")

  lazy val dependencyUpdatesNewOrgIds: SettingKey[Map[String, String]] =
    settingKey[Map[String, String]](
      "Map from old to new organization id, simply collects versions of artifacts from both orgs, does not rewrite the actual dependency"
    )

}

object DependencyUpdatesKeys extends DependencyUpdatesKeys
