#!/bin/sh
##su - oracle <<EON
export ORACLE_SID=$1
export ORACLE_HOME=$2
$ORACLE_HOME/bin/lsnrctl start
$ORACLE_HOME/bin/sqlplus /nolog <<EOF
conn / as sysdba
startup
exit
EOF
##exit
##EON
