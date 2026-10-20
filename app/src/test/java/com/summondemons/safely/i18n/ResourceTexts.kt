package com.summondemons.safely.i18n

import com.summondemons.safely.R
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The text resources of one `values*` folder, read as plain JVM data (no Android runtime). */
data class ResourceTexts(
    val strings: Map<String, String>,
    val arrays: Map<String, List<String>>,
    /** Key → quantity ("one", "many", "other"…) → text. */
    val plurals: Map<String, Map<String, String>>,
    /** Keys marked `translatable="false"`. */
    val untranslatable: Set<String>,
) {
    val keys: Set<String> get() = strings.keys + arrays.keys + plurals.keys

    companion object {
        fun load(dir: File): ResourceTexts {
            val strings = mutableMapOf<String, String>()
            val arrays = mutableMapOf<String, List<String>>()
            val plurals = mutableMapOf<String, Map<String, String>>()
            val untranslatable = mutableSetOf<String>()
            val files = dir.listFiles { f -> f.extension == "xml" }.orEmpty().sortedBy { it.name }
            for (file in files) {
                val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
                for (node in root.children()) {
                    val name = node.getAttribute("name")
                    if (node.getAttribute("translatable") == "false") untranslatable += name
                    when (node.tagName) {
                        "string" -> strings[name] = unescapeAndroid(node.textContent)
                        "string-array" -> arrays[name] = node.children("item").map { unescapeAndroid(it.textContent) }
                        "plurals" -> plurals[name] =
                            node.children("item").associate { it.getAttribute("quantity") to unescapeAndroid(it.textContent) }
                    }
                }
            }
            return ResourceTexts(strings, arrays, plurals, untranslatable)
        }

        private fun Element.children(tag: String? = null): List<Element> =
            (0 until childNodes.length).map { childNodes.item(it) }
                .filterIsInstance<Element>()
                .filter { tag == null || it.tagName == tag }
    }
}

/** Android string-resource text as shown on screen: outer quotes removed, backslash escapes resolved. */
fun unescapeAndroid(raw: String): String {
    val text = if (raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"')) raw.substring(1, raw.length - 1) else raw
    val out = StringBuilder()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\\' && i + 1 < text.length) {
            out.append(
                when (val next = text[i + 1]) {
                    'n' -> '\n'
                    't' -> '\t'
                    else -> next
                },
            )
            i += 2
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString()
}

/** The `values*` folder of a shipped locale, from the module directory. */
fun valuesDir(qualifier: String? = null): File =
    File(if (qualifier == null) "src/main/res/values" else "src/main/res/values-$qualifier")

val english: ResourceTexts by lazy { ResourceTexts.load(valuesDir()) }

/** Resource name of a string resource id (Task 3 adds `R.plurals`, Task 4 adds `R.array`). */
fun resName(id: Int): String =
    listOf<Class<*>>(R.string::class.java, R.plurals::class.java)
        .firstNotNullOfOrNull { type -> type.fields.firstOrNull { it.getInt(null) == id }?.name }
        ?: error("No string resource with id $id")

/** English text of a string resource. */
fun en(id: Int): String = english.strings.getValue(resName(id))
