package com.example.dogo.service.police.station;

import com.example.dogo.dto.police.PoliceFoundItemDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PoliceStationAddressResolver {

	private static final Logger log = LoggerFactory.getLogger(PoliceStationAddressResolver.class);
	private static final String RESOURCE_PATH = "data/police-station-addresses-20251231.csv";
	private static final String CONTACT_ALIAS_RESOURCE_PATH = "data/police-station-contact-aliases.csv";
	private static final Charset CSV_CHARSET = Charset.forName("MS949");
	private static final int MIN_MATCH_SCORE = 125;

	private volatile List<StationAddress> cachedStations;
	private volatile List<ContactAlias> cachedContactAliases;

	public Optional<String> resolveFoundArea(PoliceFoundItemDetailResponse detail, String regionName) {
		if (detail == null) {
			return Optional.empty();
		}
		return resolveFoundArea(detail.depPlace(), detail.orgNm(), detail.orgId(), detail.tel(), regionName);
	}

	public Optional<String> resolveFoundArea(String depPlace, String orgNm, String regionName) {
		return resolveFoundArea(depPlace, orgNm, null, null, regionName);
	}

	public Optional<String> resolveFoundArea(String depPlace, String orgNm, String orgId, String tel, String regionName) {
		String normalizedDepPlace = normalizeStationName(depPlace);
		if (!StringUtils.hasText(normalizedDepPlace)) {
			return Optional.empty();
		}

		String normalizedOrgName = normalizePoliceStationName(orgNm);
		boolean stationTypeHint = hasStationType(depPlace);
		StationAddress bestMatch = null;
		int bestScore = 0;
		boolean tied = false;

		for (StationAddress station : stations()) {
			int score = matchScore(station, normalizedDepPlace, normalizedOrgName, regionName, stationTypeHint);
			if (score > bestScore) {
				bestMatch = station;
				bestScore = score;
				tied = false;
			} else if (score == bestScore && score > 0) {
				tied = true;
			}
		}

		if (bestMatch == null || bestScore < MIN_MATCH_SCORE || tied) {
			Optional<String> aliasArea = resolveByContactAlias(orgId, tel, normalizedDepPlace, regionName);
			if (aliasArea.isPresent()) {
				return aliasArea;
			}
			return resolveByPoliceOffice(normalizedDepPlace, normalizedOrgName, regionName);
		}
		return areaFromAddress(bestMatch.address());
	}

	private Optional<String> resolveByContactAlias(String orgId, String tel, String normalizedDepPlace, String regionName) {
		List<ContactAlias> matchedAliases = contactAliases().stream()
				.filter(alias -> alias.matches(orgId, tel))
				.toList();
		if (matchedAliases.isEmpty()) {
			return Optional.empty();
		}

		for (ContactAlias alias : matchedAliases) {
			String normalizedAliasOffice = normalizePoliceStationName(alias.policeStationName());
			String normalizedAliasStation = normalizeStationName(alias.stationName());
			List<String> areas = stations().stream()
					.filter(station -> policeOfficeMatches(station, normalizedAliasOffice))
					.filter(station -> stationNameMatches(station, normalizedAliasStation, normalizedDepPlace))
					.filter(station -> !StringUtils.hasText(regionKey(regionName)) || regionMatches(station, regionName))
					.map(station -> areaFromAddress(station.address()))
					.flatMap(Optional::stream)
					.distinct()
					.toList();
			if (areas.size() == 1) {
				return Optional.of(areas.get(0));
			}
		}
		return Optional.empty();
	}

	private boolean stationNameMatches(StationAddress station, String normalizedAliasStation, String normalizedDepPlace) {
		if (StringUtils.hasText(normalizedAliasStation)) {
			return normalizedAliasStation.equals(station.normalizedStationName());
		}
		return StringUtils.hasText(normalizedDepPlace)
				&& normalizedDepPlace.equals(station.normalizedStationName());
	}

	private Optional<String> resolveByPoliceOffice(String normalizedDepPlace, String normalizedOrgName, String regionName) {
		List<String> candidates = new ArrayList<>();
		addCandidate(candidates, normalizePoliceStationName(normalizedDepPlace));
		addCandidate(candidates, normalizedOrgName);

		for (String candidate : candidates) {
			List<String> areas = stations().stream()
					.filter(station -> policeOfficeMatches(station, candidate))
					.filter(station -> !StringUtils.hasText(regionKey(regionName)) || regionMatches(station, regionName))
					.map(station -> areaFromAddress(station.address()))
					.flatMap(Optional::stream)
					.distinct()
					.toList();
			if (areas.size() == 1) {
				return Optional.of(areas.get(0));
			}
		}
		return Optional.empty();
	}

	private void addCandidate(List<String> candidates, String candidate) {
		if (StringUtils.hasText(candidate) && !candidates.contains(candidate)) {
			candidates.add(candidate);
		}
	}

	private boolean policeOfficeMatches(StationAddress station, String candidate) {
		return StringUtils.hasText(candidate)
				&& (candidate.equals(station.normalizedPoliceStationName())
				|| candidate.contains(station.normalizedPoliceStationName())
				|| station.normalizedPoliceStationName().contains(candidate));
	}

	private int matchScore(
			StationAddress station,
			String normalizedDepPlace,
			String normalizedOrgName,
			String regionName,
			boolean stationTypeHint
	) {
		int score = 0;
		if (normalizedDepPlace.equals(station.normalizedStationName())) {
			score += 100;
		} else if (normalizedDepPlace.contains(station.normalizedStationName())
				|| station.normalizedStationName().contains(normalizedDepPlace)) {
			score += 70;
		} else {
			return 0;
		}

		boolean officeMatched = false;
		if (StringUtils.hasText(normalizedOrgName)) {
			if (normalizedOrgName.equals(station.normalizedPoliceStationName())) {
				score += 50;
				officeMatched = true;
			} else if (normalizedOrgName.contains(station.normalizedPoliceStationName())
					|| station.normalizedPoliceStationName().contains(normalizedOrgName)) {
				score += 35;
				officeMatched = true;
			}
		}

		if (!stationTypeHint && !officeMatched) {
			return 0;
		}

		if (regionMatches(station, regionName)) {
			score += 25;
		}
		return score;
	}

	private boolean hasStationType(String value) {
		return StringUtils.hasText(value)
				&& (value.contains("지구대")
				|| value.contains("파출소")
				|| value.contains("치안센터")
				|| value.contains("출장소"));
	}

	private boolean regionMatches(StationAddress station, String regionName) {
		String regionKey = regionKey(regionName);
		if (!StringUtils.hasText(regionKey)) {
			return false;
		}
		return station.sidoOffice().contains(regionKey) || station.address().contains(regionKey);
	}

	private Optional<String> areaFromAddress(String address) {
		if (!StringUtils.hasText(address)) {
			return Optional.empty();
		}

		String[] parts = address.trim().replaceAll("\\s+", " ").split(" ");
		if (parts.length < 2) {
			return Optional.empty();
		}

		String province = abbreviateProvince(parts[0]);
		if (!StringUtils.hasText(province)) {
			return Optional.empty();
		}

		String district = parts[1];
		if (!isAdministrativeUnit(district)) {
			return Optional.of(province);
		}

		if (parts.length > 2 && district.endsWith("시") && parts[2].endsWith("구")) {
			district = district + " " + parts[2];
		}
		return Optional.of(province + ", " + district);
	}

	private boolean isAdministrativeUnit(String value) {
		return StringUtils.hasText(value)
				&& (value.endsWith("시") || value.endsWith("군") || value.endsWith("구"));
	}

	private String abbreviateProvince(String value) {
		return switch (value) {
			case "서울특별시" -> "서울";
			case "부산광역시" -> "부산";
			case "대구광역시" -> "대구";
			case "인천광역시" -> "인천";
			case "광주광역시" -> "광주";
			case "대전광역시" -> "대전";
			case "울산광역시" -> "울산";
			case "세종특별자치시" -> "세종";
			case "경기도" -> "경기";
			case "강원도", "강원특별자치도" -> "강원";
			case "충청북도" -> "충북";
			case "충청남도" -> "충남";
			case "전라북도", "전북특별자치도" -> "전북";
			case "전라남도" -> "전남";
			case "경상북도" -> "경북";
			case "경상남도" -> "경남";
			case "제주특별자치도" -> "제주";
			default -> value;
		};
	}

	private String regionKey(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String normalized = value.replaceAll("\\s+", "");
		if (normalized.contains("서울")) {
			return "서울";
		}
		if (normalized.contains("부산")) {
			return "부산";
		}
		if (normalized.contains("대구")) {
			return "대구";
		}
		if (normalized.contains("인천")) {
			return "인천";
		}
		if (normalized.contains("광주")) {
			return "광주";
		}
		if (normalized.contains("대전")) {
			return "대전";
		}
		if (normalized.contains("울산")) {
			return "울산";
		}
		if (normalized.contains("세종")) {
			return "세종";
		}
		if (normalized.contains("경기")) {
			return "경기";
		}
		if (normalized.contains("강원")) {
			return "강원";
		}
		if (normalized.contains("충북") || normalized.contains("충청북")) {
			return "충북";
		}
		if (normalized.contains("충남") || normalized.contains("충청남")) {
			return "충남";
		}
		if (normalized.contains("전북") || normalized.contains("전라북")) {
			return "전북";
		}
		if (normalized.contains("전남") || normalized.contains("전라남")) {
			return "전남";
		}
		if (normalized.contains("경북") || normalized.contains("경상북")) {
			return "경북";
		}
		if (normalized.contains("경남") || normalized.contains("경상남")) {
			return "경남";
		}
		if (normalized.contains("제주")) {
			return "제주";
		}
		return null;
	}

	private String normalizePoliceStationName(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.replaceAll("\\([^)]*\\)", "")
				.replaceAll("\\s+", "")
				.replace("경찰서", "")
				.trim();
	}

	private String normalizeStationName(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.replaceAll("\\([^)]*\\)", "")
				.replaceAll("\\s+", "")
				.replace("지구대", "")
				.replace("파출소", "")
				.replace("치안센터", "")
				.replace("출장소", "")
				.trim();
	}

	private List<StationAddress> stations() {
		List<StationAddress> stations = cachedStations;
		if (stations == null) {
			synchronized (this) {
				stations = cachedStations;
				if (stations == null) {
					stations = loadStations();
					cachedStations = stations;
				}
			}
		}
		return stations;
	}

	private List<ContactAlias> contactAliases() {
		List<ContactAlias> aliases = cachedContactAliases;
		if (aliases == null) {
			synchronized (this) {
				aliases = cachedContactAliases;
				if (aliases == null) {
					aliases = loadContactAliases();
					cachedContactAliases = aliases;
				}
			}
		}
		return aliases;
	}

	private List<StationAddress> loadStations() {
		ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
		if (!resource.exists()) {
			log.warn("Police station address CSV not found: {}", RESOURCE_PATH);
			return List.of();
		}

		List<StationAddress> stations = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), CSV_CHARSET))) {
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				List<String> fields = parseCsvLine(line);
				if (fields.size() < 6) {
					continue;
				}
				String policeStationName = fields.get(2).trim();
				String stationName = fields.get(3).trim();
				String address = fields.get(5).trim();
				if (!StringUtils.hasText(stationName) || !StringUtils.hasText(address)) {
					continue;
				}
				stations.add(new StationAddress(
						fields.get(1).trim(),
						policeStationName,
						stationName,
						fields.get(4).trim(),
						address,
						normalizePoliceStationName(policeStationName),
						normalizeStationName(stationName)
				));
			}
		} catch (Exception exception) {
			log.warn("Failed to load police station address CSV: {}", RESOURCE_PATH, exception);
			return List.of();
		}
		return List.copyOf(stations);
	}

	private List<ContactAlias> loadContactAliases() {
		ClassPathResource resource = new ClassPathResource(CONTACT_ALIAS_RESOURCE_PATH);
		if (!resource.exists()) {
			return List.of();
		}

		List<ContactAlias> aliases = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				List<String> fields = parseCsvLine(line);
				if (fields.size() < 4) {
					continue;
				}
				aliases.add(new ContactAlias(
						blankToNull(fields.get(0)),
						digitsOnly(fields.get(1)),
						blankToNull(fields.get(2)),
						blankToNull(fields.get(3))
				));
			}
		} catch (Exception exception) {
			log.warn("Failed to load police station contact alias CSV: {}", CONTACT_ALIAS_RESOURCE_PATH, exception);
			return List.of();
		}
		return List.copyOf(aliases);
	}

	private List<String> parseCsvLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean inQuotes = false;
		for (int index = 0; index < line.length(); index++) {
			char current = line.charAt(index);
			if (current == '"') {
				if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					field.append('"');
					index++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (current == ',' && !inQuotes) {
				fields.add(field.toString());
				field.setLength(0);
			} else {
				field.append(current);
			}
		}
		fields.add(field.toString());
		return fields;
	}

	private String digitsOnly(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String digits = value.replaceAll("\\D", "");
		return StringUtils.hasText(digits) ? digits : null;
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private record StationAddress(
			String sidoOffice,
			String policeStationName,
			String stationName,
			String type,
			String address,
			String normalizedPoliceStationName,
			String normalizedStationName
	) {
	}

	private record ContactAlias(
			String orgId,
			String telPrefix,
			String policeStationName,
			String stationName
	) {

		boolean matches(String candidateOrgId, String candidateTel) {
			if (StringUtils.hasText(orgId) && orgId.equals(blank(candidateOrgId))) {
				return true;
			}
			String normalizedTel = candidateTel == null ? null : candidateTel.replaceAll("\\D", "");
			return StringUtils.hasText(telPrefix)
					&& StringUtils.hasText(normalizedTel)
					&& normalizedTel.startsWith(telPrefix);
		}

		private String blank(String value) {
			if (!StringUtils.hasText(value)) {
				return null;
			}
			return value.trim();
		}
	}
}
