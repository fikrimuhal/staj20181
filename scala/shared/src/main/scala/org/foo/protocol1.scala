package org.foo

import java.nio.ByteBuffer

import boopickle.Default._

object protocol1 {
  sealed trait MyProtocol

  case class Message1(x: Int)              extends MyProtocol
  case class Message2(xs: List[Message1])  extends MyProtocol
  case class Message3(maybeX: Option[Int]) extends MyProtocol

  def fromBinary(msg: ByteBuffer): MyProtocol = {
    Unpickle.apply[MyProtocol].fromBytes(msg)
  }
  def toBinary(msg: MyProtocol): ByteBuffer = {
    Pickle.intoBytes(msg)
  }
}
