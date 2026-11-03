package com.example.nativegallery.data

/** Bounded appearance samples cover changes in pose without retaining another copy of every face. */
internal class FaceClusterIndex {
    internal class Cluster(val key: String, photoId: String, embedding: FloatArray) {
        val photoIds = linkedSetOf(photoId)
        private var sum = embedding.copyOf()
        var centroid = embedding
            private set
        private val samples = mutableListOf(embedding)

        fun score(embedding: FloatArray): Float {
            val exemplar = samples.maxOf { normalizedFaceSimilarity(it, embedding) }
            val center = normalizedFaceSimilarity(centroid, embedding)
            return maxOf(exemplar, center)
        }

        fun add(photoId: String, embedding: FloatArray) {
            photoIds += photoId
            for (i in sum.indices) sum[i] += embedding[i]
            centroid = normalizeFaceVector(sum)
            addSample(embedding)
        }

        fun merge(other: Cluster) {
            photoIds += other.photoIds
            for (i in sum.indices) sum[i] += other.sum[i]
            centroid = normalizeFaceVector(sum)
            other.samples.forEach(::addSample)
        }

        private fun addSample(embedding: FloatArray) {
            if (samples.any { normalizedFaceSimilarity(it, embedding) > 0.98f }) return
            if (samples.size < 6) {
                samples += embedding
                return
            }
            // Keep the original cover and replace a redundant appearance with a more distinct one.
            val redundant = (1 until samples.size).maxBy { index ->
                samples.indices.filter { it != index }.maxOf { normalizedFaceSimilarity(samples[index], samples[it]) }
            }
            val redundancy = samples.indices.filter { it != redundant }
                .maxOf { normalizedFaceSimilarity(samples[redundant], samples[it]) }
            if (samples.maxOf { normalizedFaceSimilarity(it, embedding) } < redundancy) samples[redundant] = embedding
        }
    }

    private val mutableClusters = mutableListOf<Cluster>()
    val clusters: List<Cluster> get() = mutableClusters

    fun add(photoId: String, faceKey: String, vector: FloatArray) {
        if (vector.isEmpty() || vector.any { !it.isFinite() }) return
        val embedding = normalizeFaceVector(vector)
        if (embedding.none { it != 0f }) return
        val matches = mutableClusters.mapNotNull { cluster ->
            if (photoId in cluster.photoIds) null else {
                val score = cluster.score(embedding)
                if (score >= 0.5f) cluster to score else null
            }
        }.sortedByDescending { it.second }.map { it.first }
        val best = matches.firstOrNull()
        if (best == null) {
            mutableClusters += Cluster(faceKey, photoId, embedding)
            return
        }
        best.add(photoId, embedding)
        // A new pose can connect two previously separate groups. People appearing together
        // cannot be merged, even if one embedding is unusually similar.
        for (candidate in matches.drop(1)) {
            if (candidate.photoIds.none { it in best.photoIds }) {
                best.merge(candidate)
                mutableClusters.remove(candidate)
            }
        }
    }
}
