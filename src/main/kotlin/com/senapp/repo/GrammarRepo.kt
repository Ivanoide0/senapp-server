package com.senapp.repo

import com.senapp.model.GrammarRule
import javax.sql.DataSource

data class LemmaRow(
    val gloss: String,
    val pos: String,
    val surfaceEs: String?
)

/**
 * Repositorio SQL para:
 *  - mapear sign_id -> (glosa, POS)
 *  - cargar reglas gramaticales
 *  - opcional: morfología simple (conjugación muy básica)
 */
class GrammarRepo(private val ds: DataSource) {

    /** Devuelve las posibles glosas (en orden de prioridad) para un sign_id. */
    fun mapSignToLemma(signId: String): List<LemmaRow> {
        val sql = """
            select l.gloss, l.pos, l.surface_es
            from sign_lemma s
            join lexeme l on l.id = s.lexeme_id
            where s.sign_id = ?
            order by s.weight desc, s.lexeme_id asc
        """.trimIndent()

        ds.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, signId)
                ps.executeQuery().use { rs ->
                    val out = mutableListOf<LemmaRow>()
                    while (rs.next()) {
                        out += LemmaRow(
                            gloss = rs.getString("gloss"),
                            pos = rs.getString("pos"),
                            surfaceEs = rs.getString("surface_es")
                        )
                    }
                    // Fallback si no hay mapeo en BD: devolvemos el signId como INTJ
                    if (out.isEmpty()) {
                        out += LemmaRow(gloss = signId.uppercase(), pos = "INTJ", surfaceEs = null)
                    }
                    return out
                }
            }
        }
    }

    /** Carga reglas activas para es-MX ordenadas por weight desc. */
    fun loadRules(lang: String = "es-MX"): List<GrammarRule> {
        val sql = """
        select id, pos_pattern, output_template, lang_code, active, weight
        from v_grammar_rule
        where active = true and (lang_code = ? or lang_code is null)
        order by weight desc, id asc
    """.trimIndent()

        ds.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, lang)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<GrammarRule>()
                    while (rs.next()) {
                        list += GrammarRule(
                            id = rs.getInt("id"),
                            pattern = rs.getString("pos_pattern"),
                            template = rs.getString("output_template"),
                            lang = rs.getString("lang_code"),
                            active = rs.getBoolean("active"),
                            weight = rs.getInt("weight")
                        )
                    }
                    return list
                }
            }
        }
    }


    /**
     * Morfología MUY básica para demo.
     * Si no sabe conjugar, devuelve null y el intérprete usará la glosa "tal cual".
     */
    fun morph(gloss: String, conj: String, persona: Int?): String? {
        val g = gloss.uppercase().trim()

        return when (g) {
            "AMAR" -> when (conj) {
                "pres" -> when (persona) {
                    1 -> "amo"
                    2 -> "amas"
                    3 -> "ama"
                    else -> "amar"
                }
                else -> "amar"
            }
            "QUERER" -> when (conj) {
                "pres" -> when (persona) {
                    1 -> "quiero"
                    2 -> "quieres"
                    3 -> "quiere"
                    else -> "querer"
                }
                else -> "querer"
            }
            else -> null
        }
    }
}
