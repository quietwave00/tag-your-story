create table users
(
    user_id     bigint auto_increment primary key,
    created_at  datetime(6)  not null,
    updated_at  datetime(6)  not null,
    email       varchar(255) not null,
    nickname    varchar(255) not null,
    role        varchar(255) not null,
    user_key    varchar(255) not null,
    user_status varchar(255) not null,

    index idx_user_key (user_key)
) ENGINE = InnoDB;

create table artist
(
    artist_id      bigint auto_increment primary key,
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,
    name           varchar(255) not null,
    spotify_id     varchar(255) not null,
    musicbrainz_id varchar(255),

    constraint uk_artist_spotify_id unique (spotify_id),
    index idx_artist_musicbrainz_id (musicbrainz_id),
    index idx_artist_name (name)
) ENGINE = InnoDB;

create table album
(
    album_id       bigint auto_increment primary key,
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,
    title          varchar(255) not null,
    spotify_id     varchar(255) not null,
    musicbrainz_id varchar(255),
    release_year   int,

    constraint uk_album_spotify_id unique (spotify_id),
    index idx_album_musicbrainz_id (musicbrainz_id),
    index idx_album_title (title)
) ENGINE = InnoDB;

create table track
(
    track_id       bigint auto_increment primary key,
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,
    title          varchar(255) not null,
    spotify_id     varchar(255) not null,
    musicbrainz_id varchar(255),
    isrc            varchar(255),
    duration_ms     int          not null,
    album_id        bigint       not null,

    constraint uk_track_spotify_id unique (spotify_id),
    foreign key (album_id) references album (album_id),
    index idx_track_musicbrainz_id (musicbrainz_id),
    index idx_track_isrc (isrc),
    index idx_track_album_id (album_id),
    index idx_track_title (title)
) ENGINE = InnoDB;

create table album_artist
(
    album_artist_id bigint auto_increment primary key,
    album_id        bigint not null,
    artist_id       bigint not null,
    position        int    not null,

    constraint uk_album_artist unique (album_id, artist_id),
    constraint uk_album_artist_position unique (album_id, position),
    foreign key (album_id) references album (album_id),
    foreign key (artist_id) references artist (artist_id),
    index idx_album_artist_artist_album (artist_id, album_id)
) ENGINE = InnoDB;

create table track_artist
(
    track_artist_id bigint auto_increment primary key,
    track_id        bigint not null,
    artist_id       bigint not null,
    position        int    not null,

    constraint uk_track_artist unique (track_id, artist_id),
    constraint uk_track_artist_position unique (track_id, position),
    foreign key (track_id) references track (track_id),
    foreign key (artist_id) references artist (artist_id),
    index idx_track_artist_artist_track (artist_id, track_id)
) ENGINE = InnoDB;

create table tag
(
    tag_id             bigint auto_increment primary key,
    created_at         datetime(6)  not null,
    updated_at         datetime(6)  not null,
    name               varchar(255) not null,
    slug               varchar(255) not null,
    type               varchar(255) not null,
    status             varchar(255) not null,
    merged_into_tag_id bigint,
    description        varchar(255),

    constraint uk_tag_slug unique (slug),
    foreign key (merged_into_tag_id) references tag (tag_id),
    index idx_tag_type_status (type, status),
    index idx_tag_name (name)
) ENGINE = InnoDB;

create table tag_alias
(
    alias_id         bigint auto_increment primary key,
    tag_id           bigint       not null,
    alias            varchar(255) not null,
    normalized_alias varchar(255) not null,
    source           varchar(255) not null,
    status           varchar(255) not null,

    constraint uk_tag_alias_tag_normalized unique (tag_id, normalized_alias),
    foreign key (tag_id) references tag (tag_id),
    index idx_tag_alias_normalized_status (normalized_alias, status)
) ENGINE = InnoDB;

create table external_tag_observation
(
    observation_id bigint auto_increment primary key,
    subject_type   varchar(255) not null,
    subject_id     bigint       not null,
    source         varchar(255) not null,
    raw_name       varchar(255) not null,
    normalized_name varchar(255) not null,
    external_ref   varchar(255) not null,
    status         varchar(255) not null,
    matched_tag_id bigint,
    observed_at    datetime(6)  not null,

    constraint uk_external_tag_observation_identity unique
        (subject_type, subject_id, source, normalized_name, external_ref),
    foreign key (matched_tag_id) references tag (tag_id),
    index idx_external_tag_observation_subject (subject_type, subject_id),
    index idx_external_tag_observation_status_name (status, normalized_name)
) ENGINE = InnoDB;

create table tag_assertion
(
    assertion_id                bigint auto_increment primary key,
    subject_type               varchar(255) not null,
    subject_id                 bigint       not null,
    tag_id                     bigint       not null,
    source                     varchar(255) not null,
    evidence_type              varchar(255) not null,
    confidence                 double       not null,
    status                     varchar(255) not null,
    inherited_from_assertion_id bigint,
    created_at                 datetime(6)  not null,

    constraint uk_tag_assertion_identity unique
        (subject_type, subject_id, tag_id, source, evidence_type),
    foreign key (tag_id) references tag (tag_id),
    foreign key (inherited_from_assertion_id) references tag_assertion (assertion_id) on delete cascade,
    index idx_tag_assertion_subject_status (subject_type, subject_id, status),
    index idx_tag_assertion_tag (tag_id)
) ENGINE = InnoDB;

create table subject_tag_resolved
(
    resolved_id       bigint auto_increment primary key,
    subject_type      varchar(255) not null,
    subject_id        bigint       not null,
    tag_id            bigint       not null,
    score             double       not null,
    status            varchar(255) not null,
    resolution_reason varchar(255) not null,
    last_resolved_at  datetime(6)  not null,

    constraint uk_subject_tag_resolved_identity unique (subject_type, subject_id, tag_id),
    foreign key (tag_id) references tag (tag_id),
    index idx_subject_tag_resolved_subject_score (subject_type, subject_id, score)
) ENGINE = InnoDB;

create table board
(
    board_id   varchar(255)  not null primary key,
    created_at datetime(6)   not null,
    updated_at datetime(6)   not null,
    content    longtext      not null,
    count      int default 0 not null,
    status     varchar(255)  not null,
    track_id   varchar(255)  not null,
    user_id    bigint        not null,
    like_count int default 0 not null,
    foreign key (user_id) references users (user_id)
) ENGINE = InnoDB;

create table comments
(
    comment_id bigint auto_increment primary key,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    content    varchar(255) not null,
    status     varchar(255) not null,
    board_id   varchar(255) not null,
    parent_id  bigint,
    user_id    bigint       not null,

    foreign key (board_id) references board (board_id),
    foreign key (user_id) references users (user_id),
    foreign key (parent_id) references comments (comment_id)
) ENGINE = InnoDB;

create table user_tag
(
    user_tag_id    bigint auto_increment primary key,
    user_id        bigint       not null,
    name           varchar(255) not null,
    normalized_name varchar(255) not null,
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,

    constraint uk_user_tag_owner_normalized_name unique (user_id, normalized_name),
    foreign key (user_id) references users (user_id),
    index idx_user_tag_normalized_owner (normalized_name, user_id)
) ENGINE = InnoDB;

create table board_user_tag
(
    board_user_tag_id bigint auto_increment primary key,
    board_id         varchar(255) not null,
    user_tag_id       bigint       not null,
    created_at        datetime(6)   not null,
    updated_at        datetime(6)   not null,

    constraint uk_board_user_tag_board_tag unique (board_id, user_tag_id),
    foreign key (user_tag_id) references user_tag (user_tag_id),
    foreign key (board_id) references board (board_id),
    index idx_board_user_tag_tag_board (user_tag_id, board_id)
) ENGINE = InnoDB;

create table files
(
    file_id    bigint auto_increment primary key,
    file_level varchar(255) not null,
    file_name  varchar(255) not null,
    file_path  varchar(255) not null,
    board_id   varchar(255) not null,
    status     varchar(255) not null,

    foreign key (board_id) references board (board_id)
) ENGINE = InnoDB;

create table likes
(
    like_id  bigint auto_increment primary key,
    board_id varchar(255) not null,
    user_id  bigint       not null,

    foreign key (board_id) references board (board_id),
    foreign key (user_id) references users (user_id)
) ENGINE = InnoDB;

create table notification
(
    notification_id bigint auto_increment primary key,
    publisher_id bigint not null,
    subscriber_id bigint not null,
    type VARCHAR(10) not null,
    content_id VARCHAR(255) not null,
    is_read tinyint(1) not null default 0,
    created_at  datetime(6)  not null,
    updated_at  datetime(6)  not null,

    foreign key (publisher_id) references users (user_id),
    foreign key (subscriber_id) references users (user_id)
) ENGINE = InnoDB;

-- batch
-- Autogenerated: do not edit this schema
CREATE TABLE BATCH_JOB_INSTANCE  (
                                     JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY ,
                                     VERSION BIGINT ,
                                     JOB_NAME VARCHAR(100) NOT NULL,
                                     JOB_KEY VARCHAR(32) NOT NULL,
                                     constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION  (
                                      JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
                                      VERSION BIGINT  ,
                                      JOB_INSTANCE_ID BIGINT NOT NULL,
                                      CREATE_TIME DATETIME(6) NOT NULL,
                                      START_TIME DATETIME(6) DEFAULT NULL ,
                                      END_TIME DATETIME(6) DEFAULT NULL ,
                                      STATUS VARCHAR(10) ,
                                      EXIT_CODE VARCHAR(2500) ,
                                      EXIT_MESSAGE VARCHAR(2500) ,
                                      LAST_UPDATED DATETIME(6),
                                      constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
                                          references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
                                             JOB_EXECUTION_ID BIGINT NOT NULL ,
                                             PARAMETER_NAME VARCHAR(100) NOT NULL ,
                                             PARAMETER_TYPE VARCHAR(100) NOT NULL ,
                                             PARAMETER_VALUE VARCHAR(2500) ,
                                             IDENTIFYING CHAR(1) NOT NULL ,
                                             constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
                                                 references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION  (
                                       STEP_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
                                       VERSION BIGINT NOT NULL,
                                       STEP_NAME VARCHAR(100) NOT NULL,
                                       JOB_EXECUTION_ID BIGINT NOT NULL,
                                       CREATE_TIME DATETIME(6) NOT NULL,
                                       START_TIME DATETIME(6) DEFAULT NULL ,
                                       END_TIME DATETIME(6) DEFAULT NULL ,
                                       STATUS VARCHAR(10) ,
                                       COMMIT_COUNT BIGINT ,
                                       READ_COUNT BIGINT ,
                                       FILTER_COUNT BIGINT ,
                                       WRITE_COUNT BIGINT ,
                                       READ_SKIP_COUNT BIGINT ,
                                       WRITE_SKIP_COUNT BIGINT ,
                                       PROCESS_SKIP_COUNT BIGINT ,
                                       ROLLBACK_COUNT BIGINT ,
                                       EXIT_CODE VARCHAR(2500) ,
                                       EXIT_MESSAGE VARCHAR(2500) ,
                                       LAST_UPDATED DATETIME(6),
                                       constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
                                           references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
                                               STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
                                               SHORT_CONTEXT VARCHAR(2500) NOT NULL,
                                               SERIALIZED_CONTEXT TEXT ,
                                               constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
                                                   references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
                                              JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
                                              SHORT_CONTEXT VARCHAR(2500) NOT NULL,
                                              SERIALIZED_CONTEXT TEXT ,
                                              constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
                                                  references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION_SEQ (
                                          ID BIGINT NOT NULL,
                                          UNIQUE_KEY CHAR(1) NOT NULL,
                                          constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_STEP_EXECUTION_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_STEP_EXECUTION_SEQ);

CREATE TABLE BATCH_JOB_EXECUTION_SEQ (
                                         ID BIGINT NOT NULL,
                                         UNIQUE_KEY CHAR(1) NOT NULL,
                                         constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_JOB_EXECUTION_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_JOB_EXECUTION_SEQ);

CREATE TABLE BATCH_JOB_SEQ (
                               ID BIGINT NOT NULL,
                               UNIQUE_KEY CHAR(1) NOT NULL,
                               constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_JOB_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_JOB_SEQ);
