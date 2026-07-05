package com.test.orderProcessingSystem.repository;


import com.test.orderProcessingSystem.entity.Dual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface HealthCheckRepository extends JpaRepository<Dual, String> {

    @Query(	value = "SELECT * from dual", nativeQuery = true)
    List<Object[]> dbConnectionCheck();
}
