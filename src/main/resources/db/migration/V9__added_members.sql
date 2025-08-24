CREATE TABLE organization_members
(
    id              UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    role            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_organization_members PRIMARY KEY (id)
);

ALTER TABLE organization_members
    ADD CONSTRAINT uc_80237fd462dcaf534f880ab33 UNIQUE (organization_id, user_id);

ALTER TABLE organization_members
    REPLICA IDENTITY FULL;

ALTER TABLE organization_members
    ADD CONSTRAINT FK_ORGANIZATION_MEMBERS_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organizations (id);