package com.example.restsoapconverter.repository;

import com.example.restsoapconverter.entity.CachePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CachePolicyRepository extends JpaRepository<CachePolicy, Long> {

    Optional<CachePolicy> findByEndpointId(Long endpointId);
    void deleteByEndpointId(Long endpointId);
}
