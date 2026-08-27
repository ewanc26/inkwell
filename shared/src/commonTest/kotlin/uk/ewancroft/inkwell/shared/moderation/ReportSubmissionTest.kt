package uk.ewancroft.inkwell.shared.moderation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReportSubmissionTest {

    @Test
    fun standardReasonsUseFullyQualifiedLexiconTokens() {
        assertEquals(
            "com.atproto.moderation.defs#reasonSpam",
            ReportReasonType.Spam.wireValue,
        )
        assertEquals(
            "com.atproto.moderation.defs#reasonRude",
            ReportReasonType.Rude.wireValue,
        )
        assertEquals(
            "com.atproto.moderation.defs#reasonOther",
            ReportReasonType.Other.wireValue,
        )
    }

    @Test
    fun classifiesAtUriSubjectsAsRecords() {
        val submission = ReportSubmission(
            subject = "at://did:plc:alice/site.standard.document/3abc",
            reasonType = ReportReasonType.Misleading,
            recordCid = "bafyreiaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        )

        assertEquals(ReportSubjectKind.Record, submission.subjectKind)
    }

    @Test
    fun classifiesDidSubjectsAsAccounts() {
        val submission = ReportSubmission(
            subject = "did:plc:alice",
            reasonType = ReportReasonType.Spam,
        )

        assertEquals(ReportSubjectKind.Account, submission.subjectKind)
    }

    @Test
    fun trimsOptionalReasonAndOmitsBlankText() {
        val withText = ReportSubmission(
            subject = "did:plc:alice",
            reasonType = ReportReasonType.Violation,
            reason = "  Additional context  ",
        )
        val blank = ReportSubmission(
            subject = "did:plc:alice",
            reasonType = ReportReasonType.Other,
            reason = "   ",
        )

        assertEquals("Additional context", withText.normalizedReason)
        assertNull(blank.normalizedReason)
    }

    @Test
    fun rejectsMalformedSubjects() {
        assertFailsWith<IllegalArgumentException> {
            ReportSubmission(
                subject = "https://example.com/not-an-at-uri",
                reasonType = ReportReasonType.Other,
            )
        }
    }

    @Test
    fun requiresCidForRecordSubjects() {
        assertFailsWith<IllegalArgumentException> {
            ReportSubmission(
                subject = "at://did:plc:alice/site.standard.document/3abc",
                reasonType = ReportReasonType.Other,
            )
        }
    }

    @Test
    fun rejectsCidForAccountSubjects() {
        assertFailsWith<IllegalArgumentException> {
            ReportSubmission(
                subject = "did:plc:alice",
                reasonType = ReportReasonType.Other,
                recordCid = "bafyreiaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            )
        }
    }
}
