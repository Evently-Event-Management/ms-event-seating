CREATE TABLE discount_tiers
(
    discount_id UUID NOT NULL,
    tier_id     UUID NOT NULL
);

ALTER TABLE discount_tiers
    ADD CONSTRAINT fk_distie_on_discount FOREIGN KEY (discount_id) REFERENCES discounts (id);

ALTER TABLE discount_tiers
    ADD CONSTRAINT fk_distie_on_tier FOREIGN KEY (tier_id) REFERENCES tiers (id);