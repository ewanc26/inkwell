package uk.ewancroft.inkwell.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.w3c.dom.Element

/**
 * Locale regression coverage for the string catalogues (#88).
 *
 * French is the end-to-end validation locale: every default (`values/`)
 * string must have a French counterpart with compatible format specifiers,
 * and every `<plurals>` entry must carry exactly the quantity variants its
 * locale's CLDR rules can select. This is the test that would have caught
 * #161 (a `zero` quantity neither English nor French CLDR rules select, and
 * an empty state rendered from a quantity branch instead of an explicit
 * count check).
 *
 * Runs as a plain JVM unit test: it parses the source XML directly rather
 * than going through Android resources, so it needs no device or emulator.
 * The resource directory is resolved relative to the module working
 * directory, which is where Gradle runs unit tests; the paths fail loudly
 * if that ever stops being true.
 */
class LocaleResourcesParityTest {

    private val valuesDir: File
        get() {
            val dir = File("src/main/res")
            assertTrue(dir.isDirectory, "expected Android res dir at ${dir.absolutePath}")
            return dir
        }

    private data class PluralItem(val quantity: String, val text: String)

    private fun strings(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        return doc.getElementsByTagName("string").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
                .associate { it.getAttribute("name") to it.textContent.trim() }
        }
    }

    private fun plurals(file: File): Map<String, List<PluralItem>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        return doc.getElementsByTagName("plurals").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }.associate { plural ->
                val items = plural.getElementsByTagName("item").let { items ->
                    (0 until items.length).map { items.item(it) as Element }
                        .map { PluralItem(it.getAttribute("quantity"), it.textContent.trim()) }
                }
                plural.getAttribute("name") to items
            }
        }
    }

    /**
     * Format specifiers (`%d`, `%1$s`, …) in a string, ignoring the escaped
     * literal `%%`. Returned as a multiset so a duplicated specifier counts
     * twice: repeating a positional argument is safe, dropping one is not.
     */
    private fun specifiers(text: String): List<String> {
        val stripped = text.replace("%%", "")
        return Regex("%(?:\\d+\\$)?[a-zA-Z]").findAll(stripped).map { it.value }.sorted().toList()
    }

    /**
     * Quantity variants each locale's CLDR plural rules can actually select.
     * English selects one/other; French selects one (which includes 0),
     * many (millions and above) and other. A variant outside this set is
     * dead weight that misleads the next editor — see #161.
     */
    private val selectableQuantities = mapOf(
        "values" to setOf("one", "other"),
        "values-fr" to setOf("one", "many", "other"),
    )

    private fun resFile(localeDir: String): File {
        val file = File(valuesDir, "$localeDir/strings.xml")
        assertTrue(file.isFile, "missing $file")
        return file
    }

    @Test
    fun `every default string has a French counterpart and vice versa`() {
        val default = strings(resFile("values"))
        val french = strings(resFile("values-fr"))

        val missing = default.keys - french.keys
        val unexpected = french.keys - default.keys
        assertTrue(missing.isEmpty(), "French strings.xml is missing: $missing")
        assertTrue(unexpected.isEmpty(), "French strings.xml has unexpected keys: $unexpected")
    }

    @Test
    fun `format specifiers match between default and French strings`() {
        val default = strings(resFile("values"))
        val french = strings(resFile("values-fr"))

        val mismatches = default.keys.mapNotNull { name ->
            val expected = specifiers(default.getValue(name))
            val actual = specifiers(french.getValue(name))
            if (expected != actual) "$name: default $expected vs fr $actual" else null
        }
        assertTrue(mismatches.isEmpty(), "placeholder drift:\n${mismatches.joinToString("\n")}")
    }

    @Test
    fun `every default plural has a French counterpart and vice versa`() {
        val default = plurals(resFile("values"))
        val french = plurals(resFile("values-fr"))

        val missing = default.keys - french.keys
        val unexpected = french.keys - default.keys
        assertTrue(missing.isEmpty(), "French strings.xml is missing plurals: $missing")
        assertTrue(unexpected.isEmpty(), "French strings.xml has unexpected plurals: $unexpected")
    }

    @Test
    fun `plural quantities are exactly the ones each locale can select`() {
        for ((localeDir, allowed) in selectableQuantities) {
            val entries = plurals(resFile(localeDir))
            val problems = entries.flatMap { (name, items) ->
                val quantities = items.map { it.quantity }.toSet()
                buildList {
                    val unknown = quantities - allowed
                    if (unknown.isNotEmpty()) add("$name has unselectable $unknown")
                    if ("other" !in quantities) add("$name is missing the required 'other' variant")
                }
            }
            assertTrue(problems.isEmpty(), "$localeDir:\n${problems.joinToString("\n")}")
        }
    }

    @Test
    fun `plural format specifiers match per shared quantity`() {
        val default = plurals(resFile("values"))
        val french = plurals(resFile("values-fr"))

        val mismatches = mutableListOf<String>()
        for (name in default.keys.intersect(french.keys)) {
            val defaultByQuantity = default.getValue(name).associate { it.quantity to specifiers(it.text) }
            val frenchByQuantity = french.getValue(name).associate { it.quantity to specifiers(it.text) }
            for (quantity in defaultByQuantity.keys.intersect(frenchByQuantity.keys)) {
                val expected = defaultByQuantity.getValue(quantity)
                val actual = frenchByQuantity.getValue(quantity)
                if (quantity == "one" && expected.isEmpty()) {
                    // French `one` covers 0 as well as 1, so it must always carry
                    // the count while English may hardcode the singular ("1 new
                    // document"). There is nothing to line up against; every
                    // other quantity is still compared strictly below.
                    continue
                }
                if (expected != actual) mismatches += "$name [$quantity]: default $expected vs fr $actual"
            }
        }
        assertTrue(mismatches.isEmpty(), "placeholder drift:\n${mismatches.joinToString("\n")}")
    }

    @Test
    fun `no string or plural item is blank`() {
        for (localeDir in selectableQuantities.keys) {
            val strings = strings(resFile(localeDir))
            val blankStrings = strings.filterValues { it.isBlank() }.keys
            assertTrue(blankStrings.isEmpty(), "$localeDir has blank strings: $blankStrings")

            val plurals = plurals(resFile(localeDir))
            val blankItems = plurals.flatMap { (name, items) ->
                items.filter { it.text.isBlank() }.map { "$name [${it.quantity}]" }
            }
            assertTrue(blankItems.isEmpty(), "$localeDir has blank plural items: $blankItems")
        }
    }

    @Test
    fun `duplicate resource names fail loudly instead of shadowing`() {
        // The DOM parse above keeps the last duplicate silently; re-scan the raw
        // XML so a copy-paste duplicate trips the test instead of shadowing.
        for (localeDir in selectableQuantities.keys) {
            val text = resFile(localeDir).readText()
            val names = Regex("<(?:string|plurals)\\s+name=\"([^\"]+)\"")
                .findAll(text).map { it.groupValues[1] }.toList()
            val duplicates = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertTrue(duplicates.isEmpty(), "$localeDir has duplicate resource names: $duplicates")
        }
    }

    @Test
    fun `locale coverage does not regress without updating this test`() {
        // Adding values-<locale> is a deliberate act: extend
        // `selectableQuantities` with the new locale's CLDR-selectable set.
        val localeDirs = valuesDir.listFiles { file ->
            file.isDirectory && (file.name == "values" || file.name.startsWith("values-"))
        }
            ?.map { it.name }
            ?.filter { it != "values-night" }
            ?.sorted()
            ?: fail("could not list ${valuesDir.absolutePath}")
        assertEquals(
            selectableQuantities.keys.sorted(),
            localeDirs,
            "a locale resource dir was added or removed; update selectableQuantities",
        )
    }
}
