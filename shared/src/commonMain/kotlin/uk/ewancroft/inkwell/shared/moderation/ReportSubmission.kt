package uk.ewancroft.inkwell.shared.moderation

import uk.ewancroft.inkwell.shared.AtUri

/**
 * The standard report categories supported by
 * `com.atproto.moderation.createReport`.
 *
 * The wire values are fully-qualified lexicon tokens. Keeping them here
 * prevents platform clients from drifting to abbreviated or display-only
 * values when submitting a report.
 */
enum class ReportReasonType(
    val wireValue: String,
    val displayName: String,
) {
    Spam("com.atproto.moderation.defs#reasonSpam", "Spam"),
    Violation("com.atproto.moderation.defs#reasonViolation", "Violation"),
    Misleading("com.atproto.moderation.defs#reasonMisleading", "Misleading"),
    Sexual("com.atproto.moderation.defs#reasonSexual", "Sexual content"),
    Rude("com.atproto.moderation.defs#reasonRude", "Rude or harassing"),
    Other("com.atproto.moderation.defs#reasonOther", "Other"),
}

/** The AT Protocol subject union variant used by a [ReportSubmission]. */
enum class ReportSubjectKind {
    Account,
    Record,
}

/**
 * Platform-neutral input for `com.atproto.moderation.createReport`.
 *
 * OAuth authentication and JSON transport remain platform-specific because
 * each app owns its own OAuth session. The subject, reason type, and text
 * normalization are common business rules and live here.
 */
data class ReportSubmission(
    val subject: String,
    val reasonType: ReportReasonType,
    val reason: String? = null,
    /** Required content fingerprint when [subject] names an AT-URI record. */
    val recordCid: String? = null,
) {
    init {
        val subjectKind = subjectKindOrNull
        require(subject.isNotBlank()) { "A report must identify an account or record." }
        require(subject == subject.trim()) { "A report subject cannot have surrounding whitespace." }
        require(subjectKind != null) { "A report subject must be a DID or AT-URI." }
        require(recordCid == null || recordCid == recordCid.trim()) {
            "A record CID cannot have surrounding whitespace."
        }
        when (checkNotNull(subjectKind)) {
            ReportSubjectKind.Account -> require(recordCid == null) {
                "An account report cannot include a record CID."
            }
            ReportSubjectKind.Record -> require(!recordCid.isNullOrBlank()) {
                "A record report must include the record's CID."
            }
        }
        require(reason == null || reason.length <= MAX_REASON_LENGTH) {
            "A report comment cannot exceed $MAX_REASON_LENGTH characters."
        }
    }

    /** Whether [subject] represents a repository account or an AT-URI record. */
    val subjectKind: ReportSubjectKind
        get() = checkNotNull(subjectKindOrNull)

    /** Optional report text trimmed for the JSON request; blank text is omitted. */
    val normalizedReason: String?
        get() = reason?.trim()?.takeIf(String::isNotEmpty)

    private val subjectKindOrNull: ReportSubjectKind?
        get() = when {
            subject.startsWith("did:") -> ReportSubjectKind.Account
            AtUri.parse(subject) != null -> ReportSubjectKind.Record
            else -> null
        }

    companion object {
        /** `createReport.reason` permits up to 20,000 code units. */
        const val MAX_REASON_LENGTH = 20_000
    }
}
