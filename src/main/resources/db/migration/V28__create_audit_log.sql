CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id BIGINT NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    action VARCHAR(50) NOT NULL,
    outcome VARCHAR(10) NOT NULL CHECK (outcome IN ('SUCCESS', 'DENIED')),
    target_type VARCHAR(20) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    state_before JSONB,
    state_after JSONB,
    reason VARCHAR(500),
    request_id VARCHAR(64),
    trace_id VARCHAR(64),
    ip VARCHAR(45),
    user_agent VARCHAR(255)
);

CREATE INDEX idx_audit_log_target ON audit_log (target_type, target_id, occurred_at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_id, occurred_at DESC);
CREATE INDEX idx_audit_log_recent ON audit_log (occurred_at DESC, id DESC);

CREATE FUNCTION audit_log_is_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_is_append_only();

CREATE TRIGGER audit_log_no_truncate
    BEFORE TRUNCATE ON audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION audit_log_is_append_only();
