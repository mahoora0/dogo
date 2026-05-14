from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.schemas import HealthResponse, SimilarityRequest, SimilarityResponse
from app.similarity import MODEL_NAME, _load_model, compute_similarity


@asynccontextmanager
async def lifespan(app: FastAPI):
    _load_model()
    yield


app = FastAPI(title="dogo-python-match", version="0.1.0", lifespan=lifespan)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", model=MODEL_NAME)


@app.post("/similarity", response_model=SimilarityResponse)
def similarity(request: SimilarityRequest) -> SimilarityResponse:
    results = compute_similarity(request.query, request.candidates)
    return SimilarityResponse(model=MODEL_NAME, results=results)
