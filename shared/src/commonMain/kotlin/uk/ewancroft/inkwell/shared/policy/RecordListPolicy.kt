package uk.ewancroft.inkwell.shared.policy

/**
 * Shared pagination policy for `com.atproto.repo.listRecords`.
 *
 * Both platforms page through a repo's records to completion. Safety against
 * non-progressing or endless cursors belongs to each transport's page-budget
 * and repeated-cursor guards, not to a silent record-count truncation.
 */
object RecordListPolicy {
    /** Records requested per page. */
    const val PAGE_LIMIT: Int = 100
}
