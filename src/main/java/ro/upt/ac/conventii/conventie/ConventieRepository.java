package ro.upt.ac.conventii.conventie;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ro.upt.ac.conventii.cadruDidactic.CadruDidactic;
import ro.upt.ac.conventii.companie.Companie;
import ro.upt.ac.conventii.student.Student;
import ro.upt.ac.conventii.tutore.Tutore;

@Repository
public interface ConventieRepository extends JpaRepository<Conventie, Integer> {
    
    // Metode pentru căutare directă după ID
    Conventie findById(int id);
    
    // Metode pentru căutare după status
    List<Conventie> findByStatus(ConventieStatus status);
    List<Conventie> findByStatusInAndCompanieId(List<ConventieStatus> statuses, int companieId);
    List<Conventie> findByStatusAndCompanieId(ConventieStatus status, int companieId);
    
    // Metode pentru căutare după entități
    List<Conventie> findByStudent(Student student);
    List<Conventie> findByCadruDidactic(CadruDidactic cadruDidactic);
    List<Conventie> findByCompanie(Companie companie);
    List<Conventie> findByCompanieId(int companieId);
    
    // ======== FOARTE IMPORTANT: Metode pentru tutore ========
    // Metode cu interogări JPQL pentru a evita problemele de tip
    @Query("SELECT c FROM Conventie c WHERE c.tutore.id = :tutoreId")
    List<Conventie> findByTutoreId(@Param("tutoreId") int tutoreId);
    
    @Query("SELECT COUNT(c) FROM Conventie c WHERE c.tutore.id = :tutoreId")
    long countByTutoreId(@Param("tutoreId") int tutoreId);
    
    // Metode pentru căutare după entitatea Tutore
    List<Conventie> findByTutore(Tutore tutore);
    
    // Metode pentru căutare cu companie și status
    List<Conventie> findByCompanieAndStatus(Companie companie, ConventieStatus status);
    List<Conventie> findByCompanieIdAndStatus(int companieId, ConventieStatus status);
    
    // Metode pentru căutare după email student
    @Query("SELECT c FROM Conventie c WHERE c.student.email = :email")
    List<Conventie> findByStudentEmail(@Param("email") String email);
    
    @Query("SELECT c FROM Conventie c WHERE c.student.email = :email ORDER BY c.dataIntocmirii DESC")
    List<Conventie> findByStudentEmailOrderByDataIntocmiriiDesc(@Param("email") String email);
    
    @Query("SELECT c FROM Conventie c WHERE c.student.email = :email ORDER BY c.dataIntocmirii DESC")
    List<Conventie> findTop3ByStudentEmailOrderByDataIntocmiriiDesc(@Param("email") String email, Pageable pageable);
    
    // Metode pentru căutare cu paginare
    @Query("SELECT c FROM Conventie c WHERE c.status = :status ORDER BY c.dataIntocmirii DESC")
    List<Conventie> findTop3ByStatusOrderByDataIntocmiriiDesc(@Param("status") ConventieStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Conventie c WHERE c.status = :status ORDER BY c.dataIntocmirii DESC")
    List<Conventie> findTop5ByStatusOrderByDataIntocmiriiDesc(@Param("status") ConventieStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Conventie c WHERE c.status = :status AND c.companie.id = :companieId ORDER BY c.dataIntocmirii DESC")
    List<Conventie> findTop5ByStatusAndCompanieIdOrderByDataIntocmiriiDesc(
        @Param("status") ConventieStatus status, 
        @Param("companieId") int companieId, 
        Pageable pageable);

    
    @Query("SELECT c FROM Conventie c WHERE c.companie = :companie AND c.status = :status ORDER BY c.dataIntocmirii DESC")
    List<Conventie> findTop5ByCompanieAndStatusOrderByDataIntocmiriiDesc(
        @Param("companie") Companie companie, 
        @Param("status") ConventieStatus status, 
        Pageable pageable);
}