package com.example.restsoapconverter.repository;

import com.example.restsoapconverter.entity.RestCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestCallRepository extends JpaRepository<RestCall, Long> {

    @Query("SELECT r FROM RestCall r WHERE r.endpoint.id = :endpointId ORDER BY r.sequenceOrder")
    List<RestCall> findByEndpointIdOrderBySequenceOrder(@Param("endpointId") Long endpointId);

    void deleteByEndpointId(Long endpointId);
}
