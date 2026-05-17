package com.example.dogo.service.match.embedding;

import java.util.List;

public record EmbeddingVector(long id, List<Float> vector) {
}
