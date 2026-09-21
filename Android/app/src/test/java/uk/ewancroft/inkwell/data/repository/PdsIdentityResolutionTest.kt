package uk.ewancroft.inkwell.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class PdsIdentityResolutionTest {
    private val json = Json

    @Test
    fun `builds did document urls for plc and web`() {
        assertEquals("https://plc.directory/did:plc:abc", didDocumentUrl("did:plc:abc"))
        assertEquals("https://example.com/.well-known/did.json", didDocumentUrl("did:web:example.com"))
        assertEquals("https://example.com/users/alice/did.json", didDocumentUrl("did:web:example.com:users:alice"))
    }

    @Test
    fun `extracts relative and fully qualified atproto pds services`() {
        val relative = json.parseToJsonElement("""{"service":[{"id":"#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"https://pds.example"}]}""").jsonObject
        val qualified = json.parseToJsonElement("""{"service":[{"id":"did:web:example.com#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"https://pds.example/"}]}""").jsonObject

        assertEquals("https://pds.example", extractAtprotoPdsEndpoint(relative, "did:web:example.com"))
        assertEquals("https://pds.example", extractAtprotoPdsEndpoint(qualified, "did:web:example.com"))
    }

    @Test
    fun `rejects unsupported or malformed identity documents`() {
        assertFailsWith<PdsResolutionException> { didDocumentUrl("did:key:z6Mk") }
        val malformed = json.parseToJsonElement("""{"service":[{"id":"#atproto_pds","type":"Other","serviceEndpoint":"http://pds.example"}]}""").jsonObject
        assertFailsWith<PdsResolutionException> { extractAtprotoPdsEndpoint(malformed, "did:web:example.com") }
    }

    @Test
    fun `rejects path bearing pds endpoints`() {
        val document = json.parseToJsonElement(
            """{"service":[{"id":"#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"https://pds.example/xrpc"}]}""",
        ).jsonObject

        assertFailsWith<IllegalStateException> {
            extractAtprotoPdsEndpoint(document, "did:web:example.com")
        }
    }
}
