package com.example.dogo.service.match.embedding;

import java.util.List;

public record EmbeddingsResponse(String model, List<EmbeddingVector> embeddings) {
}
