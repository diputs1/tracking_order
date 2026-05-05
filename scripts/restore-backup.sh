#!/bin/bash
# restore-backup.sh
# Dùng để restore lại file backup SQL từ thư mục ./backup/ vào mysql-primary

if [ -z "$1" ]; then
  echo "Usage: ./scripts/restore-backup.sh <backup_file.sql.gz>"
  echo "Available backups in ./backup/:"
  ls -la ./backup/
  exit 1
fi

BACKUP_FILE=$1
echo "Restoring $BACKUP_FILE to mysql-primary..."

# Giải nén và đẩy vào mysql container
gunzip < "./backup/$BACKUP_FILE" | docker exec -i mysql-primary mysql -uroot -ppassword tracking_order

echo "Restore completed!"
