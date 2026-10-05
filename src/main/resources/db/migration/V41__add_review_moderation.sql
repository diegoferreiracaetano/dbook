-- A review can be edited by its author, hidden by the team (it stops counting and showing) and reported by customers.
ALTER TABLE review
    ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE', 'HIDDEN')),
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN hidden_reason VARCHAR(500),
    ADD COLUMN hidden_by BIGINT REFERENCES app_user (id),
    ADD COLUMN hidden_at TIMESTAMP;

-- One report per customer and review. A report is open until the team acts on the review (hides it, or dismisses the
-- reports): the moderation queue is the visible reviews that have an open report.
CREATE TABLE review_report (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES review (id) ON DELETE CASCADE,
    reporter_id BIGINT NOT NULL REFERENCES app_user (id),
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    UNIQUE (review_id, reporter_id)
);

CREATE INDEX idx_review_report_open ON review_report (review_id) WHERE resolved_at IS NULL;

-- The reviews of a destination go review -> booking -> flight -> airport: this is the missing hop (the destination).
CREATE INDEX idx_flight_destination ON flight (destination_airport_id);
