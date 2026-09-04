package com.copy2wa.app

object PhoneNormalizer {

    // Whole text must be a phone-like token: optional '+', digits, spaces, dashes, dots, parentheses
    private val token = Regex("""\A\+?[\d\s\-().]{7,20}\z""")

    /** Returns WhatsApp-ready digits (e.g. "62817996027") or null if not a phone number. */
    fun normalize(raw: String, defaultCountryCode: String = "62"): String? {
        val text = raw.trim()
        if (!token.matches(text)) return null
        val digits = text.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val cc = defaultCountryCode.filter { it.isDigit() }.ifEmpty { "62" }

        val normalized = when {
            digits.startsWith(cc)  -> digits                    // +62 817... / 62817...
            text.startsWith("+")   -> digits                    // other explicit country code
            digits.startsWith("0") -> cc + digits.drop(1)       // 0817... -> 62817...
            digits.startsWith("8") -> cc + digits               // 817996027 -> 62817996027
            else -> return null
        }
        if (normalized.length < 10 || normalized.length > 15) return null
        return normalized
    }
}
