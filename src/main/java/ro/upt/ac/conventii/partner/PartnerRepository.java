package ro.upt.ac.conventii.partner;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ro.upt.ac.conventii.companie.Companie;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Integer> {
    Partner findById(int id);
    Optional<Partner> findByEmail(String email);
    List<Partner> findByCompanie(Companie companie);
    List<Partner> findByCompanieId(int companieId);
    
}