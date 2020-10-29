package org.clulab.alignment.webapp

class ReindexMessage(val message: String) // This will just be "finished"

trait ReindexReceiver {
  def receive(reindexSender: ReindexSender, reindexMessage: ReindexMessage)
}

trait ReindexSender {
  // Provide access to something like the location index?
  // Put that into the message
}

//class ReindexCallback(reindexReceiver: ReindexReceiver, reindexMessage: ReindexMessage) {
//  def callback(reindexSender: ReindexSender): Unit = reindexReceiver.receive(reindexSender, reindexMessage)
//}

object Reindex {
  type ReindexCallback = ReindexSender => Unit

  val muteReindexCallback: ReindexCallback = (reindexSender: ReindexSender) => ()
}
