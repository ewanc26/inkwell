package uk.ewancroft.inkwell.shared.xrpc

object XrpcEndpoints {
    const val PUBLIC_BSKY_API = "https://public.api.bsky.app"
    const val CONSTELLATION_API = "https://constellation.microcosm.blue"

    const val REPO_CREATE_RECORD = "/xrpc/com.atproto.repo.createRecord"
    const val REPO_PUT_RECORD = "/xrpc/com.atproto.repo.putRecord"
    const val REPO_DELETE_RECORD = "/xrpc/com.atproto.repo.deleteRecord"
    const val REPO_GET_RECORD = "/xrpc/com.atproto.repo.getRecord"
    const val REPO_LIST_RECORDS = "/xrpc/com.atproto.repo.listRecords"
    const val REPO_UPLOAD_BLOB = "/xrpc/com.atproto.repo.uploadBlob"
    const val SYNC_GET_BLOB = "/xrpc/com.atproto.sync.getBlob"
    const val SERVER_GET_SESSION = "/xrpc/com.atproto.server.getSession"
    const val IDENTITY_RESOLVE_HANDLE = "/xrpc/com.atproto.identity.resolveHandle"
    const val ACTOR_GET_PROFILE = "/xrpc/app.bsky.actor.getProfile"
    const val FEED_GET_POSTS = "/xrpc/app.bsky.feed.getPosts"
    const val GRAPH_GET_LIST = "/xrpc/app.bsky.graph.getList"
    const val GRAPH_MUTE_ACTOR = "/xrpc/app.bsky.graph.muteActor"
    const val GRAPH_UNMUTE_ACTOR = "/xrpc/app.bsky.graph.unmuteActor"
    const val GRAPH_GET_MUTES = "/xrpc/app.bsky.graph.getMutes"
    const val GRAPH_GET_BLOCKS = "/xrpc/app.bsky.graph.getBlocks"
    const val MODERATION_CREATE_REPORT = "/xrpc/com.atproto.moderation.createReport"
    const val MICROCOSM_GET_BACKLINKS = "/xrpc/blue.microcosm.links.getBacklinks"
}
