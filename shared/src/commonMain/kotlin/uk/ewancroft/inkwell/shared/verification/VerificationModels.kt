package uk.ewancroft.inkwell.shared.verification

/**
 * Why a publication or document failed verification.
 * Kept as distinct, diagnosable cases so the UI and logs can say
 * why a record is untrusted.
 */
sealed class VerificationFailure(val reason: String) {

    data class InvalidPublicationURL(val url: String) :
        VerificationFailure("\"$url\" isn't a valid publication URL.")

    data class InvalidDocumentURL(val url: String) :
        VerificationFailure("\"$url\" isn't a valid document URL.")

    data class EndpointUnreachable(val statusCode: Int?) :
        VerificationFailure(
            if (statusCode != null) "The endpoint returned status $statusCode."
            else "The endpoint couldn't be reached."
        )

    object MalformedResponse :
        VerificationFailure("The .well-known endpoint's response wasn't a usable AT-URI.")

    data class MismatchedURI(val expected: String, val found: String) :
        VerificationFailure(
            "Expected $expected but the domain's .well-known endpoint points to $found."
        )

    data class DocumentLinkMissing(val expected: String) :
        VerificationFailure("The document page doesn't link back to $expected.")

    data class Unexpected(val message: String?) :
        VerificationFailure("Verification failed unexpectedly: ${message ?: "unknown error"}.")
}

sealed class VerificationResult {
    object Verified : VerificationResult()
    data class Failed(val failure: VerificationFailure) : VerificationResult()
}
