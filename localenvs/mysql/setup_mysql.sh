#!/bin/bash
# timezone
mysql_tzinfo_to_sql /usr/share/zoneinfo | sed 's/Local time zone must be set--see zic manual page/FCTY/' | mysql -uroot mysql

mysql -uroot -e "GRANT ALL PRIVILEGES ON *.* TO 'tagStory'@'%' WITH GRANT OPTION"
mysql -uroot -e "FLUSH PRIVILEGES;"

mysql -uroot -e "CREATE DATABASE tagStory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"