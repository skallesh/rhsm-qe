#! /bin/bash

#############################################
# This script listens eternally on a port but never responds.
# It was created to help test the python-rhsm RFE Bug 1346417 - Allow users to set socket timeout.
# Following are example setup steps...
#
# 1. On a test environment server logged in as root, create a dummy certificate for the NCat listener to use.
# 
# [root@auto-services timeout_listener]# openssl genrsa -out timeout_listener.key 4096
# [root@auto-services timeout_listener]# openssl req -new -x509 -key timeout_listener.key -out timeout_listener.pem -days 3650 -subj '/CN=auto-services.usersys.redhat.com/C=US/L=Raleigh'
# View the resultant certificate with
# [root@auto-services timeout_listener]# openssl x509 -in timeout_listener.pem -noout -text
#
# 2. Install NCat
# [root@auto-services timeout_listener]# yum install nmap-ncat
#
# 3. Start NCat configured to listen forever and to accept SSL/TLS connections on port 8883.
# [root@auto-services timeout_listener]# nc --ssl --ssl-key ./timeout_listener.key --ssl-cert ./timeout_listener.pem --listen --keep-open 8883
#
# OR encapsulate the command above into a script, timeout_listener.sh, that
#    can be added to SystemD using the `timeout_listener.service` file.
#    Add the timeout_listener.service file to `/etc/systemd/system/` and then 
#    enable and start the service with `systemctl enable timeout_listener` and `systemctl start
#    timeout_listener`. 
# [root@auto-services timeout_listener]# chmod 744 timeout_listener.sh
# [root@auto-services timeout_listener]# systemctl enable timeout_listener
# [root@auto-services timeout_listener]# systemctl start timeout_listener
# View the systemd status with
# [root@auto-services timeout_listener]# systemctl status timeout_listener
#
# 4. On the client: Copy timeout_listener.pem to /etc/rhsm/ca/timeout_listener.pem
#
# 5. On the client: Configure rhsm.conf with server=auto-services.usersys.redhat.com
# and port=8883 and, optionally, server_timeout=X where X is some number of seconds. 
# [root@jsefler-rhel7 ~]# subscription-manager config --server.hostname=auto-services.usersys.redhat.com --server.port=8883 --server.server_timeout=4
#
# 6. Run `subscription-manager version` which will make a call
# to /status and then timeout in X seconds.
# [root@jsefler-rhel7 ~]# time subscription-manager version
# Unable to verify server's identity: timed out
#
# real	0m4.506s
# user	0m0.233s
# sys	0m0.031s
#
#############################################

PORT=8883; # assumes this port is available, you can check by calling netstat -an | grep <port_number>

echo "Listening on $PORT forever.  Ctrl-C to cancel."
nc --ssl --ssl-key ./timeout_listener.key --ssl-cert ./timeout_listener.pem --listen --keep-open $PORT

