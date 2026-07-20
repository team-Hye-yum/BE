package site.dataon.hyeyum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.Company;

public interface CompanyRepository extends JpaRepository<Company, Integer> {}
