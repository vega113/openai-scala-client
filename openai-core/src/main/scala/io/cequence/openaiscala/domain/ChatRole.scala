package io.cequence.openaiscala.domain

import io.cequence.wsclient.domain.EnumValue

sealed trait ChatRole extends EnumValue {
  override def toString: String = super.toString.toLowerCase
}

object ChatRole {
  case object User extends ChatRole
  case object System extends ChatRole
  case object Assistant extends ChatRole
  @Deprecated
  case object Function extends ChatRole
  case object Tool extends ChatRole
}
