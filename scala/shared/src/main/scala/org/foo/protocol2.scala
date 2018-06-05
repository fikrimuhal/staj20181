package org.foo

import io.circe
import io.circe.parser.decode
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._


object protocol2 {

  sealed trait MyProtocol

  case class Message1(x: Int) extends MyProtocol

  case class Message2(xs: List[Message1]) extends MyProtocol

  case class Message3(maybeX: Option[Int]) extends MyProtocol

  def toJSON(msg: MyProtocol): Json = {
    msg.asJson
  }

  def fromJSON(msg: String): Either[circe.Error, MyProtocol] = {
    decode[MyProtocol](msg)
  }
}
