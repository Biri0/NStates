package it.rfmariano.nstates.data.api

/**
 * Replaces HTML named and numeric character entities with their Unicode equivalents
 * so that [org.xmlpull.v1.XmlPullParser] can process the XML without choking on
 * entities like `&agrave;` or `&nbsp;` which are valid in HTML but not in XML.
 *
 * Handles:
 * - Named entities (`&agrave;` -> `a`, `&nbsp;` -> ` `, etc.)
 * - Decimal numeric entities (`&#160;` -> the corresponding character)
 * - Hex numeric entities (`&#xA0;` -> the corresponding character)
 *
 * The five standard XML entities (`&amp;`, `&lt;`, `&gt;`, `&quot;`, `&apos;`)
 * are preserved as-is since `XmlPullParser` handles them natively.
 */
fun replaceHtmlEntities(xml: String): String {
    return ENTITY_REGEX.replace(xml) { match ->
        val entity = match.groupValues[1] // content between & and ;
        when {
            // Preserve the five standard XML entities
            entity == "amp" || entity == "lt" || entity == "gt" ||
                entity == "quot" || entity == "apos" -> match.value

            // Decimal numeric entity: &#123;
            entity.startsWith("#") && !entity.startsWith("#x", ignoreCase = true) -> {
                val codePoint = entity.substring(1).toIntOrNull()
                if (codePoint != null) {
                    String(Character.toChars(codePoint))
                } else {
                    match.value
                }
            }

            // Hex numeric entity: &#x1F; or &#X1f;
            entity.startsWith("#x", ignoreCase = true) -> {
                val codePoint = entity.substring(2).toIntOrNull(16)
                if (codePoint != null) {
                    String(Character.toChars(codePoint))
                } else {
                    match.value
                }
            }

            // Named entity lookup
            else -> HTML_ENTITIES[entity] ?: match.value
        }
    }
}

private val ENTITY_REGEX = Regex("&([a-zA-Z0-9#]+?);")

/**
 * Map of common HTML named entities to their Unicode character equivalents.
 * Covers Latin-1 supplement, common symbols, and frequently used entities
 * found in NationStates API responses.
 */
private val HTML_ENTITIES: Map<String, String> = mapOf(
    // Whitespace / special
    "nbsp" to "\u00A0",
    "ensp" to "\u2002",
    "emsp" to "\u2003",
    "thinsp" to "\u2009",
    "zwnj" to "\u200C",
    "zwj" to "\u200D",
    "lrm" to "\u200E",
    "rlm" to "\u200F",

    // Latin accented characters (lowercase)
    "agrave" to "\u00E0",
    "aacute" to "\u00E1",
    "acirc" to "\u00E2",
    "atilde" to "\u00E3",
    "auml" to "\u00E4",
    "aring" to "\u00E5",
    "aelig" to "\u00E6",
    "ccedil" to "\u00E7",
    "egrave" to "\u00E8",
    "eacute" to "\u00E9",
    "ecirc" to "\u00EA",
    "euml" to "\u00EB",
    "igrave" to "\u00EC",
    "iacute" to "\u00ED",
    "icirc" to "\u00EE",
    "iuml" to "\u00EF",
    "eth" to "\u00F0",
    "ntilde" to "\u00F1",
    "ograve" to "\u00F2",
    "oacute" to "\u00F3",
    "ocirc" to "\u00F4",
    "otilde" to "\u00F5",
    "ouml" to "\u00F6",
    "oslash" to "\u00F8",
    "ugrave" to "\u00F9",
    "uacute" to "\u00FA",
    "ucirc" to "\u00FB",
    "uuml" to "\u00FC",
    "yacute" to "\u00FD",
    "thorn" to "\u00FE",
    "yuml" to "\u00FF",

    // Latin accented characters (uppercase)
    "Agrave" to "\u00C0",
    "Aacute" to "\u00C1",
    "Acirc" to "\u00C2",
    "Atilde" to "\u00C3",
    "Auml" to "\u00C4",
    "Aring" to "\u00C5",
    "AElig" to "\u00C6",
    "Ccedil" to "\u00C7",
    "Egrave" to "\u00C8",
    "Eacute" to "\u00C9",
    "Ecirc" to "\u00CA",
    "Euml" to "\u00CB",
    "Igrave" to "\u00CC",
    "Iacute" to "\u00CD",
    "Icirc" to "\u00CE",
    "Iuml" to "\u00CF",
    "ETH" to "\u00D0",
    "Ntilde" to "\u00D1",
    "Ograve" to "\u00D2",
    "Oacute" to "\u00D3",
    "Ocirc" to "\u00D4",
    "Otilde" to "\u00D5",
    "Ouml" to "\u00D6",
    "Oslash" to "\u00D8",
    "Ugrave" to "\u00D9",
    "Uacute" to "\u00DA",
    "Ucirc" to "\u00DB",
    "Uuml" to "\u00DC",
    "Yacute" to "\u00DD",
    "THORN" to "\u00DE",

    // Other Latin
    "szlig" to "\u00DF",
    "OElig" to "\u0152",
    "oelig" to "\u0153",
    "Scaron" to "\u0160",
    "scaron" to "\u0161",
    "Yuml" to "\u0178",
    "fnof" to "\u0192",

    // Punctuation and symbols
    "iexcl" to "\u00A1",
    "cent" to "\u00A2",
    "pound" to "\u00A3",
    "curren" to "\u00A4",
    "yen" to "\u00A5",
    "brvbar" to "\u00A6",
    "sect" to "\u00A7",
    "uml" to "\u00A8",
    "copy" to "\u00A9",
    "ordf" to "\u00AA",
    "laquo" to "\u00AB",
    "not" to "\u00AC",
    "shy" to "\u00AD",
    "reg" to "\u00AE",
    "macr" to "\u00AF",
    "deg" to "\u00B0",
    "plusmn" to "\u00B1",
    "sup2" to "\u00B2",
    "sup3" to "\u00B3",
    "acute" to "\u00B4",
    "micro" to "\u00B5",
    "para" to "\u00B6",
    "middot" to "\u00B7",
    "cedil" to "\u00B8",
    "sup1" to "\u00B9",
    "ordm" to "\u00BA",
    "raquo" to "\u00BB",
    "frac14" to "\u00BC",
    "frac12" to "\u00BD",
    "frac34" to "\u00BE",
    "iquest" to "\u00BF",
    "times" to "\u00D7",
    "divide" to "\u00F7",

    // Typographic
    "ndash" to "\u2013",
    "mdash" to "\u2014",
    "lsquo" to "\u2018",
    "rsquo" to "\u2019",
    "sbquo" to "\u201A",
    "ldquo" to "\u201C",
    "rdquo" to "\u201D",
    "bdquo" to "\u201E",
    "dagger" to "\u2020",
    "Dagger" to "\u2021",
    "bull" to "\u2022",
    "hellip" to "\u2026",
    "permil" to "\u2030",
    "prime" to "\u2032",
    "Prime" to "\u2033",
    "lsaquo" to "\u2039",
    "rsaquo" to "\u203A",
    "oline" to "\u203E",
    "trade" to "\u2122",

    // Currency / math
    "euro" to "\u20AC",
    "frasl" to "\u2044",
    "minus" to "\u2212",
    "lowast" to "\u2217",
    "radic" to "\u221A",
    "infin" to "\u221E",

    // Arrows
    "larr" to "\u2190",
    "uarr" to "\u2191",
    "rarr" to "\u2192",
    "darr" to "\u2193",
    "harr" to "\u2194",

    // Greek (commonly encountered)
    "Alpha" to "\u0391",
    "Beta" to "\u0392",
    "Gamma" to "\u0393",
    "Delta" to "\u0394",
    "Epsilon" to "\u0395",
    "Zeta" to "\u0396",
    "Eta" to "\u0397",
    "Theta" to "\u0398",
    "Iota" to "\u0399",
    "Kappa" to "\u039A",
    "Lambda" to "\u039B",
    "Mu" to "\u039C",
    "Nu" to "\u039D",
    "Xi" to "\u039E",
    "Omicron" to "\u039F",
    "Pi" to "\u03A0",
    "Rho" to "\u03A1",
    "Sigma" to "\u03A3",
    "Tau" to "\u03A4",
    "Upsilon" to "\u03A5",
    "Phi" to "\u03A6",
    "Chi" to "\u03A7",
    "Psi" to "\u03A8",
    "Omega" to "\u03A9",
    "alpha" to "\u03B1",
    "beta" to "\u03B2",
    "gamma" to "\u03B3",
    "delta" to "\u03B4",
    "epsilon" to "\u03B5",
    "zeta" to "\u03B6",
    "eta" to "\u03B7",
    "theta" to "\u03B8",
    "iota" to "\u03B9",
    "kappa" to "\u03BA",
    "lambda" to "\u03BB",
    "mu" to "\u03BC",
    "nu" to "\u03BD",
    "xi" to "\u03BE",
    "omicron" to "\u03BF",
    "pi" to "\u03C0",
    "rho" to "\u03C1",
    "sigmaf" to "\u03C2",
    "sigma" to "\u03C3",
    "tau" to "\u03C4",
    "upsilon" to "\u03C5",
    "phi" to "\u03C6",
    "chi" to "\u03C7",
    "psi" to "\u03C8",
    "omega" to "\u03C9"
)
