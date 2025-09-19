package com.example.restsoapconverter.repository;

import com.example.restsoapconverter.entity.SoapEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoapEndpointRepository extends JpaRepository<SoapEndpoint, Long> {

    @Query("SELECT s FROM SoapEndpoint s WHERE s.operationName = :operationName AND s.namespace = :namespace AND s.enabled = true")
    Optional<SoapEndpoint> findByOperationNameAndNamespace(@Param("operationName") String operationName, 
                                                           @Param("namespace") String namespace);

    List<SoapEndpoint> findByEnabledTrue();
    Optional<SoapEndpoint> findByName(String name);
    boolean existsByName(String name);
}
