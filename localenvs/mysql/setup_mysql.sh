#!/bin/bash

# 1. 타임존 설정 (-proot 추가)
mysql_tzinfo_to_sql /usr/share/zoneinfo | sed 's/Local time zone must be set--see zic manual page/FCTY/' | mysql -uroot -proot mysql

# 2. 계정이 없으면 생성 (있으면 넘어감)
mysql -uroot -proot -e "CREATE USER IF NOT EXISTS 'tagnote'@'%' IDENTIFIED WITH mysql_native_password BY 'tagnote';"

# 3. 데이터베이스 생성
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS tagnote DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 4. 권한 부여 및 적용
mysql -uroot -proot -e "GRANT ALL PRIVILEGES ON *.* TO 'tagnote'@'%' WITH GRANT OPTION;"
mysql -uroot -proot -e "FLUSH PRIVILEGES;"
