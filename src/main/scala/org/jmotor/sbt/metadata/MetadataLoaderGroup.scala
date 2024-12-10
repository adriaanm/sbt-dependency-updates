package org.jmotor.sbt.metadata

import org.apache.maven.artifact.versioning.ArtifactVersion
import org.jmotor.sbt.artifact.exception.ArtifactNotFoundException
import org.jmotor.sbt.artifact.metadata.MetadataLoader
import org.jmotor.sbt.artifact.metadata.loader.IvyPatternsMetadataLoader
import org.jmotor.sbt.concurrent.MultiFuture
import sbt.librarymanagement.{Binary, Constant, Disabled, Full, ModuleID, Patch}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import org.jmotor.sbt.artifact.metadata.loader.MavenRepoMetadataLoader

/**
 * Component: Description: Date: 2018/3/1
 *
 * @author
 *   AI
 */
class MetadataLoaderGroup(scalaVersion: String, scalaBinaryVersion: String, loaders: Seq[MetadataLoader]) {

  def getVersions(
    module: ModuleID,
    sbtBinaryVersion: Option[String] = None,
    sbtScalaBinaryVersion: Option[String] = None,
    newOrgIds: Map[String, String] = Map.empty
  ): Future[Seq[ArtifactVersion]] =
    firstCompletedOf(loaders.map { loader =>
      val (artifactId, attrs) = getArtifactIdAndAttrs(loader, module, sbtBinaryVersion, sbtScalaBinaryVersion)
      loader
        .getVersions(module.organization, artifactId, attrs)
        .flatMap(versions =>
          newOrgIds.get(module.organization).fold(Future.successful(versions)) { newOrgId =>
            loader.getVersions(newOrgId, artifactId, attrs).map(_ ++ versions)
          }
        )
    })

  private[metadata] def firstCompletedOf(
    futures: Seq[Future[Seq[ArtifactVersion]]]
  )(implicit executor: ExecutionContext): Future[Seq[ArtifactVersion]] =
    if (futures.lengthCompare(1) > 0) {
      val p           = Promise[Seq[ArtifactVersion]]()
      val multiFuture = new MultiFuture[Seq[ArtifactVersion]](p, futures.size, Seq.empty)
      futures foreach { future =>
        future.onComplete {
          case Success(r) if r.nonEmpty              => p trySuccess r
          case Success(_)                            => multiFuture.tryComplete()
          case Failure(_: ArtifactNotFoundException) => multiFuture.tryComplete()
          case Failure(t)                            => multiFuture.tryComplete(t)
        }(scala.concurrent.ExecutionContext.Implicits.global)
      }
      p.future
    } else futures.headOption.getOrElse(Future.successful(Seq.empty[ArtifactVersion]))

  private[metadata] def getArtifactIdAndAttrs(
    loader: MetadataLoader,
    module: ModuleID,
    sbtBinaryVersion: Option[String] = None,
    sbtScalaBinaryVersion: Option[String] = None
  ): (String, Map[String, String]) = {
    val remapVersion = module.crossVersion match {
      case _: Disabled        => None
      case binary: Binary     => Option(binary.prefix + scalaBinaryVersion)
      case _: Full            => Option(scalaVersion)
      case _: Patch           => Option(scalaVersion)
      case constant: Constant => Option(constant.value)
      case _                  => None
    }
    val name = remapVersion.map(v => s"${module.name}_$v").getOrElse(module.name)
    (sbtBinaryVersion, sbtScalaBinaryVersion) match {
      case (Some(sbtVersion), Some(scalaVersion)) =>
        loader match {
          case _: IvyPatternsMetadataLoader => (name, Map("sbtVersion" -> sbtVersion, "scalaVersion" -> scalaVersion))
          case _: MavenRepoMetadataLoader   => (s"${name}_${scalaVersion}_${sbtVersion}", Map.empty)
          case _                            => (name, Map.empty)
        }

      case _ => (name, Map.empty)
    }
  }

}

object MetadataLoaderGroup {

  def apply(scalaVersion: String, scalaBinaryVersion: String, loaders: MetadataLoader*): MetadataLoaderGroup =
    new MetadataLoaderGroup(scalaVersion, scalaBinaryVersion, loaders)

}
