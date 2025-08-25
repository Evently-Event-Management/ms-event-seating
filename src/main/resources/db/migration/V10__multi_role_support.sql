CREATE TABLE organization_member_roles
(
    organization_member_id UUID         NOT NULL,
    role                   VARCHAR(255) NOT NULL
);

ALTER TABLE organization_member_roles
    ADD CONSTRAINT fk_organization_member_roles_on_organization_member FOREIGN KEY (organization_member_id) REFERENCES organization_members (id);

ALTER TABLE organization_members
    DROP COLUMN role;