package com.example.restsoapconverter.repository;

import com.example.restsoapconverter.entity.ErrorMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ErrorMappingRepository extends JpaRepository<ErrorMapping, Long> {

    List<ErrorMapping> findByEndpointId(Long endpointId);
    void deleteByEndpointId(Long endpointId);
}
