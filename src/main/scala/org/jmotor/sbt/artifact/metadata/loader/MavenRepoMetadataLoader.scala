package org.jmotor.sbt.artifact.metadata.loader

import org.apache.maven.artifact.versioning.{ArtifactVersion, DefaultArtifactVersion}
import org.jmotor.sbt.artifact.exception.ArtifactNotFoundException
import org.jmotor.sbt.artifact.metadata.MetadataLoader

import java.net.URI
import java.nio.file.{Files, Paths}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.XML

case class LoaderCredentials(realm: String, userName: String, password: String)

/**
 * Component: Description: Date: 2018/2/8
 *
 * @author
 *   AI
 */
class MavenRepoMetadataLoader(uri: URI, credentials: Option[LoaderCredentials])(implicit ec: ExecutionContext)
    extends MetadataLoader {

  private[this] lazy val (protocol, base) =
    (uri.getScheme + "://" -> uri.getRawSchemeSpecificPart.stripPrefix("//"))

  override def getVersions(
    organization: String,
    artifactId: String,
    attrs: Map[String, String]
  ): Future[Seq[ArtifactVersion]] = {
    val location = new URI(s"$protocol$base/${organization.split('.').mkString("/")}/$artifactId/maven-metadata.xml")
      .normalize()
      .toString()
    download(organization, artifactId, location, credentials).map { file =>
      val stream = Files.newInputStream(file)
      try {
        val xml = XML.load(stream)
        xml \ "versioning" \ "versions" \ "version" map (node => new DefaultArtifactVersion(node.text))
      } catch {
        case _: Throwable => throw ArtifactNotFoundException(organization, artifactId)
      } finally {
        stream.close()
      }
    }
  }

}
