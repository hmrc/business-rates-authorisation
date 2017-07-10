/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package businessrates.authorisation

import businessrates.authorisation.models._
import org.joda.time.{DateTime, LocalDate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryDataGeneration {

  implicit def getArbitrary[T](t: Gen[T]): T = t.sample.get

  def randomShortString: Gen[String] = Gen.listOfN(20, Gen.alphaNumChar).map(_.mkString)
  def randomNumericString: Gen[String] = Gen.listOfN(20, Gen.numChar).map(_.mkString)
  def randomPositiveLong: Gen[Long] = Gen.choose(0L, Long.MaxValue)
  def randomDate: Gen[DateTime] = Gen.choose(0L, 32472144000000L /* 1/1/2999 */).map(new DateTime(_))
  def randomLocalDate: Gen[LocalDate] = Gen.choose(0L, 32472144000000L /* 1/1/2999 */).map(new DateTime(_).toLocalDate)

  def randomEmail: Gen[String] = for {
    mailbox <- randomShortString
    domain <- randomShortString
    tld <- randomShortString
  } yield s"$mailbox@$domain.$tld"

  def randomOrganisation: Gen[Organisation] = for {
    id <- arbitrary[Int]
    groupId <- randomShortString
    companyName <- randomShortString
    addressId <- arbitrary[Int]
    email <- randomEmail
    phone <- randomNumericString
    isAgent <- arbitrary[Boolean]
    agentCode <- randomPositiveLong
  } yield Organisation(id, groupId, companyName, addressId, email, phone, isAgent, agentCode)

  def randomPersonDetails: Gen[PersonDetails] = for {
    firstName <- randomShortString
    lastName <- randomShortString
    email <- randomEmail
    phone1 <- randomNumericString
    phone2 <- Gen.option(randomNumericString)
    addressId <- arbitrary[Int]
  } yield PersonDetails(firstName, lastName, email, phone1, phone2, addressId)

  def randomPerson: Gen[Person] = for {
    externalId <- randomShortString
    trustId <- randomShortString
    organisationId <- randomPositiveLong
    individualId <- randomPositiveLong
    details <- randomPersonDetails
  } yield Person(externalId, trustId, organisationId, individualId, details)

  def randomAssessment: Gen[Assessment] = for {
    assessmentRef <- randomPositiveLong
    listYear = "2017"
    uarn <- randomPositiveLong
    effectiveDate <- randomDate.map(_.toLocalDate)
  } yield Assessment(assessmentRef, listYear, uarn, effectiveDate)

  def randomPropertyLink: Gen[PropertyLink] = for {
    authorisationId <- randomPositiveLong
    uarn <- randomPositiveLong
    organisationId <- randomPositiveLong
    personId <- randomPositiveLong
    linkedDate <- randomLocalDate
    pending <- arbitrary[Boolean]
    assessment <- Gen.nonEmptyListOf(randomAssessment).retryUntil(_.size < 10)
    status <- Gen.oneOf("APPROVED", "PENDING", "REVOKED", "DECLINED")
  } yield PropertyLink(authorisationId, uarn, organisationId, personId, linkedDate, pending, assessment, Nil, status)

  def randomParty: Gen[Party] = for {
    permissions <- randomPermissions
    authorisedPartyStatus <- Gen.oneOf("APPROVED", "PENDING")
    organisationId <- randomPositiveLong
  } yield Party(permissions, authorisedPartyStatus, organisationId)

  def randomPermissions: Gen[Seq[Permission]] = for {
    checkPermission <- Gen.oneOf("START_AND_CONTINUE", "NOT_PERMITTED")
    challengePermission <- Gen.oneOf("START_AND_CONTINUE", "NOT_PERMITTED")
  } yield Seq(Permission(checkPermission, challengePermission, None))
}
