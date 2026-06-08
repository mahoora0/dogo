package com.example.dogo.service.animal;

import com.example.dogo.dto.animal.AnimalReportCreateRequest;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportImage;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.animal.AnimalReportImageRepository;
import com.example.dogo.repository.animal.AnimalReportMatchRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.area.AreaRepository;
import com.example.dogo.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalReportServiceEditTest {

	@Mock
	private AnimalReportRepository animalReportRepository;

	@Mock
	private AnimalReportImageRepository animalReportImageRepository;

	@Mock
	private AnimalReportMatchRepository animalReportMatchRepository;

	@Mock
	private AreaRepository areaRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@TempDir
	private Path uploadDir;

	private AnimalReportService animalReportService;

	@BeforeEach
	void setUp() {
		animalReportService = new AnimalReportService(
				animalReportRepository,
				animalReportImageRepository,
				animalReportMatchRepository,
				areaRepository,
				userRepository,
				eventPublisher,
				Optional.empty(),
				uploadDir.toString()
		);
	}

	@Test
	void getForEditReturnsEditData() {
		AnimalReport report = reportWithOwner(1L, 10L);
		when(animalReportRepository.findById(1L)).thenReturn(Optional.of(report));
		when(animalReportImageRepository.findByAnimalReportOrderBySortOrderAscImageIdAsc(report))
				.thenReturn(List.of());

		User loginUser = userWithNo(10L);

		var result = animalReportService.getForEdit(1L, loginUser);

		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.reportType()).isEqualTo("MISSING");
		assertThat(result.animalType()).isEqualTo("DOG");
		assertThat(result.careLocationAddress()).isNull();
		assertThat(result.existingImageUrls()).isEmpty();
	}

	@Test
	void createAllowsOnlyMissingAndSightingReportTypesForUserInput() {
		AnimalReportCreateRequest req = validRequest();
		req.setReportType("RETURNED");

		assertThatThrownBy(() -> animalReportService.create(req, userWithNo(10L)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("신고 구분을 선택해주세요.");
	}

	@Test
	void createRequiresCareLocationAddressWhenTransferred() {
		AnimalReportCreateRequest req = validRequest();
		req.setReportType("SIGHTING");
		req.setSightingCareStatus("TRANSFERRED");

		assertThatThrownBy(() -> animalReportService.create(req, userWithNo(10L)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("동물을 인계한 장소의 주소를 입력해주세요.");
	}

	@Test
	void createIgnoresCareLocationWhenSightingIsProtected() {
		when(areaRepository.findByAreaName(any())).thenReturn(Optional.empty());
		when(animalReportRepository.save(any(AnimalReport.class))).thenAnswer(invocation -> {
			AnimalReport report = invocation.getArgument(0);
			ReflectionTestUtils.setField(report, "reportId", 100L);
			return report;
		});
		AnimalReportCreateRequest req = validRequest();
		req.setReportType("SIGHTING");
		req.setSightingCareStatus("PROTECTING");
		req.setCareLocationName("임시 보호처");
		req.setCareLocationAddress("서울특별시 강남구 보호로 1");
		req.setCareContactPhone("010-2222-3333");

		Long reportId = animalReportService.create(req, userWithNo(10L));

		assertThat(reportId).isEqualTo(100L);
		ArgumentCaptor<AnimalReport> captor = ArgumentCaptor.forClass(AnimalReport.class);
		verify(animalReportRepository).save(captor.capture());
		assertThat(captor.getValue().getSightingCareStatus()).isEqualTo("PROTECTING");
		assertThat(captor.getValue().getCareLocationName()).isNull();
		assertThat(captor.getValue().getCareLocationAddress()).isNull();
		assertThat(captor.getValue().getCareContactPhone()).isNull();
	}

	@Test
	void createSavesCareLocationWhenSightingIsTransferred() {
		when(areaRepository.findByAreaName(any())).thenReturn(Optional.empty());
		when(animalReportRepository.save(any(AnimalReport.class))).thenAnswer(invocation -> {
			AnimalReport report = invocation.getArgument(0);
			ReflectionTestUtils.setField(report, "reportId", 100L);
			return report;
		});
		AnimalReportCreateRequest req = validRequest();
		req.setReportType("SIGHTING");
		req.setSightingCareStatus("TRANSFERRED");
		req.setCareLocationName("연계 동물병원");
		req.setCareLocationAddress("서울특별시 강남구 병원로 2");
		req.setCareContactPhone("02-111-2222");

		Long reportId = animalReportService.create(req, userWithNo(10L));

		assertThat(reportId).isEqualTo(100L);
		ArgumentCaptor<AnimalReport> captor = ArgumentCaptor.forClass(AnimalReport.class);
		verify(animalReportRepository).save(captor.capture());
		assertThat(captor.getValue().getCareLocationName()).isEqualTo("연계 동물병원");
		assertThat(captor.getValue().getCareLocationAddress()).isEqualTo("서울특별시 강남구 병원로 2");
		assertThat(captor.getValue().getCareContactPhone()).isEqualTo("02-111-2222");
	}

	@Test
	void getForEditThrowsWhenNotFound() {
		when(animalReportRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> animalReportService.getForEdit(99L, userWithNo(1L)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("동물 신고 게시글을 찾을 수 없습니다.");
	}

	@Test
	void getForEditThrowsWhenOwnershipMismatch() {
		AnimalReport report = reportWithOwner(2L, 10L);
		when(animalReportRepository.findById(2L)).thenReturn(Optional.of(report));

		assertThatThrownBy(() -> animalReportService.getForEdit(2L, userWithNo(99L)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("수정 권한이 없습니다.");
	}

	@Test
	void updateChangesReportFields() {
		AnimalReport report = reportWithOwner(3L, 10L);
		when(animalReportRepository.findById(3L)).thenReturn(Optional.of(report));
		when(areaRepository.findByAreaName(any())).thenReturn(Optional.empty());

		User loginUser = userWithNo(10L);
		AnimalReportCreateRequest req = validRequest();
		req.setAnimalType("CAT");
		req.setDetailPlace("수정된 장소");
		req.setReportType("SIGHTING");
		req.setSightingCareStatus("TRANSFERRED");
		req.setCareLocationName("연계 동물병원");
		req.setCareLocationAddress("서울특별시 강남구 병원로 2");
		req.setCareContactPhone("02-111-2222");

		animalReportService.update(3L, req, loginUser);

		assertThat(report.getAnimalType()).isEqualTo("CAT");
		verify(animalReportMatchRepository).deleteByMissingReport_ReportId(3L);
		verify(animalReportMatchRepository).deleteBySightingReport_ReportId(3L);
		verify(animalReportMatchRepository).flush();
		assertThat(report.getDetailPlace()).isEqualTo("수정된 장소");
		assertThat(report.getSightingCareStatus()).isEqualTo("TRANSFERRED");
		assertThat(report.getCareLocationName()).isEqualTo("연계 동물병원");
		assertThat(report.getCareLocationAddress()).isEqualTo("서울특별시 강남구 병원로 2");
		assertThat(report.getCareContactPhone()).isEqualTo("02-111-2222");
	}

	@Test
	void updateReplacesImagesWhenNewImagesUploaded() throws Exception {
		AnimalReport report = reportWithOwner(4L, 10L);
		AnimalReportImage oldImage = new AnimalReportImage(report, "old.jpg", "old.jpg",
				"/uploads/animal-reports/old.jpg", "image/jpeg", 100L, 0);

		Path animalDir = uploadDir.resolve("animal-reports");
		Files.createDirectories(animalDir);
		Files.writeString(animalDir.resolve("old.jpg"), "old-data");

		when(animalReportRepository.findById(4L)).thenReturn(Optional.of(report));
		when(animalReportImageRepository.findByAnimalReportOrderBySortOrderAscImageIdAsc(report))
				.thenReturn(List.of(oldImage));
		when(areaRepository.findByAreaName(any())).thenReturn(Optional.empty());

		MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "new-data".getBytes());
		AnimalReportCreateRequest req = validRequest();
		req.setImage(newImage);

		animalReportService.update(4L, req, userWithNo(10L));

		verify(animalReportImageRepository).deleteAll(List.of(oldImage));
		assertThat(Files.exists(animalDir.resolve("old.jpg"))).isFalse();

		ArgumentCaptor<AnimalReportImage> captor = ArgumentCaptor.forClass(AnimalReportImage.class);
		verify(animalReportImageRepository).save(captor.capture());
		assertThat(captor.getValue().getImageUrl()).startsWith("/uploads/animal-reports/");
	}

	@Test
	void updateKeepsExistingImagesWhenNoNewImages() {
		AnimalReport report = reportWithOwner(5L, 10L);
		when(animalReportRepository.findById(5L)).thenReturn(Optional.of(report));
		when(areaRepository.findByAreaName(any())).thenReturn(Optional.empty());

		animalReportService.update(5L, validRequest(), userWithNo(10L));

		verify(animalReportImageRepository, never()).deleteAll(any());
		verify(animalReportImageRepository, never()).save(any());
	}

	private AnimalReport reportWithOwner(Long reportId, Long ownerUserNo) {
		User owner = userWithNo(ownerUserNo);
		AnimalReport report = new AnimalReport(
				owner, "MISSING", "실종 신고",
				LocalDate.of(2026, 5, 1), null, null,
				"서울특별시 강남구", "강남역 2번 출구",
				"010-1234-5678", true, null,
				"DOG", "말티즈", "MALE", "UNKNOWN",
				2, "YEAR", null, "흰색", null, null
		);
		ReflectionTestUtils.setField(report, "reportId", reportId);
		return report;
	}

	private User userWithNo(Long userNo) {
		User user = new User("user" + userNo + "@dogo.local", "사용자" + userNo, "010-0000-0000");
		ReflectionTestUtils.setField(user, "userNo", userNo);
		return user;
	}

	private AnimalReportCreateRequest validRequest() {
		AnimalReportCreateRequest req = new AnimalReportCreateRequest();
		req.setReportType("MISSING");
		req.setEventDate(LocalDate.of(2026, 5, 1));
		req.setRegionName("서울특별시 강남구");
		req.setDetailPlace("강남역 2번 출구");
		req.setAnimalType("DOG");
		req.setGender("MALE");
		req.setNeuteredStatus("UNKNOWN");
		return req;
	}
}
