package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.MissingPersonCreateRequest;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissingPersonServiceTest {

	private final MissingPersonRepository missingPersonRepository = mock(MissingPersonRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final MissingPersonService missingPersonService = new MissingPersonService(missingPersonRepository, userRepository);

	@Test
	void createStoresRequiredMissingPersonFields() {
		MissingPersonCreateRequest request = createRequest();
		when(userRepository.findByEmail("dev@dogo.local")).thenReturn(Optional.of(new User("dev@dogo.local", "개발용 사용자", "010-0000-0000")));
		when(missingPersonRepository.save(any(MissingPersonReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

		missingPersonService.create(request, null);

		verify(missingPersonRepository).save(any(MissingPersonReport.class));
	}

	@Test
	void createRejectsMissingRequiredFields() {
		MissingPersonCreateRequest request = createRequest();
		request.setOccurredPlace(" ");

		assertThatThrownBy(() -> missingPersonService.create(request, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("발생장소");
	}

	@Test
	void searchReturnsListViews() {
		MissingPersonReport report = new MissingPersonReport(
				new User("dev@dogo.local", "개발용 사용자", "010-0000-0000"),
				13,
				"대한민국",
				LocalDateTime.of(2026, 5, 18, 9, 30),
				"서울 강남역",
				170,
				new BigDecimal("58.0"),
				"마른 체형",
				"계란형",
				"검정",
				"짧은 머리",
				"흰색 후드티"
		);
		when(missingPersonRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(report)));

		var result = missingPersonService.search("김", "OPEN", PageRequest.of(0, 9));

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).summary()).isEqualTo("13세 대한민국 실종자");
		assertThat(result.getContent().get(0).statusLabel()).isEqualTo("접수");
	}

	private MissingPersonCreateRequest createRequest() {
		MissingPersonCreateRequest request = new MissingPersonCreateRequest();
		request.setAge(13);
		request.setNationality("대한민국");
		request.setOccurredAt(LocalDateTime.of(2026, 5, 18, 9, 30));
		request.setOccurredPlace("서울 강남역");
		request.setHeightCm(170);
		request.setWeightKg(new BigDecimal("58.0"));
		request.setBodyType("마른 체형");
		request.setFaceShape("계란형");
		request.setHairColor("검정");
		request.setHairStyle("짧은 머리");
		request.setClothing("흰색 후드티");
		return request;
	}
}
