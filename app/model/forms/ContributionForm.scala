package model.forms

case class ContributionForm(headline: String, body: String, location: Option[String], latitude: Option[BigDecimal], longitude: Option[BigDecimal])

