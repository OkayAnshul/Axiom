package com.cosmiclaboratory.axiom.domain.ml

import kotlin.math.ln
import com.cosmiclaboratory.axiom.domain.text.TextTokens

/**
 * A mood classifier trained on the user's own words, on their own device.
 *
 * Multinomial naive Bayes over token counts. Chosen over anything heavier for
 * reasons that matter here rather than reasons of fashion: it trains in
 * milliseconds on a few dozen examples, needs no model file, no network and no
 * API key, and — because it learns this person's vocabulary rather than a
 * pretrained one — it handles Hindi, Hinglish and private shorthand for free.
 * A general-purpose sentiment model knows what "terrible" means; this one
 * learns what *your* bad days sound like.
 *
 * Its whole job is to give keyless users the mood tracking and patterns that
 * otherwise need a cloud model. Where an API key exists the LLM does it better,
 * and this stays out of the way.
 */
object MoodClassifier {

    /** Below this many labelled entries, predictions are noise dressed as insight. */
    const val MIN_TRAINING_SAMPLES = 12

    /** One-class training data can only ever predict that class. */
    const val MIN_DISTINCT_MOODS = 2

    /** Posterior below this and we say nothing rather than guess. */
    const val MIN_CONFIDENCE = 0.55

    /** Words appearing once are noise; requiring two sightings shrinks the model and helps it. */
    const val MIN_TOKEN_OCCURRENCES = 2

    data class Model(
        val logPriors: Map<Int, Double>,
        /** mood -> token -> log P(token | mood) */
        val logLikelihoods: Map<Int, Map<String, Double>>,
        /** mood -> log P(unseen token | mood), the smoothed fallback */
        val logUnseen: Map<Int, Double>,
        val vocabulary: Set<String>,
        val trainingSize: Int
    )

    data class Prediction(val mood: Int, val confidence: Double)

    /**
     * Trains from (text, mood) pairs — in practice, entries where the user
     * tapped a mood themselves. Returns null when there is not enough to learn
     * from, which the caller must treat as "do not predict" rather than as a
     * neutral model.
     */
    fun train(samples: List<Pair<String, Int>>): Model? {
        val usable = samples.filter { (text, mood) -> mood in 1..5 && text.isNotBlank() }
        if (usable.size < MIN_TRAINING_SAMPLES) return null
        if (usable.map { it.second }.distinct().size < MIN_DISTINCT_MOODS) return null

        val tokenized = usable.map { (text, mood) -> tokenize(text) to mood }

        // Vocabulary trimmed to tokens seen more than once anywhere in the corpus.
        val globalCounts = mutableMapOf<String, Int>()
        tokenized.forEach { (tokens, _) ->
            tokens.forEach { token -> globalCounts[token] = (globalCounts[token] ?: 0) + 1 }
        }
        val vocabulary = globalCounts.filterValues { it >= MIN_TOKEN_OCCURRENCES }.keys
        if (vocabulary.isEmpty()) return null

        val byMood = tokenized.groupBy({ it.second }, { it.first })
        val logPriors = mutableMapOf<Int, Double>()
        val logLikelihoods = mutableMapOf<Int, Map<String, Double>>()
        val logUnseen = mutableMapOf<Int, Double>()

        byMood.forEach { (mood, documents) ->
            logPriors[mood] = ln(documents.size.toDouble() / usable.size)

            val counts = mutableMapOf<String, Int>()
            var total = 0
            documents.forEach { tokens ->
                tokens.filter { it in vocabulary }.forEach { token ->
                    counts[token] = (counts[token] ?: 0) + 1
                    total++
                }
            }
            // Laplace smoothing: every vocabulary word gets one imaginary sighting,
            // so an unseen word makes a mood unlikely rather than impossible.
            val denominator = (total + vocabulary.size).toDouble()
            logLikelihoods[mood] = vocabulary.associateWith { token ->
                ln(((counts[token] ?: 0) + 1).toDouble() / denominator)
            }
            logUnseen[mood] = ln(1.0 / denominator)
        }

        return Model(logPriors, logLikelihoods, logUnseen, vocabulary, usable.size)
    }

    /**
     * Predicts a mood, or null when the text shares nothing with the vocabulary
     * or no class clears [MIN_CONFIDENCE]. Confidence is the normalised
     * posterior, computed in log space so long entries do not underflow to zero.
     */
    fun predict(model: Model, text: String): Prediction? {
        val tokens = tokenize(text).filter { it in model.vocabulary }
        if (tokens.isEmpty()) return null

        val scores = model.logPriors.mapValues { (mood, logPrior) ->
            val likelihoods = model.logLikelihoods[mood].orEmpty()
            val unseen = model.logUnseen[mood] ?: 0.0
            logPrior + tokens.sumOf { token -> likelihoods[token] ?: unseen }
        }

        val best = scores.maxByOrNull { it.value } ?: return null
        // Subtract the max before exponentiating — the standard log-sum-exp guard.
        val shifted = scores.mapValues { (_, score) -> Math.exp(score - best.value) }
        val confidence = shifted.getValue(best.key) / shifted.values.sum()

        return if (confidence >= MIN_CONFIDENCE) Prediction(best.key, confidence) else null
    }

    /**
     * Lowercase word tokens of at least three characters, unicode-aware so
     * Devanagari survives. Deliberately no stemming and no stopword list: on a
     * personal corpus the "stopwords" of one person are signal for another, and
     * naive Bayes handles uninformative words by giving them flat likelihoods.
     */
    internal fun tokenize(text: String): List<String> =
        TextTokens.words(text, minLength = 3)

}
