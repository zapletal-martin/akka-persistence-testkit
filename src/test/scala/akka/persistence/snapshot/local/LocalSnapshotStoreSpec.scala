package akka.persistence.snapshot.local

import akka.persistence.PluginCleanup
import akka.persistence.snapshot.SnapshotStoreSpec

import com.typesafe.config.ConfigFactory

class LocalSnapshotStoreSpec extends SnapshotStoreSpec with PluginCleanup {
  lazy val config = ConfigFactory.parseString(
    """
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/snapshots"
    """.stripMargin)

}
