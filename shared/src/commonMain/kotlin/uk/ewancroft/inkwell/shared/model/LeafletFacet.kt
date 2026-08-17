package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a Leaflet inline facet (byte-range formatting).
 *
 * Mirrors Android `LeafletFacet` and iOS `LeafletFacet`.
 */
data class LeafletFacet(
    val type: String? = null,
    val index: ByteSlice,
    val features: List<LeafletFacetFeature> = emptyList()
)
