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
