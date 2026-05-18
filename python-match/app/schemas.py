from pydantic import BaseModel


class MatchItem(BaseModel):
    id: int
    type: str
    itemName: str | None = None
    title: str | None = None
    category: str | None = None
    color: str | None = None
    area: str | None = None
    place: str | None = None
    content: str | None = None


class SimilarityRequest(BaseModel):
    query: MatchItem
    candidates: list[MatchItem]


class CandidateResult(BaseModel):
    candidateId: int
    semanticScore: float
    reasons: list[str]


class SimilarityResponse(BaseModel):
    model: str
    results: list[CandidateResult]


class HealthResponse(BaseModel):
    status: str
    model: str


class EmbeddingItem(BaseModel):
    id: int
    text: str


class EmbeddingVector(BaseModel):
    id: int
    vector: list[float]


class EmbeddingsRequest(BaseModel):
    items: list[EmbeddingItem]


class EmbeddingsResponse(BaseModel):
    model: str
    embeddings: list[EmbeddingVector]


class PetImageEmbeddingResponse(BaseModel):
    vector: list[float]
    model: str
    cropType: str = "ORIGINAL_FALLBACK"


class PetImageEmbeddingItem(BaseModel):
    id: int
    vector: list[float]
    model: str
    cropType: str = "ORIGINAL_FALLBACK"


class PetImageEmbeddingsResponse(BaseModel):
    embeddings: list[PetImageEmbeddingItem]
