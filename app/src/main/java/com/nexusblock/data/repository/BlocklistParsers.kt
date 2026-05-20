package com.nexusblock.data.repository

object BlocklistParsers {

    fun parseHostFile(content: String): List<String> {
        val domains = mutableListOf<String>()
        val seen = HashSet<String>()

        for (line in content.lineSequence()) {
            val trimmed = line.substringBefore("#").trim()
            if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[")) continue

            val parts = trimmed.split(Regex("\\s+"))
            val domain = when {
                parts.size >= 2 && parts[0].isSinkholeAddress() -> parts[1]
                parts.size == 1 && !parts[0].contains(":") -> parts[0]
                else -> continue
            }.normalizeDomain()

            if (domain !in seen && isValidDomain(domain)) {
                seen.add(domain)
                domains.add(domain)
            }
        }

        return domains
    }

    fun parseAdGuardFilter(content: String): List<String> {
        val rules = mutableListOf<String>()
        val seen = HashSet<String>()

        for (line in content.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() ||
                trimmed.startsWith("!") ||
                trimmed.startsWith("#") ||
                trimmed.startsWith("[") ||
                trimmed.contains("##") ||
                trimmed.contains("#@#") ||
                trimmed.contains("#$#")
            ) {
                continue
            }

            val isAllow = trimmed.startsWith("@@")
            val body = if (isAllow) trimmed.removePrefix("@@") else trimmed
            val domain = extractAdGuardDomain(body)?.normalizeDomain() ?: continue
            if (!isValidDomain(domain)) continue

            val normalizedRule = if (isAllow) "@@||$domain^" else domain
            if (normalizedRule !in seen) {
                seen.add(normalizedRule)
                rules.add(normalizedRule)
            }
        }

        return rules
    }

    private fun extractAdGuardDomain(rule: String): String? {
        val start = when {
            rule.startsWith("||") -> 2
            rule.startsWith("|") -> 1
            else -> return null
        }
        val end = rule.indexOfAny(charArrayOf('^', '$', '/', '*', '|'), start)
        val raw = if (end > start) rule.substring(start, end) else rule.substring(start)
        return raw.takeIf { it.isNotBlank() }
    }

    fun isValidDomain(domain: String): Boolean {
        if (domain.length !in 1..253) return false
        if (domain.any { it.isWhitespace() }) return false
        if (domain.startsWith(".") || domain.endsWith(".")) return false
        if (domain.contains("*") || domain.contains("/")) return false

        val labels = domain.split(".")
        return labels.size >= 2 && labels.all { label ->
            label.isNotEmpty() &&
                label.length <= 63 &&
                label.all { it.isLetterOrDigit() || it == '-' || it == '_' } &&
                !label.startsWith("-") &&
                !label.endsWith("-")
        }
    }

    private fun String.isSinkholeAddress(): Boolean {
        return this == "0.0.0.0" || this == "127.0.0.1" || this == "::" || this == "::1"
    }

    private fun String.normalizeDomain(): String {
        return lowercase()
            .trim()
            .trimEnd('.')
    }
}
