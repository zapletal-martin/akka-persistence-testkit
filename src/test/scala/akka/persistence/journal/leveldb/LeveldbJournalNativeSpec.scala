package akka.persistence.journal.leveldb

import akka.persistence.journal.JournalSpec
import akka.persistence.PluginCleanup

import com.typesafe.config.ConfigFactory

class LeveldbJournalNativeSpec extends JournalSpec with PluginCleanup {
  lazy val config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
      |akka.persistence.journal.leveldb.native = on
      |akka.persistence.journal.leveldb.dir = "target/journal-native"
      |akka.persistence.snapshot-store.local.dir = "target/snapshots-native/"
    """.stripMargin)
}
