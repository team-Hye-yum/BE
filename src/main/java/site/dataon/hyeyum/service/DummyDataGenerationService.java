package site.dataon.hyeyum.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.dto.DummyDataGenerationResult;

@Service
public class DummyDataGenerationService {

    private static final int COMPANY_COUNT = 20;
    private static final String[] INDUSTRY_SECTORS = {
        "전자부품",
        "의료기기",
        "소프트웨어",
        "산업장비",
        "정밀기계",
        "자동차부품",
        "조선해양기자재",
        "로봇",
        "바이오소재",
        "식품가공",
        "친환경에너지",
        "이차전지",
        "반도체장비",
        "스마트물류",
        "정보보안",
        "AI데이터",
        "디지털헬스케어",
        "고분자소재",
        "금속가공",
        "환경설비"
    };
    private static final String[] INDUSTRY_SPECIALTIES = {
        "제조업",
        "부품 개발업",
        "장비 제작업",
        "시스템 통합업",
        "품질 시험업",
        "공정 자동화업",
        "소재 가공업",
        "플랫폼 운영업",
        "연구개발업",
        "유지보수 서비스업"
    };
    private static final String[] INDUSTRY_CODE_PREFIXES = {
        "C26", "C27", "J58", "C29", "C28", "C30", "C31", "C29", "C21", "C10",
        "D35", "C28", "C29", "H52", "J62", "J63", "M70", "C22", "C24", "E39"
    };
    private static final String[] INDUSTRIAL_AREAS = {
        "명지녹산국가산업단지",
        "센텀시티산업단지",
        "신평장림산업단지",
        "사상공업지역",
        "미음산업단지",
        "신호산업단지",
        "부산과학산업단지",
        "화전산업단지",
        "강서보고산업단지",
        "생곡산업단지",
        "성우산업단지",
        "풍상산업단지",
        "지사2산업단지",
        "국제산업물류도시",
        "모라도시첨단산업단지",
        "지사글로벌산업단지",
        "정관농공단지",
        "정관산업단지",
        "장안산업단지",
        "기룡산업단지",
        "정관코리산업단지",
        "회동석대도시첨단산업단지",
        "명례산업단지",
        "부산신소재산업단지",
        "반룡산업단지",
        "오리산업단지",
        "에코장안산업단지",
        "동남권의과학산업단지",
        "동부산E-PARK산업단지"
    };
    private static final String[] ROAD_ADDRESSES = {
        "부산광역시 강서구 녹산산단261로",
        "부산광역시 해운대구 센텀중앙로",
        "부산광역시 사하구 다대로",
        "부산광역시 사상구 학감대로",
        "부산광역시 강서구 미음산단로",
        "부산광역시 강서구 과학산단로",
        "부산광역시 강서구 화전산단로",
        "부산광역시 강서구 신호산단로",
        "부산광역시 강서구 지사산단로",
        "부산광역시 강서구 생곡산단로",
        "부산광역시 기장군 정관산단로",
        "부산광역시 기장군 장안산단로",
        "부산광역시 기장군 명례산단로",
        "부산광역시 금정구 개좌로",
        "부산광역시 금정구 회동석대로",
        "부산광역시 북구 모라로",
        "부산광역시 사상구 낙동대로",
        "부산광역시 사하구 을숙도대로",
        "부산광역시 해운대구 센텀동로",
        "부산광역시 해운대구 APEC로",
        "부산광역시 남구 신선로",
        "부산광역시 부산진구 전포대로",
        "부산광역시 동래구 충렬대로",
        "부산광역시 수영구 광안해변로"
    };
    private static final String[] ROAD_REGIONS = {
        "강서구", "해운대구", "사하구", "사상구", "강서구",
        "강서구", "강서구", "강서구", "강서구", "강서구",
        "기장군", "기장군", "기장군", "금정구", "금정구",
        "북구", "사상구", "사하구", "해운대구", "해운대구",
        "남구", "부산진구", "동래구", "수영구"
    };
    private static final String[] COMPANY_PREFIXES = {
        "해온", "동백", "마린", "센텀", "녹산", "미래", "가온", "하이", "비전", "부경",
        "동남", "에코", "오션", "라온", "유니", "케이", "에이치", "제이", "솔라", "네오"
    };
    private static final String[] COMPANY_MIDDLES = {
        "나노", "스마트", "그린", "오토", "메디", "마이크로", "파워", "디지털", "하이브리드", "코어",
        "프라임", "에이스", "넥스", "큐브", "인더스", "모빌", "웨이브", "링크", "퓨처", "랩"
    };
    private static final String[] COMPANY_SUFFIXES = {
        "테크", "정밀", "바이오", "시스템", "솔루션", "이노텍", "메디칼", "엔지니어링", "데이터", "로보틱스"
    };
    private static final String[] SECONDARY_BUSINESS_PURPOSES = {
        "스마트공장 구축 및 설비 유지보수업",
        "산업용 데이터 분석 및 플랫폼 운영업",
        "연구개발 컨설팅 및 시험평가 서비스업",
        "시제품 설계 및 양산 전환 지원업",
        "자동화 설비 통합 및 공정 개선업",
        "품질 인증 대응 및 기술문서 작성업",
        "산업용 소프트웨어 개발 및 공급업",
        "부품 조달 관리 및 기술지원 서비스업",
        "친환경 공정 전환 및 에너지 진단업",
        "디지털 전환 솔루션 구축업",
        "공동 연구개발 과제 수행업",
        "제품 신뢰성 시험 및 성능평가업",
        "생산관리 시스템 구축 및 운영업",
        "기술사업화 전략 수립 및 시장검증업",
        "산업안전 모니터링 장비 개발업",
        "설비 예지보전 알고리즘 개발업",
        "공공 실증사업 운영 및 성과관리업",
        "수출형 제품 현지화 및 인증 지원업",
        "고객 맞춤형 장비 개조 및 교육업",
        "클라우드 기반 제조 데이터 서비스업"
    };
    private static final String[] RESEARCH_KEYWORDS = {
        "핵심공정 고도화", "시제품 실증", "제품 개선", "양산성 검증", "품질 신뢰성 향상",
        "공정 자동화", "데이터 기반 최적화", "부품 국산화", "에너지 효율 개선", "현장 적용성 검증",
        "모듈 경량화", "센서 융합", "AI 예측진단", "소재 내구성 향상", "표준공정 설계",
        "디지털 트윈 검증", "저전력 설계", "사용성 개선", "안전성 평가", "사업화 연계 실증"
    };
    private static final String[] SUPPORT_DETAILS = {
        "시제품 제작 및 성능평가 지원",
        "제품 고도화와 인증 대응 지원",
        "실증 테스트베드 활용 지원",
        "공정 자동화 전환 지원",
        "수요처 연계 사업화 지원",
        "품질 신뢰성 개선 지원",
        "기술문서 작성 및 시험분석 지원",
        "디지털 전환 컨설팅 지원",
        "지식재산 확보 및 출원 지원",
        "현장 맞춤형 장비 개선 지원",
        "친환경 공정 개선 지원",
        "데이터 기반 생산성 향상 지원",
        "시장검증 및 판로개척 지원",
        "공동연구 네트워크 연계 지원",
        "시범 생산라인 구축 지원",
        "수출 인증 사전진단 지원",
        "기술사업화 전략 수립 지원",
        "산업안전 모니터링 개선 지원",
        "설비 예지보전 모델 개발 지원",
        "부산 지역 특화산업 연계 지원"
    };
    private static final String[] NTIS_PROGRAM_NAMES = {
        "중소기업기술혁신개발",
        "창업성장기술개발",
        "해양수산산업핵심기자재국산화및표준화기술개발",
        "해양수산신산업기술사업화지원",
        "해양산업수요기술개발사업",
        "연구개발특구육성",
        "LNG벙커링핵심기술개발및체계구축",
        "지역주력산업육성",
        "소재부품기술개발",
        "스마트특성화기반구축"
    };
    private static final String[] NTIS_MINISTRIES = {
        "중소벤처기업부",
        "해양수산부",
        "산업통상자원부",
        "과학기술정보통신부",
        "농촌진흥청",
        "행정안전부"
    };
    private static final String[] NTIS_SCIENCE_CATEGORIES = {
        "해양오염방지기술",
        "달리 분류되지 않는 해양과학",
        "위험감지/모니터링 장비",
        "유공압 부품",
        "해양환경/안전설비",
        "에너지소재기술",
        "해양구조물/설비기술",
        "달리 분류되지 않는 해양안전/교통기술",
        "생체재료",
        "정보보호"
    };

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiSupportProgramAnalysisClient analysisClient;

    public DummyDataGenerationService(
            JdbcTemplate jdbcTemplate, OpenAiSupportProgramAnalysisClient analysisClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.analysisClient = analysisClient;
    }

    @Transactional
    public DummyDataGenerationResult generate(MultipartFile announcementPdf, int requestedYear) {
        validateYear(requestedYear);

        SupportProgramAnalysis analysis = analysisClient.analyze(announcementPdf, requestedYear);
        String programCode = uniqueProgramCode(analysis.code(), requestedYear);
        insertSupportProgram(programCode, requestedYear, analysis);

        int firstCompanyId = nextCompanyId();
        Random random = new Random(seed(programCode, requestedYear));
        DataImportCounter counter = new DataImportCounter();
        counter.increment("btp_support_program");

        List<Integer> companyIds = new ArrayList<>();
        for (int offset = 0; offset < COMPANY_COUNT; offset++) {
            int companyId = firstCompanyId + offset;
            companyIds.add(companyId);
            DummyCompanyProfile profile = profile(companyId, offset, requestedYear, random);
            insertCompany(profile);
            insertCompanyStatistics(profile, requestedYear, random);
            insertSupportHistory(profile, programCode, requestedYear, analysis, random);
            counter.increment("company");
            counter.increment("company_financial_statistics");
            counter.increment("company_financial_statistics");
            counter.increment("company_financial_statistics");
            counter.increment("company_employment_statistics");
            counter.increment("company_employment_statistics");
            counter.increment("company_employment_statistics");
            counter.increment("company_patent_statistics");
            counter.increment("company_patent_statistics");
            counter.increment("company_patent_statistics");
            counter.increment("company_patent");
            counter.increment("company_patent");
            counter.increment("company_ntis_lead_project");
            counter.increment("company_ntis_collaborative_project");
            counter.increment("company_business_purpose");
            counter.increment("company_business_purpose");
            counter.increment("btp_support_history");
        }

        return new DummyDataGenerationResult(requestedYear, programCode, companyIds, counter.snapshot());
    }

    private void validateYear(int requestedYear) {
        int currentYear = LocalDate.now().getYear();
        if (requestedYear < 2000 || requestedYear > currentYear) {
            throw new IllegalArgumentException("year must be between 2000 and " + currentYear + ".");
        }
    }

    private void insertSupportProgram(String programCode, int requestedYear, SupportProgramAnalysis analysis) {
        jdbcTemplate.update(
                """
                insert into btp_support_program (
                    program_year, code, budget_program_name, program_category, support_type,
                    start_date, end_date, department_name, local_government_name, program_summary
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                requestedYear,
                programCode,
                analysis.budgetProgramName(),
                analysis.programCategory(),
                analysis.supportType(),
                capDate(analysis.startDate(), requestedYear),
                capDate(analysis.endDate(), requestedYear),
                analysis.departmentName(),
                analysis.localGovernmentName(),
                analysis.programSummary());
    }

    private void insertCompany(DummyCompanyProfile profile) {
        jdbcTemplate.update(
                """
                insert into company (
                    company_id, region_name, established_date, business_entity_type, company_size,
                    listing_status, company_type, ksic_code, industry_name, industry_description,
                    main_product, is_closed, reference_date, company_status, is_innobiz,
                    is_mainbiz, is_venture_company, is_materials_company, is_net_certified,
                    is_nep_certified, researcher_count, has_research_lab, research_lab_registered_date,
                    has_rnd_department, rnd_department_registered_date, debt_ratio, cost_of_sales_ratio,
                    sales_growth_rate, employment_growth_rate, government_rnd_dependency,
                    supported_sales_growth_rate, employment_peak_index, employee_turnover_rate,
                    company_name, business_registration_number, address, industry_brief,
                    ai_summary, ai_one_line_summary
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                profile.companyId(),
                varchar20(profile.regionName()),
                profile.establishedDate(),
                varchar20("법인기업"),
                varchar20(profile.companySize()),
                varchar20(profile.companyId() % 3 == 0 ? "외감" : "일반법인"),
                varchar20("주식회사"),
                varchar20(profile.industryCode()),
                varchar1000(profile.industryName()),
                profile.industrialArea() + " 입주기업을 참고한 " + profile.industryName() + " 분야 기업 프로필입니다.",
                varchar1000(profile.mainProduct()),
                false,
                profile.referenceDate(),
                varchar20("부가가치세 일반과세자"),
                profile.rndExpenseRatio() >= 0.055,
                profile.annualSalesGrowthRate() >= 0.055 && profile.operatingMargin() >= 0.045,
                profile.rndExpenseRatio() >= 0.06 && profile.patentApplicationBase() >= 2,
                profile.industryName().contains("소재") || profile.industryName().contains("부품"),
                profile.rndExpenseRatio() >= 0.07 && profile.patentApplicationBase() >= 3,
                profile.industryName().contains("에너지") || profile.industryName().contains("환경"),
                profile.researcherCount(),
                profile.researcherCount() > 5,
                profile.establishedDate().plusYears(2),
                profile.researcherCount() > 3,
                profile.establishedDate().plusYears(1),
                profile.debtRatio(),
                profile.costOfSalesRatio(),
                profile.salesGrowthRate(),
                profile.employmentGrowthRate(),
                profile.governmentRndDependency(),
                profile.supportedSalesGrowthRate(),
                profile.employmentPeakIndex(),
                profile.employeeTurnoverRate(),
                varchar20(profile.companyName()),
                varchar20(profile.businessRegistrationNumber()),
                varchar1000(profile.address()),
                profile.industrialArea() + " 기반의 " + profile.industryName() + " 기업",
                profile.companyName()
                        + "은 "
                        + profile.industrialArea()
                        + " 권역에서 "
                        + profile.mainProduct()
                        + " 관련 제품과 서비스를 개발하는 중소기업 프로필입니다.",
                profile.mainProduct() + " 전문 기업");
    }

    private void insertCompanyStatistics(DummyCompanyProfile profile, int requestedYear, Random random) {
        int startYear = Math.max(profile.establishedDate().getYear(), requestedYear - 2);
        for (int year = startYear; year <= requestedYear; year++) {
            int age = Math.max(1, year - profile.establishedDate().getYear() + 1);
            int yearsFromBase = year - startYear;
            double annualNoise = 0.96 + random.nextDouble() * 0.08;
            int salesAmount = positiveInt(profile.baseSalesAmount()
                    * Math.pow(1.0 + profile.annualSalesGrowthRate(), yearsFromBase)
                    * annualNoise);
            int costOfSales = positiveInt(salesAmount * profile.statisticCostOfSalesRatio() * (0.97 + random.nextDouble() * 0.05));
            int operatingIncome = positiveInt(salesAmount * profile.operatingMargin() * (0.88 + random.nextDouble() * 0.24));
            int rndExpense = positiveInt(salesAmount * profile.rndExpenseRatio() * (0.9 + random.nextDouble() * 0.2));
            int totalAssets = positiveInt(salesAmount * (0.75 + random.nextDouble() * 0.65));
            int totalLiabilities = positiveInt(totalAssets * profile.debtRatio() / (100.0 + profile.debtRatio()));
            int employeeCount = Math.max(3, positiveInt(profile.baseEmployeeCount()
                    * Math.pow(1.0 + profile.employmentGrowthRate() / 100.0, yearsFromBase)
                    * (0.97 + random.nextDouble() * 0.06)));
            int patentApplications = Math.max(0, profile.patentApplicationBase() + random.nextInt(3) - 1);
            int registeredPatents = Math.max(0, patentApplications - random.nextInt(2));

            jdbcTemplate.update(
                    """
                    insert into company_financial_statistics (
                        company_id, year, sales_amount, operating_income, cost_of_sales, net_income,
                        operating_margin, total_assets, total_liabilities, total_equity,
                        paid_in_capital, research_and_development_expense
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (company_id, year) do nothing
                    """,
                    profile.companyId(),
                    year,
                    salesAmount,
                    operatingIncome,
                    costOfSales,
                    positiveInt(operatingIncome * (0.72 + random.nextDouble() * 0.18)),
                    operatingIncome / (double) salesAmount,
                    totalAssets,
                    totalLiabilities,
                    totalAssets - totalLiabilities,
                    50_000 + random.nextInt(300_000),
                    rndExpense);
            jdbcTemplate.update(
                    """
                    insert into company_employment_statistics (
                        company_id, year, employee_count, pension_subscriber_count,
                        pension_new_hire_count, pension_retiree_count, average_salary
                    ) values (?, ?, ?, ?, ?, ?, ?)
                    on conflict (company_id, year) do nothing
                    """,
                    profile.companyId(),
                    year,
                    employeeCount,
                    Math.max(0, employeeCount - random.nextInt(3)),
                    Math.max(1, positiveInt(employeeCount * (0.08 + random.nextDouble() * 0.08))),
                    Math.max(0, positiveInt(employeeCount * (0.04 + random.nextDouble() * 0.08))),
                    positiveInt(profile.averageSalary() * (0.96 + random.nextDouble() * 0.08)));
            jdbcTemplate.update(
                    """
                    insert into company_patent_statistics (
                        company_id, year, registered_patent_count, patent_application_count
                    ) values (?, ?, ?, ?)
                    on conflict (company_id, year) do nothing
                    """,
                    profile.companyId(),
                    year,
                    registeredPatents,
                    patentApplications);
        }

        insertPatent(profile, requestedYear, random, 1);
        insertPatent(profile, requestedYear, random, 2);
        insertNtisLeadProject(profile, requestedYear, random);
        insertNtisCollaborativeProject(profile, requestedYear, random);
        insertBusinessPurpose(profile, requestedYear, 1, profile.mainProduct() + " 제조 및 판매업");
        insertBusinessPurpose(profile, requestedYear, 2, secondaryBusinessPurpose(profile));
    }

    private void insertPatent(DummyCompanyProfile profile, int requestedYear, Random random, int sequence) {
        LocalDate applicationDate = randomDate(requestedYear - random.nextInt(3), random);
        LocalDate registrationDate = applicationDate.plusMonths(8 + random.nextInt(16));
        if (registrationDate.getYear() > requestedYear) {
            registrationDate = LocalDate.of(requestedYear, Month.DECEMBER, 15);
        }
        String sourceHash = ExcelImportSupport.hash("dummy-patent", profile.companyId(), sequence);
        jdbcTemplate.update(
                """
                insert into company_patent (
                    company_id, patent_type, registration_status, application_date,
                    registration_date, company_relation_code, is_active, source_hash
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (source_hash) do nothing
                """,
                profile.companyId(),
                varchar20(patentType(profile, sequence)),
                varchar20(sequence % 3 == 0 ? "공개" : "등록"),
                applicationDate,
                registrationDate,
                varchar20(sequence % 7 == 0 ? "대표이사" : "본인"),
                sequence % 3 != 0,
                sourceHash);
    }

    private void insertNtisLeadProject(DummyCompanyProfile profile, int requestedYear, Random random) {
        int referenceYear = Math.max(profile.establishedDate().getYear(), requestedYear - random.nextInt(3));
        int governmentFund = ntisGovernmentFund(profile, random);
        int privateFund = positiveInt(governmentFund * (0.25 + random.nextDouble() * 0.55));
        String sourceHash = ExcelImportSupport.hash("dummy-ntis-lead", profile.companyId(), referenceYear);
        jdbcTemplate.update(
                """
                insert into company_ntis_lead_project (
                    company_id, reference_year, reference_date, project_name, supervising_ministry_name,
                    region_name, total_research_start_date, total_research_end_date,
                    annual_research_start_date, annual_research_end_date, science_technology_category_name,
                    government_research_fund, private_research_fund, total_research_fund, source_hash
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (source_hash) do nothing
                """,
                profile.companyId(),
                referenceYear,
                LocalDate.of(referenceYear, Month.DECEMBER, 31),
                varchar1000(ntisProgramName(profile)),
                varchar20(ntisMinistry(profile)),
                varchar1000(busanRegionName(profile)),
                LocalDate.of(referenceYear, Month.MARCH, 1),
                LocalDate.of(requestedYear, Month.NOVEMBER, 30),
                LocalDate.of(referenceYear, Month.MARCH, 1),
                LocalDate.of(referenceYear, Month.NOVEMBER, 30),
                varchar1000(scienceTechnologyCategory(profile)),
                governmentFund,
                privateFund,
                governmentFund + privateFund,
                sourceHash);
    }

    private void insertNtisCollaborativeProject(DummyCompanyProfile profile, int requestedYear, Random random) {
        int referenceYear = Math.max(profile.establishedDate().getYear(), requestedYear - random.nextInt(3));
        String sourceHash = ExcelImportSupport.hash("dummy-ntis-collab", profile.companyId(), referenceYear);
        jdbcTemplate.update(
                """
                insert into company_ntis_collaborative_project (
                    company_id, reference_year, reference_date, has_foreign_institute_collaboration,
                    has_other_collaboration, research_type_name, collaboration_participation_type_name,
                    collaboration_country_name, research_performer_type_name, commissioned_research_fund,
                    collaborative_research_expense, collaborative_research_income,
                    has_company_collaboration, has_university_collaboration,
                    has_public_institute_collaboration, source_hash
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (source_hash) do nothing
                """,
                profile.companyId(),
                referenceYear,
                LocalDate.of(referenceYear, Month.DECEMBER, 31),
                false,
                false,
                varchar20(profile.companyId() % 4 == 0 ? "위탁과제" : "공동연구(국내)"),
                varchar20(profile.companyId() % 4 == 0 ? "기타" : "연구 기술개발"),
                varchar20("대한민국"),
                varchar20("중소기업"),
                profile.companyId() % 4 == 0 ? positiveInt(ntisGovernmentFund(profile, random) * 0.25) : 0,
                positiveInt(ntisGovernmentFund(profile, random) * (0.08 + random.nextDouble() * 0.25)),
                positiveInt(ntisGovernmentFund(profile, random) * (0.02 + random.nextDouble() * 0.08)),
                true,
                profile.rndExpenseRatio() >= 0.055,
                profile.patentApplicationBase() >= 3,
                sourceHash);
    }

    private void insertBusinessPurpose(DummyCompanyProfile profile, int requestedYear, int displayOrder, String purpose) {
        jdbcTemplate.update(
                """
                insert into company_business_purpose (
                    company_id, display_order, business_purpose, registered_date
                ) values (?, ?, ?, ?)
                on conflict (company_id, display_order) do nothing
                """,
                profile.companyId(),
                displayOrder,
                varchar1000("1. " + purpose),
                LocalDate.of(Math.max(profile.establishedDate().getYear(), requestedYear - 2), Month.JANUARY, 15));
    }

    private void insertSupportHistory(
            DummyCompanyProfile profile,
            String programCode,
            int requestedYear,
            SupportProgramAnalysis analysis,
            Random random) {
        LocalDate selectedDate = capDate(analysis.startDate().minusDays(15 + random.nextInt(20)), requestedYear);
        LocalDate startDate = capDate(analysis.startDate(), requestedYear);
        LocalDate endDate = capDate(analysis.endDate(), requestedYear);
        String sourceHash = ExcelImportSupport.hash("dummy-support", programCode, profile.companyId(), requestedYear);
        jdbcTemplate.update(
                """
                insert into btp_support_history (
                    support_year, code, budget_program_name, support_type, support_category,
                    support_detail, support_item, selected_date, selection_result,
                    support_amount, start_date, end_date, company_id, industry_code,
                    province_name, district_name, main_product, established_year, source_hash
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (source_hash) do nothing
                """,
                requestedYear,
                varchar20(programCode),
                varchar1000(analysis.budgetProgramName()),
                varchar20(analysis.supportType()),
                varchar20(analysis.programCategory()),
                varchar1000(supportDetail(profile)),
                varchar1000(profile.industrialArea() + " " + profile.mainProduct() + " 사업화 지원"),
                selectedDate,
                varchar20("지원대상"),
                supportAmount(profile, random),
                startDate,
                endDate,
                profile.companyId(),
                varchar20(profile.industryCode()),
                varchar20("부산광역시"),
                varchar20(profile.regionName()),
                varchar1000(profile.mainProduct()),
                profile.establishedDate().getYear(),
                sourceHash);
    }

    private DummyCompanyProfile profile(int companyId, int offset, int requestedYear, Random random) {
        int sectorIndex = random.nextInt(INDUSTRY_SECTORS.length);
        int specialtyIndex = random.nextInt(INDUSTRY_SPECIALTIES.length);
        int areaIndex = Math.floorMod(sectorIndex * 3 + specialtyIndex + offset, INDUSTRIAL_AREAS.length);
        int roadIndex = roadIndex(offset, areaIndex);
        int establishedYear = requestedYear - 3 - random.nextInt(12);
        LocalDate establishedDate = LocalDate.of(establishedYear, 1 + random.nextInt(12), 1 + random.nextInt(20));
        boolean mediumCompany = offset % 3 == 0;
        double technologyIntensity = technologyIntensity(sectorIndex, specialtyIndex);
        int baseSalesAmount = baseSalesAmount(mediumCompany, technologyIntensity, random);
        double annualSalesGrowthRate = annualSalesGrowthRate(technologyIntensity, random);
        double operatingMargin = operatingMargin(technologyIntensity, random);
        double costOfSalesRatio = costOfSalesRatio(sectorIndex, specialtyIndex, random);
        double debtRatio = debtRatio(sectorIndex, mediumCompany, random);
        int baseEmployeeCount = baseEmployeeCount(baseSalesAmount, mediumCompany, technologyIntensity, random);
        double rndExpenseRatio = rndExpenseRatio(technologyIntensity, random);
        int patentApplicationBase = patentApplicationBase(technologyIntensity, mediumCompany, random);
        double employmentGrowthRate = -1.0 + annualSalesGrowthRate * 85.0 + random.nextDouble() * 3.5;
        return new DummyCompanyProfile(
                companyId,
                ROAD_REGIONS[roadIndex],
                establishedDate,
                companySize(offset, mediumCompany),
                industryCode(sectorIndex, specialtyIndex),
                industryName(sectorIndex, specialtyIndex),
                productName(sectorIndex, specialtyIndex),
                LocalDate.of(requestedYear, Month.DECEMBER, 31),
                companyName(offset, companyId, sectorIndex, specialtyIndex),
                businessRegistrationNumber(companyId),
                address(companyId, roadIndex, areaIndex),
                INDUSTRIAL_AREAS[areaIndex],
                Math.max(2, positiveInt(baseEmployeeCount * (0.08 + technologyIntensity * 0.18))),
                debtRatio,
                costOfSalesRatio * 100.0,
                annualSalesGrowthRate * 100.0,
                employmentGrowthRate,
                4.0 + technologyIntensity * 42.0 + random.nextDouble() * 10.0,
                Math.max(0.0, annualSalesGrowthRate * 100.0 + 2.0 + random.nextDouble() * 8.0),
                78.0 + technologyIntensity * 22.0 + random.nextDouble() * 14.0,
                Math.max(1.5, 11.0 - technologyIntensity * 4.0 + random.nextDouble() * 4.0),
                baseSalesAmount,
                annualSalesGrowthRate,
                operatingMargin,
                costOfSalesRatio,
                baseEmployeeCount,
                34_000 + positiveInt(technologyIntensity * 18_000) + random.nextInt(12_000),
                rndExpenseRatio,
                patentApplicationBase);
    }

    private int nextCompanyId() {
        Integer maxCompanyId = jdbcTemplate.queryForObject("select coalesce(max(company_id), 0) from company", Integer.class);
        return (maxCompanyId == null ? 0 : maxCompanyId) + 1;
    }

    private String uniqueProgramCode(String requestedCode, int requestedYear) {
        String base = requestedCode == null || requestedCode.isBlank() ? "DUMMY" + requestedYear : requestedCode;
        base = base.replaceAll("[^A-Za-z0-9_-]", "");
        if (base.isBlank()) {
            base = "DUMMY" + requestedYear;
        }
        base = base.length() > 14 ? base.substring(0, 14) : base;
        String candidate = base;
        int sequence = 1;
        while (programExists(candidate, requestedYear)) {
            String suffix = "_" + sequence++;
            candidate = base.substring(0, Math.min(base.length(), 20 - suffix.length())) + suffix;
        }
        return candidate;
    }

    private boolean programExists(String code, int requestedYear) {
        Integer count =
                jdbcTemplate.queryForObject(
                        "select count(*) from btp_support_program where program_year = ? and code = ?",
                        Integer.class,
                        requestedYear,
                        code);
        return count != null && count > 0;
    }

    private LocalDate randomDate(int year, Random random) {
        int safeYear = Math.max(2000, year);
        return LocalDate.of(safeYear, 1 + random.nextInt(12), 1 + random.nextInt(25));
    }

    private LocalDate capDate(LocalDate date, int requestedYear) {
        if (date == null) {
            return LocalDate.of(requestedYear, Month.DECEMBER, 31);
        }
        if (date.getYear() > requestedYear) {
            return LocalDate.of(requestedYear, date.getMonth(), Math.min(date.getDayOfMonth(), date.lengthOfMonth()));
        }
        return date;
    }

    private long seed(String programCode, int requestedYear) {
        return (((long) requestedYear) << 32) ^ programCode.hashCode();
    }

    private String industryCode(int sectorIndex, int specialtyIndex) {
        int suffix = 100 + sectorIndex * INDUSTRY_SPECIALTIES.length + specialtyIndex;
        return INDUSTRY_CODE_PREFIXES[sectorIndex] + suffix;
    }

    private String industryName(int sectorIndex, int specialtyIndex) {
        return INDUSTRY_SECTORS[sectorIndex] + " " + INDUSTRY_SPECIALTIES[specialtyIndex];
    }

    private String productName(int sectorIndex, int specialtyIndex) {
        String sector = INDUSTRY_SECTORS[sectorIndex];
        return switch (specialtyIndex) {
            case 0 -> sector + " 양산품";
            case 1 -> sector + " 핵심부품";
            case 2 -> sector + " 전용장비";
            case 3 -> sector + " 통합시스템";
            case 4 -> sector + " 시험솔루션";
            case 5 -> sector + " 자동화모듈";
            case 6 -> sector + " 기능소재";
            case 7 -> sector + " 운영플랫폼";
            case 8 -> sector + " 연구시제품";
            default -> sector + " 유지보수서비스";
        };
    }

    private double technologyIntensity(int sectorIndex, int specialtyIndex) {
        double base =
                switch (INDUSTRY_SECTORS[sectorIndex]) {
                    case "소프트웨어", "로봇", "바이오소재", "이차전지", "반도체장비", "정보보안", "AI데이터", "디지털헬스케어" -> 0.8;
                    case "전자부품", "의료기기", "정밀기계", "조선해양기자재", "친환경에너지", "고분자소재", "환경설비" -> 0.62;
                    case "산업장비", "자동차부품", "스마트물류", "금속가공" -> 0.48;
                    default -> 0.34;
                };
        if (specialtyIndex == 8 || specialtyIndex == 4) {
            base += 0.1;
        }
        if (specialtyIndex == 9) {
            base -= 0.08;
        }
        return Math.max(0.25, Math.min(0.95, base));
    }

    private int baseSalesAmount(boolean mediumCompany, double technologyIntensity, Random random) {
        int base = mediumCompany ? 2_400_000 + random.nextInt(5_600_000) : 650_000 + random.nextInt(2_200_000);
        return positiveInt(base * (0.9 + technologyIntensity * 0.35));
    }

    private double annualSalesGrowthRate(double technologyIntensity, Random random) {
        return -0.015 + technologyIntensity * 0.085 + random.nextDouble() * 0.055;
    }

    private double operatingMargin(double technologyIntensity, Random random) {
        return 0.025 + technologyIntensity * 0.045 + random.nextDouble() * 0.025;
    }

    private double costOfSalesRatio(int sectorIndex, int specialtyIndex, Random random) {
        boolean serviceLike = specialtyIndex == 7 || specialtyIndex == 8 || specialtyIndex == 9
                || "소프트웨어".equals(INDUSTRY_SECTORS[sectorIndex])
                || "AI데이터".equals(INDUSTRY_SECTORS[sectorIndex])
                || "정보보안".equals(INDUSTRY_SECTORS[sectorIndex]);
        if (serviceLike) {
            return 0.38 + random.nextDouble() * 0.18;
        }
        return 0.54 + random.nextDouble() * 0.18;
    }

    private double debtRatio(int sectorIndex, boolean mediumCompany, Random random) {
        double base = mediumCompany ? 95.0 : 125.0;
        if ("소프트웨어".equals(INDUSTRY_SECTORS[sectorIndex]) || "AI데이터".equals(INDUSTRY_SECTORS[sectorIndex])) {
            base -= 25.0;
        }
        return Math.max(45.0, base + random.nextDouble() * 75.0);
    }

    private int baseEmployeeCount(
            int baseSalesAmount, boolean mediumCompany, double technologyIntensity, Random random) {
        double salesPerEmployee = mediumCompany ? 95_000.0 : 72_000.0;
        salesPerEmployee += technologyIntensity * 25_000.0;
        return Math.max(5, positiveInt(baseSalesAmount / salesPerEmployee) + random.nextInt(9));
    }

    private double rndExpenseRatio(double technologyIntensity, Random random) {
        return 0.008 + technologyIntensity * 0.07 + random.nextDouble() * 0.025;
    }

    private int patentApplicationBase(double technologyIntensity, boolean mediumCompany, Random random) {
        int base = mediumCompany ? 1 : 0;
        return base + positiveInt(technologyIntensity * 3.0) + random.nextInt(2);
    }

    private String researchKeyword(DummyCompanyProfile profile) {
        int index = Math.floorMod(
                profile.industryName().hashCode()
                        + profile.mainProduct().hashCode()
                        + positiveInt(profile.rndExpenseRatio() * 1000),
                RESEARCH_KEYWORDS.length);
        return RESEARCH_KEYWORDS[index];
    }

    private String scienceTechnologyCategory(DummyCompanyProfile profile) {
        int index = Math.floorMod(profile.industryName().hashCode() + profile.mainProduct().hashCode(), NTIS_SCIENCE_CATEGORIES.length);
        return NTIS_SCIENCE_CATEGORIES[index];
    }

    private String ntisProgramName(DummyCompanyProfile profile) {
        int index = Math.floorMod(profile.companyId() * 7 + profile.industryName().hashCode(), NTIS_PROGRAM_NAMES.length);
        return NTIS_PROGRAM_NAMES[index];
    }

    private String ntisMinistry(DummyCompanyProfile profile) {
        if (profile.industryName().contains("조선") || profile.industryName().contains("해양")) {
            return "해양수산부";
        }
        if (profile.industryName().contains("소프트웨어")
                || profile.industryName().contains("AI")
                || profile.industryName().contains("정보보안")) {
            return "과학기술정보통신부";
        }
        if (profile.industryName().contains("에너지") || profile.industryName().contains("이차전지")) {
            return "산업통상자원부";
        }
        int index = Math.floorMod(profile.companyId() + profile.mainProduct().hashCode(), NTIS_MINISTRIES.length);
        return NTIS_MINISTRIES[index];
    }

    private String busanRegionName(DummyCompanyProfile profile) {
        return "부산광역시 " + profile.regionName();
    }

    private int ntisGovernmentFund(DummyCompanyProfile profile, Random random) {
        int[] commonFunds = {4_000_000, 85_500_000, 160_000_000, 200_000_000, 225_000_000, 262_500_000, 400_000_000};
        int base = commonFunds[Math.floorMod(profile.companyId() + random.nextInt(commonFunds.length), commonFunds.length)];
        double scale = profile.rndExpenseRatio() >= 0.07 ? 1.0 : 0.55 + random.nextDouble() * 0.35;
        return positiveInt(base * scale);
    }

    private String patentType(DummyCompanyProfile profile, int sequence) {
        int index = Math.floorMod(profile.companyId() + sequence, 20);
        if (index == 0) {
            return "실용신안권";
        }
        if (index <= 3) {
            return "디자인권";
        }
        if (index <= 7) {
            return "상표권";
        }
        return "특허권";
    }

    private String companySize(int offset, boolean mediumCompany) {
        if (offset % 8 == 0) {
            return "소상공인";
        }
        return mediumCompany ? "중기업" : "소기업";
    }

    private String legacyScienceTechnologyCategory(DummyCompanyProfile profile) {
        if (profile.industryName().contains("바이오") || profile.industryName().contains("헬스케어")) {
            return "생명보건의료";
        }
        if (profile.industryName().contains("소프트웨어")
                || profile.industryName().contains("AI")
                || profile.industryName().contains("정보보안")) {
            return "정보통신";
        }
        if (profile.industryName().contains("환경") || profile.industryName().contains("에너지")) {
            return "에너지환경";
        }
        return "기계소재";
    }

    private int supportAmount(DummyCompanyProfile profile, Random random) {
        int[] commonAmounts = {3_850, 4_730, 10_377, 13_200, 13_400, 20_000};
        if (random.nextDouble() < 0.72) {
            return commonAmounts[Math.floorMod(profile.companyId() + random.nextInt(commonAmounts.length), commonAmounts.length)];
        }
        int amount = positiveInt(profile.baseSalesAmount() * (0.002 + profile.rndExpenseRatio() * 0.035));
        return Math.max(3_000, Math.min(30_000, amount));
    }

    private String secondaryBusinessPurpose(DummyCompanyProfile profile) {
        int index = Math.floorMod(
                profile.companyId() * 13 + profile.industryName().hashCode(),
                SECONDARY_BUSINESS_PURPOSES.length);
        return SECONDARY_BUSINESS_PURPOSES[index];
    }

    private String supportDetail(DummyCompanyProfile profile) {
        int index = Math.floorMod(
                profile.companyId() * 17 + profile.mainProduct().hashCode(),
                SUPPORT_DETAILS.length);
        return SUPPORT_DETAILS[index];
    }

    private int positiveInt(double value) {
        return Math.max(0, (int) Math.round(value));
    }

    private String varchar20(String value) {
        return limited(value, 20);
    }

    private String varchar1000(String value) {
        return limited(value, 1000);
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String companyName(int offset, int companyId, int sectorIndex, int specialtyIndex) {
        String prefix = COMPANY_PREFIXES[Math.floorMod(companyId * 7 + offset + sectorIndex, COMPANY_PREFIXES.length)];
        String middle =
                COMPANY_MIDDLES[
                        Math.floorMod(companyId * 5 + offset * 7 + sectorIndex * 3 + specialtyIndex, COMPANY_MIDDLES.length)];
        String suffix = COMPANY_SUFFIXES[Math.floorMod(companyId * 11 + offset * 3 + specialtyIndex, COMPANY_SUFFIXES.length)];
        String companyName = prefix + middle + suffix;
        if (companyName.length() <= 20) {
            return companyName;
        }
        return companyName.substring(0, 20);
    }

    private String businessRegistrationNumber(int companyId) {
        int middle = Math.floorMod(companyId * 37, 100);
        int serial = 10_000 + Math.floorMod(companyId * 7919, 90_000);
        return "6" + String.format("%02d", Math.floorMod(companyId, 100))
                + "-"
                + String.format("%02d", middle)
                + "-"
                + String.format("%05d", serial);
    }

    private int roadIndex(int offset, int areaIndex) {
        return Math.floorMod(areaIndex + offset * 3, ROAD_ADDRESSES.length);
    }

    private String address(int companyId, int roadIndex, int areaIndex) {
        String roadAddress = ROAD_ADDRESSES[roadIndex];
        int mainNumber = 10 + Math.floorMod(companyId * 7 + areaIndex * 11, 240);
        int floor = 2 + Math.floorMod(companyId, 7);
        int unit = 100 + Math.floorMod(companyId * 13, 80);
        return roadAddress + " " + mainNumber + ", " + floor + "층 " + unit + "호";
    }

    private record DummyCompanyProfile(
            int companyId,
            String regionName,
            LocalDate establishedDate,
            String companySize,
            String industryCode,
            String industryName,
            String mainProduct,
            LocalDate referenceDate,
            String companyName,
            String businessRegistrationNumber,
            String address,
            String industrialArea,
            int researcherCount,
            double debtRatio,
            double costOfSalesRatio,
            double salesGrowthRate,
            double employmentGrowthRate,
            double governmentRndDependency,
            double supportedSalesGrowthRate,
            double employmentPeakIndex,
            double employeeTurnoverRate,
            int baseSalesAmount,
            double annualSalesGrowthRate,
            double operatingMargin,
            double statisticCostOfSalesRatio,
            int baseEmployeeCount,
            int averageSalary,
            double rndExpenseRatio,
            int patentApplicationBase) {}
}
