package it.rfmariano.nstates.data.api

import it.rfmariano.nstates.data.model.DeathCause
import it.rfmariano.nstates.data.model.Deaths
import it.rfmariano.nstates.data.model.Freedom
import it.rfmariano.nstates.data.model.Government
import it.rfmariano.nstates.data.model.NationData
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses NationStates API XML responses into [NationData] using Android's built-in
 * [XmlPullParser]. This avoids Jackson XML / StAX, which are not available on Android.
 */
@Singleton
class NationXmlParser @Inject constructor() {

    fun parse(xml: String): NationData {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(replaceHtmlEntities(xml)))

        var name = ""
        var fullName = ""
        var type = ""
        var motto = ""
        var category = ""
        var region = ""
        var flagUrl = ""
        var population = 0L
        var currency = ""
        var animal = ""
        var leader = ""
        var capital = ""
        var founded = ""
        var lastActivity = ""
        var influence = ""
        var tax = 0.0
        var gdp = 0L
        var income = 0L
        var poorest = 0L
        var richest = 0L
        var majorIndustry = ""
        var crime = ""
        var sensibilities = ""
        var govtDescription = ""
        var industryDescription = ""
        var waStatus = ""
        var endorsements = ""
        var freedom = Freedom()
        var government = Government()
        var deaths = Deaths()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "NAME" -> name = parser.nextText()
                    "FULLNAME" -> fullName = parser.nextText()
                    "TYPE" -> type = parser.nextText()
                    "MOTTO" -> motto = parser.nextText()
                    "CATEGORY" -> category = parser.nextText()
                    "REGION" -> region = parser.nextText()
                    "FLAG" -> flagUrl = parser.nextText()
                    "POPULATION" -> population = parser.nextText().toLongOrNull() ?: 0L
                    "CURRENCY" -> currency = parser.nextText()
                    "ANIMAL" -> animal = parser.nextText()
                    "LEADER" -> leader = parser.nextText()
                    "CAPITAL" -> capital = parser.nextText()
                    "FOUNDED" -> founded = parser.nextText()
                    "LASTACTIVITY" -> lastActivity = parser.nextText()
                    "INFLUENCE" -> influence = parser.nextText()
                    "TAX" -> tax = parser.nextText().toDoubleOrNull() ?: 0.0
                    "GDP" -> gdp = parser.nextText().toLongOrNull() ?: 0L
                    "INCOME" -> income = parser.nextText().toLongOrNull() ?: 0L
                    "POOREST" -> poorest = parser.nextText().toLongOrNull() ?: 0L
                    "RICHEST" -> richest = parser.nextText().toLongOrNull() ?: 0L
                    "MAJORINDUSTRY" -> majorIndustry = parser.nextText()
                    "CRIME" -> crime = parser.nextText()
                    "SENSIBILITIES" -> sensibilities = parser.nextText()
                    "GOVTDESC" -> govtDescription = parser.nextText()
                    "INDUSTRYDESC" -> industryDescription = parser.nextText()
                    "WA" -> waStatus = parser.nextText()
                    "ENDORSEMENTS" -> endorsements = parser.nextText()
                    "FREEDOM" -> freedom = parseFreedom(parser)
                    "GOVT" -> government = parseGovernment(parser)
                    "DEATHS" -> deaths = parseDeaths(parser)
                }
            }
            eventType = parser.next()
        }

        return NationData(
            name = name,
            fullName = fullName,
            type = type,
            motto = motto,
            category = category,
            region = region,
            flagUrl = flagUrl,
            population = population,
            currency = currency,
            animal = animal,
            leader = leader,
            capital = capital,
            founded = founded,
            lastActivity = lastActivity,
            influence = influence,
            tax = tax,
            gdp = gdp,
            income = income,
            poorest = poorest,
            richest = richest,
            majorIndustry = majorIndustry,
            crime = crime,
            sensibilities = sensibilities,
            govtDescription = govtDescription,
            industryDescription = industryDescription,
            waStatus = waStatus,
            endorsements = endorsements,
            freedom = freedom,
            government = government,
            deaths = deaths
        )
    }

    private fun parseFreedom(parser: XmlPullParser): Freedom {
        var civilRights = ""
        var economy = ""
        var politicalFreedom = ""

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "CIVILRIGHTS" -> civilRights = parser.nextText()
                    "ECONOMY" -> economy = parser.nextText()
                    "POLITICALFREEDOM" -> politicalFreedom = parser.nextText()
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "FREEDOM") {
                break
            }
            eventType = parser.next()
        }

        return Freedom(
            civilRights = civilRights,
            economy = economy,
            politicalFreedom = politicalFreedom
        )
    }

    private fun parseGovernment(parser: XmlPullParser): Government {
        var administration = 0.0
        var defence = 0.0
        var education = 0.0
        var environment = 0.0
        var healthcare = 0.0
        var commerce = 0.0
        var internationalAid = 0.0
        var lawAndOrder = 0.0
        var publicTransport = 0.0
        var socialEquality = 0.0
        var spirituality = 0.0
        var welfare = 0.0

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "ADMINISTRATION" -> administration = parser.nextText().toDoubleOrNull() ?: 0.0
                    "DEFENCE" -> defence = parser.nextText().toDoubleOrNull() ?: 0.0
                    "EDUCATION" -> education = parser.nextText().toDoubleOrNull() ?: 0.0
                    "ENVIRONMENT" -> environment = parser.nextText().toDoubleOrNull() ?: 0.0
                    "HEALTHCARE" -> healthcare = parser.nextText().toDoubleOrNull() ?: 0.0
                    "COMMERCE" -> commerce = parser.nextText().toDoubleOrNull() ?: 0.0
                    "INTERNATIONALAID" -> internationalAid = parser.nextText().toDoubleOrNull() ?: 0.0
                    "LAWANDORDER" -> lawAndOrder = parser.nextText().toDoubleOrNull() ?: 0.0
                    "PUBLICTRANSPORT" -> publicTransport = parser.nextText().toDoubleOrNull() ?: 0.0
                    "SOCIALEQUALITY" -> socialEquality = parser.nextText().toDoubleOrNull() ?: 0.0
                    "SPIRITUALITY" -> spirituality = parser.nextText().toDoubleOrNull() ?: 0.0
                    "WELFARE" -> welfare = parser.nextText().toDoubleOrNull() ?: 0.0
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "GOVT") {
                break
            }
            eventType = parser.next()
        }

        return Government(
            administration = administration,
            defence = defence,
            education = education,
            environment = environment,
            healthcare = healthcare,
            commerce = commerce,
            internationalAid = internationalAid,
            lawAndOrder = lawAndOrder,
            publicTransport = publicTransport,
            socialEquality = socialEquality,
            spirituality = spirituality,
            welfare = welfare
        )
    }

    private fun parseDeaths(parser: XmlPullParser): Deaths {
        val causes = mutableListOf<DeathCause>()

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "CAUSE") {
                val causeType = parser.getAttributeValue(null, "type") ?: ""
                val percentage = parser.nextText().toDoubleOrNull() ?: 0.0
                causes.add(DeathCause(type = causeType, percentage = percentage))
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "DEATHS") {
                break
            }
            eventType = parser.next()
        }

        return Deaths(causes = causes)
    }
}
