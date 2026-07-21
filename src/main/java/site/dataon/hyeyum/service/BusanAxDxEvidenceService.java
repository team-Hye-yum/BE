package site.dataon.hyeyum.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.common.BusanAxDxEvidenceTypes;
import site.dataon.hyeyum.common.BusanAxDxKeywordGroup;
import site.dataon.hyeyum.common.BusanAxDxSourceTypes;
import site.dataon.hyeyum.common.SupportSelectionResults;
import site.dataon.hyeyum.domain.BtpSupportHistory;
import site.dataon.hyeyum.domain.Company;
import site.dataon.hyeyum.domain.CompanyNtisLeadProject;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.BusanAxDxEvidenceResponse;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.EvidenceGroup;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.EvidenceItem;
import site.dataon.hyeyum.repository.BtpSupportHistoryRepository;
import site.dataon.hyeyum.repository.CompanyNtisLeadProjectRepository;
import site.dataon.hyeyum.repository.CompanyRepository;

@Service
public class BusanAxDxEvidenceService {

    private static final String EMPTY_MESSAGE = "디지털 전환 관련 근거가 확인되지 않음";

    private final CompanyRepository companyRepository;
    private final BtpSupportHistoryRepository supportHistoryRepository;
    private final CompanyNtisLeadProjectRepository ntisLeadProjectRepository;

    public BusanAxDxEvidenceService(
            CompanyRepository companyRepository,
            BtpSupportHistoryRepository supportHistoryRepository,
            CompanyNtisLeadProjectRepository ntisLeadProjectRepository) {
        this.companyRepository = companyRepository;
        this.supportHistoryRepository = supportHistoryRepository;
        this.ntisLeadProjectRepository = ntisLeadProjectRepository;
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<BusanAxDxEvidenceResponse> findEvidence(Integer companyId) {
        return new ApiDataResponse<>(evidence(companyId));
    }

    @Transactional(readOnly = true)
    public BusanAxDxEvidenceResponse evidence(Integer companyId) {
        Company company = findCompany(companyId);
        List<CompanyNtisLeadProject> ntisLeadProjects =
                ntisLeadProjectRepository.findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisLeadProjectIdAsc(companyId);
        List<EvidenceItem> executionItems = executionItems(companyId, ntisLeadProjects);
        List<EvidenceItem> relatedItems = relatedIndustryTechItems(ntisLeadProjects, company);
        List<EvidenceGroup> groups = new ArrayList<>();
        if (!executionItems.isEmpty()) {
            groups.add(new EvidenceGroup(BusanAxDxEvidenceTypes.EXECUTION_HISTORY, "실행 이력", executionItems));
        }
        if (!relatedItems.isEmpty()) {
            groups.add(new EvidenceGroup(BusanAxDxEvidenceTypes.RELATED_INDUSTRY_TECH, "연관 산업·기술", relatedItems));
        }
        int evidenceCount = executionItems.size() + relatedItems.size();
        return new BusanAxDxEvidenceResponse(
                evidenceCount > 0,
                evidenceCount,
                groups,
                evidenceCount == 0 ? EMPTY_MESSAGE : null);
    }

    private List<EvidenceItem> executionItems(Integer companyId, List<CompanyNtisLeadProject> ntisLeadProjects) {
        List<EvidenceItem> items = new ArrayList<>();
        for (BtpSupportHistory history : supportHistoryRepository
                .findByCompanyIdAndSelectionResultOrderBySupportYearDescSupportHistoryIdAsc(
                        companyId, SupportSelectionResults.SELECTED)) {
            BusanAxDxKeywordGroup.BTP_PROGRAM_KEYWORDS.match(history.getBudgetProgramName())
                    .ifPresent(keyword -> items.add(new EvidenceItem(
                            BusanAxDxSourceTypes.BTP_SUPPORT_HISTORY,
                            keyword,
                            history.getBudgetProgramName(),
                            history.getSupportYear(),
                            history.getSelectionResult(),
                            displaySupportProgram(history))));
            BusanAxDxKeywordGroup.BTP_ITEM_KEYWORDS.match(history.getSupportItem())
                    .ifPresent(keyword -> items.add(new EvidenceItem(
                            BusanAxDxSourceTypes.BTP_SUPPORT_HISTORY,
                            keyword,
                            history.getSupportItem(),
                            history.getSupportYear(),
                            history.getSelectionResult(),
                            "지원품목: " + history.getSupportItem())));
        }
        for (CompanyNtisLeadProject project : ntisLeadProjects) {
            BusanAxDxKeywordGroup.NTIS_KEYWORDS.match(project.getProjectName())
                    .ifPresent(keyword -> items.add(new EvidenceItem(
                            BusanAxDxSourceTypes.NTIS_LEAD_PROJECT,
                            keyword,
                            project.getProjectName(),
                            project.getReferenceYear(),
                            null,
                            displayNtisProject(project))));
        }
        return items;
    }

    private List<EvidenceItem> relatedIndustryTechItems(List<CompanyNtisLeadProject> ntisLeadProjects, Company company) {
        List<EvidenceItem> items = new ArrayList<>();
        addCompanyProfileEvidence(items, company.getIndustryName());
        addCompanyProfileEvidence(items, company.getMainProduct());
        addCompanyProfileEvidence(items, company.getIndustryDescription());
        for (CompanyNtisLeadProject project : ntisLeadProjects) {
            BusanAxDxKeywordGroup.NTIS_KEYWORDS.match(project.getScienceTechnologyCategoryName())
                    .ifPresent(keyword -> items.add(new EvidenceItem(
                            BusanAxDxSourceTypes.NTIS_LEAD_PROJECT,
                            keyword,
                            project.getScienceTechnologyCategoryName(),
                            project.getReferenceYear(),
                            null,
                            "NTIS 기술분류: " + project.getScienceTechnologyCategoryName())));
        }
        return items;
    }

    private void addCompanyProfileEvidence(List<EvidenceItem> items, String text) {
        BusanAxDxKeywordGroup.COMPANY_PROFILE_KEYWORDS.match(text)
                .ifPresent(keyword -> items.add(new EvidenceItem(
                        BusanAxDxSourceTypes.COMPANY_PROFILE,
                        keyword,
                        text,
                        null,
                        null,
                        text)));
    }

    private String displaySupportProgram(BtpSupportHistory history) {
        return java.util.stream.Stream.of(
                        history.getSupportYear() == null ? null : String.valueOf(history.getSupportYear()),
                        history.getBudgetProgramName(),
                        history.getSelectionResult() == null ? null : "(" + history.getSelectionResult() + ")")
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse(history.getBudgetProgramName());
    }

    private String displayNtisProject(CompanyNtisLeadProject project) {
        if (project.getReferenceYear() == null) {
            return project.getProjectName();
        }
        return project.getReferenceYear() + " " + project.getProjectName();
    }

    private Company findCompany(Integer companyId) {
        return companyRepository
                .findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기업 정보를 찾을 수 없습니다."));
    }

}
