create index if not exists idx_company_ksic_code
    on public.company (ksic_code);

create index if not exists idx_btp_support_history_industry_code
    on public.btp_support_history (industry_code);

create index if not exists idx_company_ntis_lead_project_company_id
    on public.company_ntis_lead_project (company_id);

create index if not exists idx_btp_connection_keyword_rule_active_reviewed
    on public.btp_connection_keyword_rule (active, reviewed);

create index if not exists idx_btp_equipment_category
    on public.btp_equipment (category_large, category_middle);
