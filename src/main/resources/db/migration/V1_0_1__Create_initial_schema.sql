
-- Migration: V1_0_1__Create_initial_schema.sql
-- Location: src/main/resources/db/migration/V1_0_1__Create_initial_schema.sql

-- Create SOAP endpoints table
CREATE TABLE IF NOT EXISTS soap_endpoints (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    operation_name VARCHAR(255) NOT NULL,
    namespace VARCHAR(500) NOT NULL,
    wsdl_path VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(operation_name, namespace)
);

-- Create REST calls table (CORRECTED COLUMN NAMES)
CREATE TABLE IF NOT EXISTS rest_calls (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL,  -- FIXED: Changed from soap_endpoint_id to endpoint_id
    sequence_order INTEGER NOT NULL DEFAULT 1,
    method VARCHAR(10) NOT NULL DEFAULT 'GET',
    url_template VARCHAR(2000) NOT NULL,
    auth_type VARCHAR(50) DEFAULT 'NONE',
    timeout_ms INTEGER DEFAULT 30000,
    parallel_execution BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (endpoint_id) REFERENCES soap_endpoints(id) ON DELETE CASCADE  -- FIXED: Updated FK reference
);

-- Create mappings table (CORRECTED COLUMN NAMES)
CREATE TABLE IF NOT EXISTS mappings (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL,  -- FIXED: Changed from soap_endpoint_id to endpoint_id
    mapping_type VARCHAR(50) NOT NULL DEFAULT 'JOLT',
    mapping_definition TEXT,
    mapper_class VARCHAR(500),
    execution_order INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (endpoint_id) REFERENCES soap_endpoints(id) ON DELETE CASCADE  -- FIXED: Updated FK reference
);

-- Create error mappings table (ADDED - missing from original)
CREATE TABLE IF NOT EXISTS error_mappings (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL,
    error_code VARCHAR(50) NOT NULL,
    error_message TEXT,
    http_status_code INTEGER DEFAULT 500,
    soap_fault_code VARCHAR(100),
    soap_fault_string TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (endpoint_id) REFERENCES soap_endpoints(id) ON DELETE CASCADE
);

-- Create cache policies table (ADDED - missing from original)
CREATE TABLE IF NOT EXISTS cache_policies (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL,
    cache_enabled BOOLEAN DEFAULT false,
    cache_ttl_seconds INTEGER DEFAULT 300,
    cache_key_pattern VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (endpoint_id) REFERENCES soap_endpoints(id) ON DELETE CASCADE,
    UNIQUE(endpoint_id)  -- One cache policy per endpoint
);

-- Create indexes for performance (CORRECTED INDEX NAMES)
CREATE INDEX IF NOT EXISTS idx_soap_endpoints_operation_namespace ON soap_endpoints(operation_name, namespace);
CREATE INDEX IF NOT EXISTS idx_soap_endpoints_enabled ON soap_endpoints(enabled);
CREATE INDEX IF NOT EXISTS idx_rest_calls_endpoint_id ON rest_calls(endpoint_id);  -- FIXED: Updated column name
CREATE INDEX IF NOT EXISTS idx_rest_calls_sequence ON rest_calls(endpoint_id, sequence_order);  -- FIXED: Updated column name
CREATE INDEX IF NOT EXISTS idx_mappings_endpoint_id ON mappings(endpoint_id);  -- FIXED: Updated column name
CREATE INDEX IF NOT EXISTS idx_mappings_execution_order ON mappings(endpoint_id, execution_order);  -- FIXED: Updated column name
CREATE INDEX IF NOT EXISTS idx_error_mappings_endpoint_id ON error_mappings(endpoint_id);  -- ADDED
CREATE INDEX IF NOT EXISTS idx_cache_policies_endpoint_id ON cache_policies(endpoint_id);  -- ADDED

-- Insert sample data (optional - remove for production)
INSERT INTO soap_endpoints (name, operation_name, namespace, enabled)
VALUES
    ('IntegrationTestEndpoint', 'IntegrationTestOperation', 'http://integration.test.example.com', true)
ON CONFLICT (name) DO NOTHING;

-- Add updated_at trigger for soap_endpoints
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_soap_endpoints_updated_at
    BEFORE UPDATE ON soap_endpoints
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE soap_endpoints IS 'Stores SOAP endpoint configurations for dynamic REST-to-SOAP conversion';
COMMENT ON TABLE rest_calls IS 'Stores REST API call configurations for each SOAP endpoint';
COMMENT ON TABLE mappings IS 'Stores data transformation mappings (JOLT, Groovy, etc.) for each endpoint';
COMMENT ON TABLE error_mappings IS 'Stores error mapping configurations for each endpoint';
COMMENT ON TABLE cache_policies IS 'Stores caching policies for each endpoint';

-- Log successful migration
DO $$
BEGIN
    RAISE NOTICE 'Successfully created initial schema for REST-SOAP Converter v1.0.1 with corrected column names';
END $$;
