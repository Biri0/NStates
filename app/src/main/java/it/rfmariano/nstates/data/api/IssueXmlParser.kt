package it.rfmariano.nstates.data.api

import it.rfmariano.nstates.data.model.Issue
import it.rfmariano.nstates.data.model.IssueOption
import it.rfmariano.nstates.data.model.IssueResult
import it.rfmariano.nstates.data.model.IssuesData
import it.rfmariano.nstates.data.model.RankingChange
import it.rfmariano.nstates.data.model.Reclassification
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses NationStates API XML responses for issues-related endpoints.
 *
 * Handles:
 * - Issues list (private shard "issues" + "nextissuetime")
 * - Issue answer result (private command "issue")
 */
@Singleton
class IssueXmlParser @Inject constructor() {

    /**
     * Parse the response from `q=issues+nextissuetime`.
     */
    fun parseIssues(xml: String): IssuesData {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(replaceHtmlEntities(xml)))

        val issues = mutableListOf<Issue>()
        var nextIssueTime = 0L

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "ISSUE" -> {
                        issues.add(parseIssue(parser))
                    }
                    "NEXTISSUETIME" -> {
                        nextIssueTime = parser.nextText().toLongOrNull() ?: 0L
                    }
                }
            }
            eventType = parser.next()
        }

        return IssuesData(
            issues = issues,
            nextIssueTime = nextIssueTime
        )
    }

    private fun parseIssue(parser: XmlPullParser): Issue {
        val issueId = parser.getAttributeValue(null, "id")?.toIntOrNull() ?: 0
        var title = ""
        var text = ""
        var author = ""
        var editor = ""
        var pic1 = ""
        var pic2 = ""
        val options = mutableListOf<IssueOption>()

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "TITLE" -> title = parser.nextText()
                    "TEXT" -> text = parser.nextText()
                    "AUTHOR" -> author = parser.nextText()
                    "EDITOR" -> editor = parser.nextText()
                    "PIC1" -> pic1 = parser.nextText()
                    "PIC2" -> pic2 = parser.nextText()
                    "OPTION" -> {
                        val optionId = parser.getAttributeValue(null, "id")?.toIntOrNull() ?: 0
                        val optionText = parser.nextText()
                        options.add(IssueOption(id = optionId, text = optionText))
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "ISSUE") {
                break
            }
            eventType = parser.next()
        }

        return Issue(
            id = issueId,
            title = title,
            text = text,
            author = author,
            editor = editor,
            pic1 = pic1,
            pic2 = pic2,
            options = options
        )
    }

    /**
     * Parse the response from the `c=issue` command.
     *
     * Expected XML structure:
     * ```xml
     * <NATION ...>
     *   <ISSUE id="123" choice="0">
     *     <OK>1</OK>
     *     <DESC>The legislation was enacted.</DESC>
     *     <RANKINGS>
     *       <RANK id="0">
     *         <SCORE>42.5</SCORE>
     *         <CHANGE>1.5</CHANGE>
     *         <PCHANGE>3.66</PCHANGE>
     *       </RANK>
     *     </RANKINGS>
     *     <RECLASSIFICATIONS>
     *       <RECLASSIFY type="category">
     *         <FROM>Left-Leaning College State</FROM>
     *         <TO>Inoffensive Centrist Democracy</TO>
     *       </RECLASSIFY>
     *     </RECLASSIFICATIONS>
     *     <NEW_POLICIES>
     *       <POLICY>Policy Name</POLICY>
     *     </NEW_POLICIES>
     *     <REMOVED_POLICIES>
     *       <POLICY>Policy Name</POLICY>
     *     </REMOVED_POLICIES>
     *     <UNLOCKS>
     *       <UNLOCK>banner_code</UNLOCK>
     *     </UNLOCKS>
     *   </ISSUE>
     * </NATION>
     * ```
     */
    fun parseIssueResult(xml: String): IssueResult {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(replaceHtmlEntities(xml)))

        var ok = false
        var error = ""
        var description = ""
        val rankings = mutableListOf<RankingChange>()
        val reclassifications = mutableListOf<Reclassification>()
        val newPolicies = mutableListOf<String>()
        val removedPolicies = mutableListOf<String>()
        val unlocks = mutableListOf<String>()

        var inIssue = false
        var inRankings = false
        var inReclassifications = false
        var inNewPolicies = false
        var inRemovedPolicies = false
        var inUnlocks = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "ISSUE" -> inIssue = true
                    "OK" -> if (inIssue) ok = true.also { parser.nextText() }
                    "ERROR" -> if (inIssue) error = parser.nextText()
                    "DESC" -> if (inIssue) description = parser.nextText()
                    "RANKINGS" -> if (inIssue) inRankings = true
                    "RANK" -> if (inRankings) {
                        rankings.add(parseRankingChange(parser))
                    }
                    "RECLASSIFICATIONS" -> if (inIssue) inReclassifications = true
                    "RECLASSIFY" -> if (inReclassifications) {
                        reclassifications.add(parseReclassification(parser))
                    }
                    "NEW_POLICIES" -> if (inIssue) inNewPolicies = true
                    "REMOVED_POLICIES" -> if (inIssue) inRemovedPolicies = true
                    "POLICY" -> {
                        val policyName = parser.nextText()
                        when {
                            inNewPolicies -> newPolicies.add(policyName)
                            inRemovedPolicies -> removedPolicies.add(policyName)
                        }
                    }
                    "UNLOCKS" -> if (inIssue) inUnlocks = true
                    "UNLOCK" -> if (inUnlocks) unlocks.add(parser.nextText())
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                when (parser.name) {
                    "ISSUE" -> inIssue = false
                    "RANKINGS" -> inRankings = false
                    "RECLASSIFICATIONS" -> inReclassifications = false
                    "NEW_POLICIES" -> inNewPolicies = false
                    "REMOVED_POLICIES" -> inRemovedPolicies = false
                    "UNLOCKS" -> inUnlocks = false
                }
            }
            eventType = parser.next()
        }

        return IssueResult(
            ok = ok,
            error = error,
            description = description,
            rankings = rankings,
            reclassifications = reclassifications,
            newPolicies = newPolicies,
            removedPolicies = removedPolicies,
            unlocks = unlocks
        )
    }

    private fun parseRankingChange(parser: XmlPullParser): RankingChange {
        val id = parser.getAttributeValue(null, "id")?.toIntOrNull() ?: 0
        var score = 0.0
        var change = 0.0
        var pchange = 0.0

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "SCORE" -> score = parser.nextText().toDoubleOrNull() ?: 0.0
                    "CHANGE" -> change = parser.nextText().toDoubleOrNull() ?: 0.0
                    "PCHANGE" -> pchange = parser.nextText().toDoubleOrNull() ?: 0.0
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "RANK") {
                break
            }
            eventType = parser.next()
        }

        return RankingChange(
            id = id,
            name = CensusScales.nameFor(id),
            scoreAfter = score,
            scoreBefore = score - change,
            percentageChange = pchange
        )
    }

    private fun parseReclassification(parser: XmlPullParser): Reclassification {
        val type = parser.getAttributeValue(null, "type") ?: ""
        var from = ""
        var to = ""

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "FROM" -> from = parser.nextText()
                    "TO" -> to = parser.nextText()
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "RECLASSIFY") {
                break
            }
            eventType = parser.next()
        }

        return Reclassification(
            type = type,
            from = from,
            to = to
        )
    }
}
