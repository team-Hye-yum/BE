# Company Metrics AI Prompts

`GET /api/admin/imports/company-metrics` 호출 시 아래 템플릿이 각 컬럼 생성에 사용됩니다.

- `ai_summary.txt` -> `company.ai_summary`
- `ai_one_line_summary.txt` -> `company.ai_one_line_summary`

운영 중 파일을 직접 수정하려면 `OPENAI_COMPANY_METRICS_PROMPT_DIRECTORY`에 외부 디렉터리 경로를 지정하고, 같은 파일명을 둡니다. 외부 파일이 없으면 jar에 포함된 기본 템플릿을 사용합니다.

사용 가능한 변수:

```text
{{companyId}}
{{companyName}}
{{regionName}}
{{establishedDate}}
{{businessEntityType}}
{{companySize}}
{{listingStatus}}
{{companyType}}
{{ksicCode}}
{{industryName}}
{{industryDescription}}
{{mainProduct}}
{{isClosed}}
{{companyStatus}}
{{isInnobiz}}
{{isMainbiz}}
{{isVentureCompany}}
{{isMaterialsCompany}}
{{isNetCertified}}
{{isNepCertified}}
{{researcherCount}}
{{hasResearchLab}}
{{hasRndDepartment}}
{{debtRatio}}
{{costOfSalesRatio}}
{{salesGrowthRate}}
{{employmentGrowthRate}}
{{governmentRndDependency}}
{{supportedSalesGrowthRate}}
{{employmentPeakIndex}}
{{employeeTurnoverRate}}
```
