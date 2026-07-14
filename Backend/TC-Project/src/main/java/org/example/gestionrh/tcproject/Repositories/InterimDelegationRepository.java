package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.InterimDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InterimDelegationRepository extends JpaRepository<InterimDelegation, Long> {
    
    @Query("SELECT i FROM InterimDelegation i WHERE i.delegator.id = :chefId AND i.isActive = true AND i.startDate <= :currentDate AND i.endDate >= :currentDate")
    List<InterimDelegation> findActiveDelegationsForChef(@Param("chefId") Long chefId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT i FROM InterimDelegation i WHERE i.interim.id = :interimId AND i.isActive = true AND i.startDate <= :currentDate AND i.endDate >= :currentDate")
    List<InterimDelegation> findActiveDelegationsForInterim(@Param("interimId") Long interimId, @Param("currentDate") LocalDate currentDate);
}
