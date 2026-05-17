from contextlib import asynccontextmanager

from fastapi import FastAPI, UploadFile, File, Form

from app.clip_embedding import CLIP_MODEL_NAME, _load_clip_model, encode_image
from app.logger import get_logger
from app.schemas import (
    EmbeddingsRequest,
    EmbeddingsResponse,
    HealthResponse,
    PetImageEmbeddingResponse,
    SimilarityRequest,
    SimilarityResponse,
)
from app.similarity import MODEL_BACKEND, MODEL_NAME, _load_model, compute_embeddings, compute_similarity

logger = get_logger("main")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("dogo-python-match 서버 시작")
    try:
        _load_model()
    except Exception:
        logger.exception("텍스트 모델 사전 로딩 실패")
    try:
        _load_clip_model()
    except Exception:
        logger.exception("CLIP 모델 사전 로딩 실패")
    logger.info(f"서버 준비 완료 | 텍스트 모델={MODEL_NAME} | CLIP 모델={CLIP_MODEL_NAME} | backend={MODEL_BACKEND}")
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


@app.post("/embeddings", response_model=EmbeddingsResponse)
def embeddings(request: EmbeddingsRequest) -> EmbeddingsResponse:
    vectors = compute_embeddings(request.items)
    return EmbeddingsResponse(model=MODEL_NAME, embeddings=vectors)


@app.post("/pet-image-embedding", response_model=PetImageEmbeddingResponse)
async def pet_image_embedding(
    image: UploadFile = File(...),
    animalType: str | None = Form(None),
) -> PetImageEmbeddingResponse:
    image_bytes = await image.read()
    vector, model, crop_type = encode_image(image_bytes)
    return PetImageEmbeddingResponse(vector=vector, model=model, cropType=crop_type)
