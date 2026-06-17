--liquibase formatted sql

--changeset taskhive:1 labels:schema comment:Create sequences
CREATE SEQUENCE users_seq                 INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE workspaces_seq            INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE global_roles_seq          INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE user_global_roles_seq     INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE workspace_members_seq     INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE projects_seq              INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE project_members_seq       INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE task_statuses_seq         INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE task_state_transitions_seq INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE tasks_seq                 INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE comments_seq              INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE tags_seq                  INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE task_tags_seq             INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE activity_logs_seq         INCREMENT BY 50 START WITH 1;
CREATE SEQUENCE task_audit_logs_seq       INCREMENT BY 50 START WITH 1;

--changeset taskhive:2 labels:schema comment:Create global_roles table
CREATE TABLE global_roles (
    global_role_id BIGINT PRIMARY KEY,
    role_name       VARCHAR(50) NOT NULL UNIQUE
);

--changeset taskhive:3 labels:schema comment:Create users table
CREATE TABLE users (
    user_id       BIGINT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255),
    is_active     BOOLEAN      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ
);

--changeset taskhive:4 labels:schema comment:Create workspaces table
CREATE TABLE workspaces (
    workspace_id   BIGINT PRIMARY KEY,
    workspace_name VARCHAR(255) NOT NULL,
    owner_id       BIGINT       NOT NULL REFERENCES users (user_id),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ,
    deleted_at     TIMESTAMPTZ
);

--changeset taskhive:5 labels:schema comment:Create user_global_roles table
CREATE TABLE user_global_roles (
    user_global_role_id BIGINT PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users (user_id),
    global_role_id      BIGINT      NOT NULL REFERENCES global_roles (global_role_id),
    valid_from          TIMESTAMPTZ NOT NULL,
    valid_to            TIMESTAMPTZ
);

--changeset taskhive:6 labels:schema comment:Create workspace_members table
CREATE TABLE workspace_members (
    workspace_member_id     BIGINT PRIMARY KEY,
    workspace_id            BIGINT      NOT NULL REFERENCES workspaces (workspace_id),
    user_id                 BIGINT      NOT NULL REFERENCES users (user_id),
    workspace_role          VARCHAR(50) NOT NULL,
    workspace_member_status VARCHAR(50) NOT NULL,
    valid_from              TIMESTAMPTZ NOT NULL,
    valid_to                TIMESTAMPTZ
);

--changeset taskhive:7 labels:schema comment:Create projects table
CREATE TABLE projects (
    project_id  BIGINT PRIMARY KEY,
    workspace_id BIGINT      NOT NULL REFERENCES workspaces (workspace_id),
    creator_id  BIGINT      NOT NULL REFERENCES users (user_id),
    name        VARCHAR(255) NOT NULL,
    project_key VARCHAR(10)  NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_workspace_project_key UNIQUE (workspace_id, project_key)
);

--changeset taskhive:8 labels:schema comment:Create project_members table
CREATE TABLE project_members (
    project_member_id     BIGINT PRIMARY KEY,
    project_id            BIGINT      NOT NULL REFERENCES projects (project_id),
    user_id               BIGINT      NOT NULL REFERENCES users (user_id),
    project_role          VARCHAR(50) NOT NULL,
    project_member_status VARCHAR(50) NOT NULL,
    valid_from            TIMESTAMPTZ NOT NULL,
    valid_to              TIMESTAMPTZ
);

--changeset taskhive:9 labels:schema comment:Create task_statuses table
CREATE TABLE task_statuses (
    status_id   BIGINT PRIMARY KEY,
    workspace_id BIGINT       NOT NULL REFERENCES workspaces (workspace_id),
    status_name VARCHAR(255)  NOT NULL,
    sort_order  INT           NOT NULL,
    CONSTRAINT uq_workspace_status_name UNIQUE (workspace_id, status_name)
);

--changeset taskhive:10 labels:schema comment:Create task_state_transitions table
CREATE TABLE task_state_transitions (
    transition_id  BIGINT PRIMARY KEY,
    from_status_id BIGINT NOT NULL REFERENCES task_statuses (status_id),
    to_status_id   BIGINT NOT NULL REFERENCES task_statuses (status_id),
    CONSTRAINT uq_from_to_status UNIQUE (from_status_id, to_status_id)
);

--changeset taskhive:11 labels:schema comment:Create tasks table
CREATE TABLE tasks (
    task_id     BIGINT PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES projects (project_id),
    creator_id  BIGINT       NOT NULL REFERENCES users (user_id),
    assignee_id BIGINT                REFERENCES users (user_id),
    status_id   BIGINT       NOT NULL REFERENCES task_statuses (status_id),
    priority    VARCHAR(50),
    task_number INT          NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    due_date    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ
);

--changeset taskhive:12 labels:schema comment:Create comments table
CREATE TABLE comments (
    comment_id BIGINT PRIMARY KEY,
    task_id    BIGINT      NOT NULL REFERENCES tasks (task_id),
    user_id    BIGINT      NOT NULL REFERENCES users (user_id),
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

--changeset taskhive:13 labels:schema comment:Create tags table
CREATE TABLE tags (
    tag_id       BIGINT PRIMARY KEY,
    workspace_id BIGINT       NOT NULL REFERENCES workspaces (workspace_id),
    name         VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_workspace_tag_name UNIQUE (workspace_id, name)
);

--changeset taskhive:14 labels:schema comment:Create task_tags table
CREATE TABLE task_tags (
    task_tag_id BIGINT PRIMARY KEY,
    task_id     BIGINT      NOT NULL REFERENCES tasks (task_id),
    tag_id      BIGINT      NOT NULL REFERENCES tags (tag_id),
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_task_tag UNIQUE (task_id, tag_id)
);

--changeset taskhive:15 labels:schema comment:Create activity_logs table
CREATE TABLE activity_logs (
    log_id         BIGINT PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (user_id),
    action_type    VARCHAR(100) NOT NULL,
    description    TEXT,
    target_user_id BIGINT                REFERENCES users (user_id),
    workspace_id   BIGINT                REFERENCES workspaces (workspace_id),
    project_id     BIGINT                REFERENCES projects (project_id),
    task_id        BIGINT                REFERENCES tasks (task_id),
    created_at     TIMESTAMPTZ  NOT NULL
);

--changeset taskhive:16 labels:schema comment:Create task_audit_logs table
CREATE TABLE task_audit_logs (
    log_id              BIGINT PRIMARY KEY,
    task_id             BIGINT       NOT NULL REFERENCES tasks (task_id),
    changed_by_user_id  BIGINT       NOT NULL REFERENCES users (user_id),
    change_type         VARCHAR(100) NOT NULL,
    old_value           TEXT,
    new_value           TEXT,
    changed_at          TIMESTAMPTZ  NOT NULL
);
