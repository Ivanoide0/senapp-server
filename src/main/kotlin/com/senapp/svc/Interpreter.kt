package com.senapp.svc

import com.senapp.model.InterpretRequest
import com.senapp.model.InterpretResponse
import com.senapp.repo.GrammarRepo

/**
 * Interpreter basado en GrammarRepo:
 * - Si hay 1 sola seña: devuelve su glosa mapeada (modo simple).
 * - Si hay varias: intenta reglas por patrón de POS; si no matchea, concatena glosas normalizadas.
 */
class Interpreter(private val repo: GrammarRepo) {

    private data class Tok(val gloss: String, val pos: String)

    /** Normaliza algunas glosas a texto final. Ajusta a gusto. */
    private fun glossDefault(gloss: String): String = when (gloss) {
        "YO" -> "yo"
        "TU" -> "tú"
        "NO" -> "no"
        else -> gloss.lowercase()
    }

    fun interpret(req: InterpretRequest): InterpretResponse {
        if (req.tokens.isEmpty()) return InterpretResponse(text = "")

        // 1) signId -> mejor glosa (por peso) desde sign_lemma/lemma
        val toks: List<Tok> = req.tokens.mapNotNull { t ->
            val lemmas = repo.mapSignToLemma(t.signId)
            val top = lemmas.firstOrNull() ?: return@mapNotNull null
            Tok(top.gloss, top.pos)
        }

        // Si no hay mapeos en BD, devuelve los signId para depurar
        if (toks.isEmpty()) {
            val echo = req.tokens.joinToString(" ") { it.signId }
            return InterpretResponse(text = echo, confidence = 0f)
        }

        // 2) MODO SIMPLE: una sola seña => regresa su glosa mapeada tal cual (sin gramática)
        if (toks.size == 1) {
            val g = glossDefault(toks[0].gloss)
            val conf = req.tokens.first().conf.toFloat()
            return InterpretResponse(text = g, confidence = conf)
        }

        // 3) MODO GRAMÁTICA: intenta plantilla por secuencia de POS
        val posSeq = toks.joinToString(" ") { "[${it.pos}]" }
        val rules = repo.loadRules()
        rules.firstOrNull { it.pattern == posSeq }?.let { r ->
            val text = renderTemplate(r.template, toks)
            return InterpretResponse(text = text, confidence = 0.8f)
        }

        // 4) Fallback: concatenar glosas normalizadas
        val text = toks.joinToString(" ") { glossDefault(it.gloss) }
        return InterpretResponse(text = text, confidence = 0.5f)
    }

    /** Rellena {POS} en la plantilla con la glosa correspondiente. */
    private fun renderTemplate(tpl: String, toks: List<Tok>): String {
        var out = tpl
        fun firstBy(pos: String) = toks.firstOrNull { it.pos == pos }

        listOf("PRON", "NOUN", "INTJ", "NEG", "NUM").forEach { pos ->
            firstBy(pos)?.let { out = out.replace("{$pos}", glossDefault(it.gloss)) }
        }

        // {VERB[:conj=...,persona=...]}
        val verbRegex = "\\{VERB(:[^}]*)?}".toRegex()
        out = verbRegex.replace(out) { m ->
            val token = firstBy("VERB") ?: return@replace ""
            val params = m.groupValues.getOrNull(1)?.trim(':').orEmpty()
            if (params.isBlank()) return@replace glossDefault(token.gloss)

            val kv = params.split(",").associate {
                val p = it.split("=")
                p[0] to p.getOrNull(1)
            }
            val conj = kv["conj"] ?: "inf"
            val persona = kv["persona"]?.toIntOrNull()
            val morphed = repo.morph(token.gloss, conj, persona)
            morphed ?: glossDefault(token.gloss)
        }

        return out.replace(Regex("\\s+"), " ").trim()
    }
}
