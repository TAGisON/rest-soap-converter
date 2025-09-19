package com.example.restsoapconverter.repository;

import com.example.restsoapconverter.entity.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MappingRepository extends JpaRepository<Mapping, Long> {

    @Query("SELECT m FROM Mapping m WHERE m.endpoint.id = :endpointId ORDER BY m.executionOrder")
    List<Mapping> findByEndpointIdOrderByExecutionOrder(@Param("endpointId") Long endpointId);

    void deleteByEndpointId(Long endpointId);
}
