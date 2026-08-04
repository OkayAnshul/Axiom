package com.cosmiclaboratory.axiom.domain.text

/**
 * One definition of "what separates two words", used by every tokenizer in the
 * app so they cannot drift apart again.
 *
 * The subtle part is `\p{M}`. Unicode classifies Devanagari vowel signs — the
 * ा in थका, the े in परेशान — as combining Marks, not Letters, so the obvious
 * `[^\p{L}\p{Nd}]+` treats them as separators and shreds Hindi words into
 * fragments: परेशान becomes पर + श + न, which any minimum-length filter then
 * discards entirely. Seven tokenizers in this codebase had that bug, including
 * the FTS sanitizer whose own comment claimed Devanagari survived it. The same
 * applies to Arabic, Tamil, Bengali and every other script that writes vowels
 * as marks.
 */
object TextTokens {

    /** Runs of anything that is not a letter, mark, or digit. */
    val SEPARATOR = Regex("[^\\p{L}\\p{M}\\p{Nd}]+")

    /** Characters to strip when only letters, marks, digits and spaces may remain. */
    val NON_WORD_OR_SPACE = Regex("[^\\p{L}\\p{M}\\p{N}\\s]")

    /** Lowercase word tokens of at least [minLength] characters, any script. */
    fun words(text: String, minLength: Int = 1): List<String> =
        text.lowercase().split(SEPARATOR).filter { it.length >= minLength }
}
