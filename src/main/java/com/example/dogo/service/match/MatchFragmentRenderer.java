package com.example.dogo.service.match;

import com.example.dogo.dto.animal.AnimalMatchCandidateView;
import com.example.dogo.dto.match.MatchCandidateView;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MatchFragmentRenderer {

	private final SpringTemplateEngine templateEngine;

	public MatchFragmentRenderer(SpringTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	public String renderLostItemMatches(List<MatchCandidateView> candidates) {
		Context ctx = new Context(Locale.KOREAN);
		ctx.setVariable("matchCandidates", candidates);
		return templateEngine.process("fragments/lost-item-match-results",
				Set.of("[th:fragment='matchResults']"), ctx);
	}

	public String renderFoundItemMatches(List<MatchCandidateView> candidates) {
		Context ctx = new Context(Locale.KOREAN);
		ctx.setVariable("matchCandidates", candidates);
		return templateEngine.process("fragments/found-item-match-results",
				Set.of("[th:fragment='matchResults']"), ctx);
	}

	public String renderAnimalReportMatches(List<AnimalMatchCandidateView> candidates, String reportType) {
		Context ctx = new Context(Locale.KOREAN);
		ctx.setVariable("matchCandidates", candidates);
		ctx.setVariable("reportType", reportType);
		return templateEngine.process("fragments/animal-report-match-results",
				Set.of("[th:fragment='matchResults']"), ctx);
	}
}
