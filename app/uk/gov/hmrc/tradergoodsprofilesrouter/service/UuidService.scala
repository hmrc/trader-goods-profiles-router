package uk.gov.hmrc.tradergoodsprofilesrouter.service

import java.util.UUID

import com.google.inject.Singleton

@Singleton
class UuidService {

  def uuid(): String = UUID.randomUUID().toString

}