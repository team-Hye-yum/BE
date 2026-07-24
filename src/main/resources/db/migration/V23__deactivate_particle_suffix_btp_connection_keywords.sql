-- Deactivate keyword fragments that still include Korean object/genitive particles
-- or sentence-level predicates. These fragments are too broad for objective
-- industry-equipment matching.
update public.btp_connection_keyword_rule
set active = false
where active = true
  and (
      keyword in (
          '1차',
          '관리서비스',
          '과정에서의',
          '내의',
          '다른',
          '상태의',
          '수행할',
          '이를'
      )
      or keyword ~ '(을|를|의|만을|에서의|와의)$'
  );
