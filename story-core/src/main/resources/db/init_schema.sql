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

create table hashtag
(
    hashtag_id bigint auto_increment primary key,
    name       varchar(255) not null,

    index idx_hashtag_name (name)
) ENGINE = InnoDB;

create table board_hashtag
(
    board_hashtag_id bigint auto_increment primary key,
    board_id         varchar(255) not null,
    hashtag_id       bigint       not null,

    foreign key (hashtag_id) references hashtag (hashtag_id),
    foreign key (board_id) references board (board_id)
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
CREATE TABLE BATCH_JOB_INSTANCE
(
    JOB_INSTANCE_ID BIGINT       NOT NULL PRIMARY KEY,
    VERSION         BIGINT,
    JOB_NAME        VARCHAR(100) NOT NULL,
    JOB_KEY         VARCHAR(32)  NOT NULL,
    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
) ENGINE = InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION
(
    JOB_EXECUTION_ID           BIGINT        NOT NULL PRIMARY KEY,
    VERSION                    BIGINT,
    JOB_INSTANCE_ID            BIGINT        NOT NULL,
    CREATE_TIME                DATETIME(6)   NOT NULL,
    START_TIME                 DATETIME(6) DEFAULT NULL,
    END_TIME                   DATETIME(6) DEFAULT NULL,
    STATUS                     VARCHAR(10),
    EXIT_CODE                  VARCHAR(2500),
    EXIT_MESSAGE               VARCHAR(2500),
    LAST_UPDATED               DATETIME(6),
    JOB_CONFIGURATION_LOCATION VARCHAR(2500) NULL,
    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
        references BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
) ENGINE = InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS
(
    JOB_EXECUTION_ID BIGINT       NOT NULL,
    TYPE_CD          VARCHAR(6)   NOT NULL,
    KEY_NAME         VARCHAR(100) NOT NULL,
    STRING_VAL       VARCHAR(250),
    DATE_VAL         DATETIME(6) DEFAULT NULL,
    LONG_VAL         BIGINT,
    DOUBLE_VAL       DOUBLE PRECISION,
    IDENTIFYING      CHAR(1)      NOT NULL,
    constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
) ENGINE = InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION
(
    STEP_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
    VERSION            BIGINT       NOT NULL,
    STEP_NAME          VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID   BIGINT       NOT NULL,
    START_TIME         DATETIME(6)  NOT NULL,
    END_TIME           DATETIME(6) DEFAULT NULL,
    STATUS             VARCHAR(10),
    COMMIT_COUNT       BIGINT,
    READ_COUNT         BIGINT,
    FILTER_COUNT       BIGINT,
    WRITE_COUNT        BIGINT,
    READ_SKIP_COUNT    BIGINT,
    WRITE_SKIP_COUNT   BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT     BIGINT,
    EXIT_CODE          VARCHAR(2500),
    EXIT_MESSAGE       VARCHAR(2500),
    LAST_UPDATED       DATETIME(6),
    constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
) ENGINE = InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT
(
    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
        references BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
) ENGINE = InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT
(
    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
) ENGINE = InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION_SEQ
(
    ID         BIGINT  NOT NULL,
    UNIQUE_KEY CHAR(1) NOT NULL,
    constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE = InnoDB;

INSERT INTO BATCH_STEP_EXECUTION_SEQ (ID, UNIQUE_KEY)
select *
from (select 0 as ID, '0' as UNIQUE_KEY) as tmp
where not exists(select * from BATCH_STEP_EXECUTION_SEQ);

CREATE TABLE BATCH_JOB_EXECUTION_SEQ
(
    ID         BIGINT  NOT NULL,
    UNIQUE_KEY CHAR(1) NOT NULL,
    constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE = InnoDB;

INSERT INTO BATCH_JOB_EXECUTION_SEQ (ID, UNIQUE_KEY)
select *
from (select 0 as ID, '0' as UNIQUE_KEY) as tmp
where not exists(select * from BATCH_JOB_EXECUTION_SEQ);

CREATE TABLE BATCH_JOB_SEQ
(
    ID         BIGINT  NOT NULL,
    UNIQUE_KEY CHAR(1) NOT NULL,
    constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE = InnoDB;

INSERT INTO BATCH_JOB_SEQ (ID, UNIQUE_KEY)
select *
from (select 0 as ID, '0' as UNIQUE_KEY) as tmp
where not exists(select * from BATCH_JOB_SEQ);








