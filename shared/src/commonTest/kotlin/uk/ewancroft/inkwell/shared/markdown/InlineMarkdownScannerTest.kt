package uk.ewancroft.inkwell.shared.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InlineMarkdownScannerTest {

    @Test
    fun plainText() {
        val segments = InlineMarkdownScanner.scan("hello world")
        assertEquals(1, segments.size)
        assertEquals("hello world", (segments[0] as InlineSegment.Plain).text)
    }

    @Test
    fun bold() {
        val segments = InlineMarkdownScanner.scan("**bold**")
        assertEquals(1, segments.size)
        assertEquals("bold", (segments[0] as InlineSegment.Bold).text)
    }

    @Test
    fun italic() {
        val segments = InlineMarkdownScanner.scan("*italic*")
        assertEquals(1, segments.size)
        assertEquals("italic", (segments[0] as InlineSegment.Italic).text)
    }

    @Test
    fun code() {
        val segments = InlineMarkdownScanner.scan("`code`")
        assertEquals(1, segments.size)
        assertEquals("code", (segments[0] as InlineSegment.Code).text)
    }

    @Test
    fun codeFollowedByPlainText() {
        val segments = InlineMarkdownScanner.scan("`inline code` and a fenced block:")
        assertEquals(2, segments.size)
        assertEquals("inline code", (segments[0] as InlineSegment.Code).text)
        assertEquals(" and a fenced block:", (segments[1] as InlineSegment.Plain).text)
    }

    @Test
    fun strike() {
        val segments = InlineMarkdownScanner.scan("~~strike~~")
        assertEquals(1, segments.size)
        assertEquals("strike", (segments[0] as InlineSegment.Strike).text)
    }

    @Test
    fun link() {
        val segments = InlineMarkdownScanner.scan("[text](https://example.com)")
        assertEquals(1, segments.size)
        val link = segments[0] as InlineSegment.Link
        assertEquals("text", link.text)
        assertEquals("https://example.com", link.url)
    }

    @Test
    fun mixed() {
        val segments = InlineMarkdownScanner.scan("plain **bold** and *italic*")
        assertEquals(4, segments.size)
        assertEquals("plain ", (segments[0] as InlineSegment.Plain).text)
        assertEquals("bold", (segments[1] as InlineSegment.Bold).text)
        assertEquals(" and ", (segments[2] as InlineSegment.Plain).text)
        assertEquals("italic", (segments[3] as InlineSegment.Italic).text)
    }

    @Test
    fun unmatchedDelimitersArePlain() {
        val segments = InlineMarkdownScanner.scan("**unmatched")
        assertEquals(1, segments.size)
        assertEquals("**unmatched", (segments[0] as InlineSegment.Plain).text)
    }

    @Test
    fun emptyInput() {
        val segments = InlineMarkdownScanner.scan("")
        assertEquals(0, segments.size)
    }

    @Test
    fun unicodeText() {
        val segments = InlineMarkdownScanner.scan("**héllo** wörld")
        assertEquals(2, segments.size)
        assertEquals("héllo", (segments[0] as InlineSegment.Bold).text)
        assertEquals(" wörld", (segments[1] as InlineSegment.Plain).text)
    }
}
