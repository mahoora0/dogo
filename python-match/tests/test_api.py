"""
FastAPI 엔드포인트 테스트. TestClient 사용 (서버 실행 불필요).
"""
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.similarity import MODEL_NAME

client = TestClient(app)


class TestHealth:
    def test_status_ok(self):
        resp = client.get("/health")
        assert resp.status_code == 200

    def test_response_shape(self):
        resp = client.get("/health")
        body = resp.json()
        assert body["status"] == "ok"
        assert body["model"] == MODEL_NAME


class TestSimilarityEndpoint:
    def _payload(self, candidates: list[dict] | None = None) -> dict:
        return {
            "query": {
                "id": 1,
                "type": "LOST",
                "itemName": "검정 명함지갑",
                "title": "강남역에서 검정 명함지갑 잃어버렸어요",
            },
            "candidates": candidates or [],
        }

    def test_empty_candidates(self):
        resp = client.post("/similarity", json=self._payload([]))
        assert resp.status_code == 200
        body = resp.json()
        assert body["results"] == []
        assert body["model"] == MODEL_NAME

    def test_single_candidate_shape(self):
        payload = self._payload([
            {
                "id": 10,
                "type": "FOUND",
                "itemName": "카드케이스",
                "title": "강남역 카드케이스 습득",
            }
        ])
        resp = client.post("/similarity", json=payload)
        assert resp.status_code == 200
        body = resp.json()
        assert len(body["results"]) == 1
        result = body["results"][0]
        assert result["candidateId"] == 10
        assert isinstance(result["semanticScore"], float)
        assert isinstance(result["reasons"], list)

    def test_multiple_candidates(self):
        payload = self._payload([
            {"id": i, "type": "FOUND", "itemName": f"물건{i}", "title": f"제목{i}"}
            for i in range(1, 4)
        ])
        resp = client.post("/similarity", json=payload)
        assert resp.status_code == 200
        body = resp.json()
        assert len(body["results"]) == 3

    def test_invalid_request_missing_query(self):
        resp = client.post("/similarity", json={"candidates": []})
        assert resp.status_code == 422
