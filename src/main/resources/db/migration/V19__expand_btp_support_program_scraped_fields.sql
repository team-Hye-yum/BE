alter table public.btp_support_program
    alter column announcement_url type character varying(1000),
    alter column program_category type character varying(200),
    alter column support_type type character varying(200),
    alter column program_summary type text;
