package com.example.dogo.service.item;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegistrationOptionService {

	private static final Map<String, List<String>> CATEGORY_SUB_OPTIONS = categorySubOptions();
	private static final List<String> COLOR_OPTIONS = List.of(
			"검정",
			"흰색",
			"회색",
			"빨강",
			"파랑",
			"초록",
			"노랑",
			"분홍",
			"보라",
			"갈색",
			"베이지",
			"금색",
			"은색",
			"여러 색",
			"기타/모름"
	);
	private static final Map<String, List<String>> REGION_DISTRICTS = regionDistricts();

	public List<String> getCategoryMainOptions() {
		return List.copyOf(CATEGORY_SUB_OPTIONS.keySet());
	}

	public Map<String, List<String>> getCategorySubOptions() {
		return CATEGORY_SUB_OPTIONS;
	}

	public List<String> getColorOptions() {
		return COLOR_OPTIONS;
	}

	public List<String> getRegionOptions() {
		return List.copyOf(REGION_DISTRICTS.keySet());
	}

	public Map<String, List<String>> getRegionDistrictOptions() {
		return REGION_DISTRICTS;
	}

	private static Map<String, List<String>> categorySubOptions() {
		Map<String, List<String>> options = new LinkedHashMap<>();
		options.put("가방", List.of("남성용가방", "여성용가방", "기타가방"));
		options.put("귀금속", List.of("반지", "목걸이", "귀걸이", "시계", "기타"));
		options.put("기타물품", List.of("안경", "선글라스", "기타"));
		options.put("도서용품", List.of("소설", "학습서적", "컴퓨터서적", "기타서적"));
		options.put("무주물", List.of("무주물"));
		options.put("산업용품", List.of("기타물품", "기타"));
		options.put("서류", List.of("서류", "기타물품"));
		options.put("쇼핑백", List.of("쇼핑백"));
		options.put("스포츠용품", List.of("스포츠용품"));
		options.put("악기", List.of("관악기", "현악기"));
		options.put("유가증권", List.of("상품권", "어음", "채권", "기타"));
		options.put("유류품", List.of("유류품"));
		options.put("의류", List.of("여성의류", "남성의류", "아기의류", "신발", "모자", "기타의류"));
		options.put("자동차", List.of("자동차열쇠", "자동차번호판", "임시번호판", "기타용품"));
		options.put("전자기기", List.of("태블릿", "스마트워치", "무선이어폰", "카메라", "기타용품"));
		options.put("증명서", List.of("신분증", "면허증", "여권", "기타"));
		options.put("지갑", List.of("남성용 지갑", "여성용 지갑", "기타 지갑"));
		options.put("카드", List.of("신용(체크)카드", "교통카드", "일반카드", "기타카드"));
		options.put("컴퓨터", List.of("삼성노트북", "LG노트북", "애플노트북", "기타"));
		options.put("현금", List.of("현금", "외화", "수표", "기타"));
		options.put("휴대폰", List.of("삼성휴대폰", "아이폰", "LG휴대폰", "기타휴대폰", "기타통신기기"));
		return immutableCopy(options);
	}

	private static Map<String, List<String>> regionDistricts() {
		Map<String, List<String>> options = new LinkedHashMap<>();
		options.put("서울특별시", List.of("강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"));
		options.put("부산광역시", List.of("강서구", "금정구", "기장군", "남구", "동구", "동래구", "부산진구", "북구", "사상구", "사하구", "서구", "수영구", "연제구", "영도구", "중구", "해운대구"));
		options.put("대구광역시", List.of("군위군", "남구", "달서구", "달성군", "동구", "북구", "서구", "수성구", "중구"));
		options.put("인천광역시", List.of("강화군", "계양구", "남동구", "동구", "미추홀구", "부평구", "서구", "연수구", "옹진군", "중구"));
		options.put("광주광역시", List.of("광산구", "남구", "동구", "북구", "서구"));
		options.put("대전광역시", List.of("대덕구", "동구", "서구", "유성구", "중구"));
		options.put("울산광역시", List.of("남구", "동구", "북구", "울주군", "중구"));
		options.put("세종특별자치시", List.of("세종시"));
		options.put("경기도", List.of("가평군", "고양시", "과천시", "광명시", "광주시", "구리시", "군포시", "김포시", "남양주시", "동두천시", "부천시", "성남시", "수원시", "시흥시", "안산시", "안성시", "안양시", "양주시", "양평군", "여주시", "연천군", "오산시", "용인시", "의왕시", "의정부시", "이천시", "파주시", "평택시", "포천시", "하남시", "화성시"));
		options.put("강원특별자치도", List.of("강릉시", "고성군", "동해시", "삼척시", "속초시", "양구군", "양양군", "영월군", "원주시", "인제군", "정선군", "철원군", "춘천시", "태백시", "평창군", "홍천군", "화천군", "횡성군"));
		options.put("충청북도", List.of("괴산군", "단양군", "보은군", "영동군", "옥천군", "음성군", "제천시", "증평군", "진천군", "청주시", "충주시"));
		options.put("충청남도", List.of("계룡시", "공주시", "금산군", "논산시", "당진시", "보령시", "부여군", "서산시", "서천군", "아산시", "예산군", "천안시", "청양군", "태안군", "홍성군"));
		options.put("전북특별자치도", List.of("고창군", "군산시", "김제시", "남원시", "무주군", "부안군", "순창군", "완주군", "익산시", "임실군", "장수군", "전주시", "정읍시", "진안군"));
		options.put("전라남도", List.of("강진군", "고흥군", "곡성군", "광양시", "구례군", "나주시", "담양군", "목포시", "무안군", "보성군", "순천시", "신안군", "여수시", "영광군", "영암군", "완도군", "장성군", "장흥군", "진도군", "함평군", "해남군", "화순군"));
		options.put("경상북도", List.of("경산시", "경주시", "고령군", "구미시", "김천시", "문경시", "봉화군", "상주시", "성주군", "안동시", "영덕군", "영양군", "영주시", "영천시", "예천군", "울릉군", "울진군", "의성군", "청도군", "청송군", "칠곡군", "포항시"));
		options.put("경상남도", List.of("거제시", "거창군", "고성군", "김해시", "남해군", "밀양시", "사천시", "산청군", "양산시", "의령군", "진주시", "창녕군", "창원시", "통영시", "하동군", "함안군", "함양군", "합천군"));
		options.put("제주특별자치도", List.of("서귀포시", "제주시"));
		return immutableCopy(options);
	}

	private static Map<String, List<String>> immutableCopy(Map<String, List<String>> source) {
		Map<String, List<String>> copy = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : source.entrySet()) {
			copy.put(entry.getKey(), List.copyOf(new ArrayList<>(entry.getValue())));
		}
		return Collections.unmodifiableMap(copy);
	}
}
