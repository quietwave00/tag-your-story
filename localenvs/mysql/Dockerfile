FROM mysql:8.1.0

COPY conf/mysqld.cnf /etc/mysql/conf.d/mysqld.cnf
COPY setup_mysql.sh /docker-entrypoint-initdb.d/setup_mysql.sh

RUN chmod 0644 /etc/mysql/conf.d/mysqld.cnf \
    && chmod 0755 /docker-entrypoint-initdb.d/setup_mysql.sh
