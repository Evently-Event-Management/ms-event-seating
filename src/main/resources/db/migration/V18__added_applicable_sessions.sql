ALTER TABLE discounts
DROP
CONSTRAINT fk_discounts_on_session;

CREATE TABLE discount_sessions
(
    discount_id UUID NOT NULL,
    session_id  UUID NOT NULL
);

ALTER TABLE discount_sessions
    ADD CONSTRAINT fk_disses_on_discount FOREIGN KEY (discount_id) REFERENCES discounts (id);

ALTER TABLE discount_sessions
    ADD CONSTRAINT fk_disses_on_event_session FOREIGN KEY (session_id) REFERENCES event_sessions (id);

ALTER TABLE discounts
DROP
COLUMN session_id;