package akka.persistence.journal

import akka.actor._
import akka.persistence._
import akka.persistence.JournalProtocol._
import akka.testkit._

import com.typesafe.config._

object JournalSpec {
  val config = ConfigFactory.parseString(
    """
      |akka.persistence.publish-confirmations = on
      |akka.persistence.publish-plugin-commands = on
    """.stripMargin)
}

trait JournalSpec extends PluginSpec {
  implicit lazy val system = ActorSystem("JournalSpec", config.withFallback(JournalSpec.config))

  private var _senderProbe: TestProbe = _
  private var _receiverProbe: TestProbe = _

  private val manifest = ""
  private val writerUuid = ""

  def senderProbe: TestProbe = _senderProbe
  def receiverProbe: TestProbe = _receiverProbe

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    _senderProbe = TestProbe()
    _receiverProbe = TestProbe()
    writeMessages(1, 5, pid, senderProbe.ref)
  }

  def journal: ActorRef =
    extension.journalFor(null)

  def messagePayload(snr: Long): Any = s"a-$snr"
  
  def replayedMessage(snr: Long, deleted: Boolean = false): ReplayedMessage =
    ReplayedMessage(PersistentImpl(messagePayload(snr), snr, pid, manifest, deleted, null, writerUuid))

  def writeMessages(from: Int, to: Int, pid: String, sender: ActorRef): Unit = {
    val msgs = from to to map { i => AtomicWrite(PersistentRepr(payload = messagePayload(i), sequenceNr = i, persistenceId = pid, sender = sender)) }
    val probe = TestProbe()

    journal ! WriteMessages(msgs, probe.ref, 1)

    probe.expectMsg(WriteMessagesSuccessful)
    from to to foreach { i =>
      probe.expectMsgPF() { case WriteMessageSuccess(PersistentImpl(payload, `i`, `pid`, _, _, `sender`, _), 1) => payload should equal (messagePayload(i)) }
    }
  }

  "A journal" must {
    "replay all messages" in {
      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      1 to 5 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "replay messages using a lower sequence number bound" in {
      journal ! ReplayMessages(3, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      3 to 5 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "replay messages using an upper sequence number bound" in {
      journal ! ReplayMessages(1, 3, Long.MaxValue, pid, receiverProbe.ref)
      1 to 3 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "replay messages using a count limit" in {
      journal ! ReplayMessages(1, Long.MaxValue, 3, pid, receiverProbe.ref)
      1 to 3 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "replay messages using a lower and upper sequence number bound" in {
      journal ! ReplayMessages(2, 4, Long.MaxValue, pid, receiverProbe.ref)
      2 to 4 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "replay messages using a lower and upper sequence number bound and a count limit" in {
      journal ! ReplayMessages(2, 4, 2, pid, receiverProbe.ref)
      2 to 3 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "replay a single if lower sequence number bound equals upper sequence number bound" in {
      journal ! ReplayMessages(2, 2, Long.MaxValue, pid, receiverProbe.ref)
      2 to 2 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "replay a single message if count limit equals 1" in {
      journal ! ReplayMessages(2, 4, 1, pid, receiverProbe.ref)
      2 to 2 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "not replay messages if count limit equals 0" in {
      journal ! ReplayMessages(2, 4, 0, pid, receiverProbe.ref)
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "not replay messages if lower  sequence number bound is greater than upper sequence number bound" in {
      journal ! ReplayMessages(3, 2, Long.MaxValue, pid, receiverProbe.ref)
      receiverProbe.expectMsg(RecoverySuccess(5))
    }
    "not replay permanently deleted messages (range deletion)" in {
      val cmd = DeleteMessagesTo(pid, 3, receiverProbe.ref)
      val sub = TestProbe()

      subscribe[DeleteMessagesTo](sub.ref)
      journal ! cmd
      sub.expectMsg(cmd)

      receiverProbe.expectMsg(DeleteMessagesSuccess(3))
      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      List(4, 5) foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
    }

    // TODO: The below is not longer supported
    /*
    "replay logically deleted messages with deleted field set to true (range deletion)" in {
      val cmd = DeleteMessagesTo(pid, 3, receiverProbe.ref)
      val sub = TestProbe()

      subscribe[DeleteMessagesTo](sub.ref)
      journal ! cmd
      sub.expectMsg(cmd)

      receiverProbe.expectMsg(DeleteMessagesSuccess(3))
      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      1 to 5 foreach { i =>
        i match {
          case 1 | 2 | 3 => receiverProbe.expectMsg(replayedMessage(i, deleted = true))
          case 4 | 5     => receiverProbe.expectMsg(replayedMessage(i))
        }
      }
    }

    "return a highest stored sequence number > 0 if the processor has already written messages and the message log is non-empty" in {
      journal ! ReadHighestSequenceNr(3L, pid, receiverProbe.ref)
      receiverProbe.expectMsg(ReadHighestSequenceNrSuccess(5))

      journal ! ReadHighestSequenceNr(5L, pid, receiverProbe.ref)
      receiverProbe.expectMsg(ReadHighestSequenceNrSuccess(5))
    }
    "return a highest stored sequence number == 0 if the processor has not yet written messages" in {
      journal ! ReadHighestSequenceNr(0L, "non-existing-pid", receiverProbe.ref)
      receiverProbe.expectMsg(ReadHighestSequenceNrSuccess(0))
    }
    */
  }
}
