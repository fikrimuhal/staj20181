package org.foo

import java.nio.ByteBuffer

import boopickle.Default._
import io.circe
import protocol1._

object Main {
  def main(args: Array[String]): Unit = {
    println("Hwllo from JVM!")
    /*val msg1 = protocol1.Message1(5)
    //assert(msg.x == Unpickle[Message1].fromBytes(Pickle.intoBytes(msg)).x)
    val msg11: ByteBuffer          = toBinary(msg1)
    val msg12: protocol1.MyProtocol = protocol1.fromBinary(msg11)
    assert(msg1.x == msg12.asInstanceOf[protocol1.Message1].x)
    val msg2                = protocol1.Message2(List(msg1, msg1))
    val l: List[ByteBuffer] = (msg1 :: msg2 :: Nil) map toBinary
    val ll                  = l map fromBinary
    ll foreach {
      case m: Message1            => println(m)
      case Message2(List(s1))     => println("Ayni")
      case Message2(List(`msg1`)) => println("Ayni")
      case Message2(List(s1, s2)) => println("Farkli")
      case Message3(Some(x))      => println("Ayni")
      case Message3(maybeX)       => println("Ayni")
      case Message3(_)            => println("Ayni")
      case unknownMessage         => println(unknownMessage)
    }*/
    val msg1 = protocol2.Message1(5)
    val msg1_json: String = protocol2.toJSON(msg1).noSpaces
    val msg11: Either[circe.Error, protocol2.MyProtocol] = protocol2.fromJSON(msg1_json)
    msg11 match {
      case Left(x) => println("Error during fromJSON")
      case Right(x) => assert(x.asInstanceOf[protocol2.Message1].x == msg1.x)
    }
  }
}
