package uk.ewancroft.inkwell.shared.policy

/**
 * Shared pagination policy for `com.atproto.repo.listRecords`.
 *
 * Both platforms page through a repo's records to completion, capped so a
 * misbehaving PDS returning an endless cursor can't hang the caller forever.
 * Previously iOS capped at 1,000 records and Android at 500 for the same
 * kind of call — this unifies the two.
 */
object RecordListPolicy {
    /** Maximum records to accumulate before giving up on further pages. */
    const val MAX_RECORDS: Int = 500

    /** Records requested per page. */
    const val PAGE_LIMIT: Int = 100
}
