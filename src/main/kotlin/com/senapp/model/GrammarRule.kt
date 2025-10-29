package com.senapp.model

/**
 * Representa una regla gramatical almacenada en la tabla grammar_rule.
 * - pattern  => columna pos_pattern (secuencia de POS, p.ej. "[PRON] [VERB] [NOUN]")
 * - template => columna output_template (plantilla con placeholders: {PRON}, {VERB:conj=pres,persona=1}, {NOUN}, etc.)
 */
data class GrammarRule(
    val id: Int,
    val pattern: String,
    val template: String,
    val lang: String? = "es-MX",
    val active: Boolean = true,
    val weight: Int = 0
)
