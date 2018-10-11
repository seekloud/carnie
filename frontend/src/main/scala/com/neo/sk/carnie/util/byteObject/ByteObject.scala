package com.neo.sk.carnie.util.byteObject

import com.neo.sk.carnie.util.byteObject.decoder.{BytesDecoder, DecoderFailure}
import com.neo.sk.carnie.util.byteObject.encoder.BytesEncoder
import org.seekloud.byteobject.MiddleBuffer

/**
  * User: Taoz
  * Date: 7/16/2018
  * Time: 10:47 AM
  */
object ByteObject {


  implicit final class EncoderOps[A](val wrappedEncodeable: A) extends AnyVal {
    final def fillMiddleBuffer[B <: MiddleBuffer](
      buffer: B
    )(implicit encoder: BytesEncoder[A]): B = {
      buffer.clear()
      encoder.encode(wrappedEncodeable, buffer)
      buffer
    }
  }

  def bytesDecode[A](input: MiddleBuffer)(implicit decoder: BytesDecoder[A]): Either[DecoderFailure, A] = {
    decoder.decode(input)
  }


}
