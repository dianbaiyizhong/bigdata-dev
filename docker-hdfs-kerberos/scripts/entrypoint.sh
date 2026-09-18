#!/bin/bash

HOSTNAME=$(hostname -f)
echo "=== Hostname: $HOSTNAME ==="

# Step 1: Initialize KDC if first run
if [ ! -f /var/lib/krb5kdc/principal ]; then
    echo "=== Initializing KDC ==="
    mkdir -p /etc/krb5kdc

    cat > /etc/krb5kdc/kdc.conf <<EOF
[kdcdefaults]
    kdc_ports = 88

[realms]
    EXAMPLE.COM = {
        database_name = /var/lib/krb5kdc/principal
        admin_keytab = /etc/krb5kdc/kadm5.keytab
        acl_file = /etc/krb5kdc/kadm5.acl
        key_stash_file = /etc/krb5kdc/stash
        max_life = 10h 0m 0s
        max_renewable_life = 7d 0m 0s
        master_key_type = aes256-cts
        supported_enctypes = aes256-cts:normal aes128-cts:normal
    }
EOF

    echo "*/admin@EXAMPLE.COM  *" > /etc/krb5kdc/kadm5.acl

    # Create realm
    kdb5_util create -r EXAMPLE.COM -s -P hadoop

    # Create principals
    kadmin.local -q "addprinc -pw hadoop admin/admin@EXAMPLE.COM"
    kadmin.local -q "addprinc -randkey nn/${HOSTNAME}@EXAMPLE.COM"
    kadmin.local -q "addprinc -randkey dn/${HOSTNAME}@EXAMPLE.COM"
    kadmin.local -q "addprinc -randkey HTTP/${HOSTNAME}@EXAMPLE.COM"
    kadmin.local -q "addprinc -pw hadoop hdfs@EXAMPLE.COM"

    # Export keytabs
    mkdir -p /etc/security/keytabs
    kadmin.local -q "ktadd -k /etc/security/keytabs/nn.keytab nn/${HOSTNAME}@EXAMPLE.COM"
    kadmin.local -q "ktadd -k /etc/security/keytabs/dn.keytab dn/${HOSTNAME}@EXAMPLE.COM"
    kadmin.local -q "ktadd -k /etc/security/keytabs/http.keytab HTTP/${HOSTNAME}@EXAMPLE.COM"
    kadmin.local -q "ktadd -k /etc/security/keytabs/hdfs.keytab hdfs@EXAMPLE.COM"
    chmod 400 /etc/security/keytabs/*.keytab

    echo "=== KDC initialized ==="
fi

# Step 2: Start Kerberos services
echo "=== Starting KDC ==="
krb5kdc
kadmind
sleep 2

# Step 3: Replace _HOST in hadoop configs
sed -i "s/_HOST/$HOSTNAME/g" $HADOOP_HOME/etc/hadoop/hdfs-site.xml

# Step 4: Format namenode if not formatted
if [ ! -d /data/namenode/current ]; then
    echo "=== Formatting NameNode ==="
    $HADOOP_HOME/bin/hdfs namenode -format -force -nonInteractive
fi

export HDFS_NAMENODE_OPTS="$HDFS_NAMENODE_OPTS -Dsun.security.krb5.maxTicketSize=65536 -Dgss.native=false"

# Step 5: Start HDFS daemons
echo "=== Starting HDFS ==="
$HADOOP_HOME/bin/hdfs --daemon start namenode || true
$HADOOP_HOME/bin/hdfs --daemon start datanode || true

sleep 3

# Make HDFS root writable for hdfs user
kinit -k -t /etc/security/keytabs/nn.keytab nn/${HOSTNAME}@EXAMPLE.COM 2>/dev/null
hdfs dfs -chmod 777 / 2>/dev/null || true

echo ""
echo "============================================"
echo " HDFS with Kerberos is READY"
echo "============================================"
echo " Login:  echo hadoop | kinit hdfs@EXAMPLE.COM"
echo " Test:   hdfs dfs -ls /"
echo " Web UI: http://localhost:9870"
echo ""
echo " Kerberos principals:"
echo "   nn/${HOSTNAME}@EXAMPLE.COM"
echo "   dn/${HOSTNAME}@EXAMPLE.COM"
echo "   hdfs@EXAMPLE.COM  (password: hadoop)"
echo "============================================"

# Keep running
tail -f /dev/null
