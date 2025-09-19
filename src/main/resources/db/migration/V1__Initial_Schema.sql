-- Initial schema for REST-SOAP Converter

-- Create soap_endpoints table
CREATE TABLE IF NOT EXISTS soap_endpoints (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    namespace VARCHAR(500) NOT NULL,
    wsdl_path VARCHAR(500),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create rest_calls table
CREATE TABLE IF NOT EXISTS rest_calls (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES soap_endpoints(id) ON DELETE CASCADE,
    sequence_order INTEGER NOT NULL DEFAULT 1,
    method VARCHAR(10) NOT NULL DEFAULT 'GET',
    url_template TEXT NOT NULL,
    auth_type VARCHAR(50) DEFAULT 'NONE',
    timeout_ms INTEGER DEFAULT 30000,
    parallel_execution BOOLEAN DEFAULT false
);

-- Create mappings table
CREATE TABLE IF NOT EXISTS mappings (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES soap_endpoints(id) ON DELETE CASCADE,
    mapping_type VARCHAR(20) NOT NULL,
    mapping_definition TEXT,
    mapper_class VARCHAR(255),
    execution_order INTEGER DEFAULT 1
);

-- Create error_mappings table
CREATE TABLE IF NOT EXISTS error_mappings (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES soap_endpoints(id) ON DELETE CASCADE,
    rest_status_code INTEGER,
    soap_fault_code VARCHAR(100) NOT NULL,
    soap_fault_message VARCHAR(500) NOT NULL
);

-- Create cache_policies table
CREATE TABLE IF NOT EXISTS cache_policies (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES soap_endpoints(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT false,
    ttl_seconds INTEGER DEFAULT 300
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_soap_endpoints_operation_namespace 
    ON soap_endpoints(operation_name, namespace);

CREATE INDEX IF NOT EXISTS idx_soap_endpoints_enabled 
    ON soap_endpoints(enabled);

CREATE INDEX IF NOT EXISTS idx_rest_calls_endpoint_sequence 
    ON rest_calls(endpoint_id, sequence_order);
