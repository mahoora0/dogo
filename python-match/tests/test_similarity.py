"""
similarity.py 단위 테스트.
실제 모델을 로드하므로 첫 실행 시 시간이 걸릴 수 있습니다.
"""
import pytest

from app.schemas import MatchItem
from app.similarity import build_match_text, compute_similarity


def make_item(id: int, item_name: str | None, title: str | None, type: str = "FOUND") -> MatchItem:
    return MatchItem(id=id, type=type, itemName=item_name, title=title)


# ---------------------------------------------------------------------------
# build_match_text
# ---------------------------------------------------------------------------

class TestBuildMatchText:
    def test_both_fields(self):
        item = make_item(1, "검정 명함지갑", "강남역에서 명함지갑 잃어버렸어요")
        assert build_match_text(item) == "검정 명함지갑"

    def test_only_item_name(self):
        item = make_item(1, "명함지갑", None)
        assert build_match_text(item) == "명함지갑"

    def test_only_title(self):
        item = make_item(1, None, "강남역 습득")
        assert build_match_text(item) == "강남역 습득"

    def test_both_none(self):
        item = make_item(1, None, None)
        assert build_match_text(item) == ""

    def test_whitespace_only(self):
        item = make_item(1, "  ", "   ")
        assert build_match_text(item) == ""


# ---------------------------------------------------------------------------
# compute_similarity (실제 모델 사용)
# ---------------------------------------------------------------------------

class TestComputeSimilarity:
    def _query(self, item_name: str, title: str) -> MatchItem:
        return make_item(0, item_name, title, type="LOST")

    def test_empty_candidates(self):
        query = self._query("검정 명함지갑", "강남역 명함지갑 분실")
        results = compute_similarity(query, [])
        assert results == []

    def test_identical_text_high_score(self):
        query = self._query("검정 명함지갑", "강남역 명함지갑 분실")
        candidate = make_item(10, "검정 명함지갑", "강남역 명함지갑 분실")
        results = compute_similarity(query, [candidate])
        assert len(results) == 1
        assert results[0].candidateId == 10
        assert results[0].semanticScore >= 90.0

    def test_similar_text_reasonable_score(self):
        query = self._query("검정 명함지갑", "강남역에서 검정 명함지갑 잃어버렸어요")
        candidate = make_item(10, "카드케이스", "강남역 카드케이스 습득")
        results = compute_similarity(query, [candidate])
        assert len(results) == 1
        assert 0.0 <= results[0].semanticScore <= 100.0

    def test_unrelated_text_low_score(self):
        query = self._query("검정 명함지갑", "강남역 명함지갑 분실")
        candidate = make_item(20, "자전거", "한강공원 자전거 도난")
        results = compute_similarity(query, [candidate])
        assert len(results) == 1
        assert results[0].semanticScore < 60.0

    def test_empty_candidate_text_score_zero(self):
        query = self._query("검정 명함지갑", "명함지갑 분실")
        candidate = make_item(30, None, None)
        results = compute_similarity(query, [candidate])
        assert len(results) == 1
        assert results[0].semanticScore == 0.0

    def test_empty_query_text_all_zero(self):
        query = self._query(None, None)
        candidates = [
            make_item(1, "검정 명함지갑", "강남역 명함지갑"),
            make_item(2, "자전거", "한강공원 자전거"),
        ]
        results = compute_similarity(query, candidates)
        assert all(r.semanticScore == 0.0 for r in results)

    def test_multiple_candidates_result_count(self):
        query = self._query("명함지갑", "명함지갑 분실")
        candidates = [make_item(i, f"물건{i}", f"제목{i}") for i in range(1, 6)]
        results = compute_similarity(query, candidates)
        assert len(results) == 5

    def test_results_ordered_by_candidate_id(self):
        query = self._query("명함지갑", "명함지갑 분실")
        candidates = [make_item(i, "카드지갑", "지갑 습득") for i in [5, 3, 1, 4, 2]]
        results = compute_similarity(query, candidates)
        ids = [r.candidateId for r in results]
        assert ids == sorted(ids)

    def test_score_range(self):
        query = self._query("검정 지갑", "지갑 분실")
        candidates = [
            make_item(1, "검정 지갑", "지갑 습득"),
            make_item(2, "빨간 모자", "모자 분실"),
        ]
        results = compute_similarity(query, candidates)
        for r in results:
            assert 0.0 <= r.semanticScore <= 100.0

    def test_high_score_has_reason(self):
        query = self._query("검정 명함지갑", "강남역 명함지갑 분실")
        candidate = make_item(10, "검정 명함지갑", "강남역 명함지갑 분실")
        results = compute_similarity(query, [candidate])
        assert "물품명/제목 의미 유사" in results[0].reasons
