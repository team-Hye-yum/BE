-- Deactivate short over-broad keywords that create false positives through
-- Korean substring matching in support-program text.
update public.btp_connection_keyword_rule
set active = false
where active = true
  and keyword in (
      '기능',
      '자동',
      '제조기'
  );
