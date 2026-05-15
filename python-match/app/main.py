from contextlib import asynccontextmanager

from fastapi import FastAPI, Request

from app.logger import get_logger
from app.schemas import HealthResponse, SimilarityRequest, SimilarityResponse
from app.similarity import MODEL_BACKEND, MODEL_NAME, _load_model, compute_similarity

logger = get_logger("main")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("dogo-python-match 서버 시작")
    _load_model()
    logger.info(f"서버 준비 완료 | 모델={MODEL_NAME} | backend={MODEL_BACKEND}")
    yield
    logger.info("서버 종료")


app = FastAPI(title="dogo-python-match", version="0.1.0", lifespan=lifespan)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    logger.debug("헬스 체크 요청")
    return HealthResponse(status="ok", model=MODEL_NAME)


@app.post("/similarity", response_model=SimilarityResponse)
def similarity(request: SimilarityRequest) -> SimilarityResponse:
    results = compute_similarity(request.query, request.candidates)
    return SimilarityResponse(model=MODEL_NAME, results=results)
