package businessrates.authorisation.stubs

import play.api.libs.json.{JsArray, JsObject, Json}

object AuthStub {
  val testExternalId = "testExternalId"
  val testGroupId = "testGroupId"
  val testAffinityGroup = "Individual"

  def hmrcVoaCcaEnrolment(personId: Long): JsObject =
    Json.obj(
      "key" -> "HMRC-VOA-CCA",
      "identifiers" -> Json.arr(
        Json.obj(
          "key"   -> "VOAPersonID",
          "value" -> personId.toString
        )
      )
    )

  def authResponseBody(enrolments: JsObject*): JsObject = Json.obj(
    "externalId"           -> testExternalId,
    "groupIdentifier"      -> testGroupId,
    "affinityGroup"        -> testAffinityGroup,
    "authorisedEnrolments" -> JsArray(enrolments)
  )

}
