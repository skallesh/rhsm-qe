#!/usr/bin/expect

### WORK IN PROGRESS to replace implementation in MigrateTests.executeRhnMigrateClassicToRhsmWithOptions
###ORIGINAL CALL  expect -c "spawn rhn-migrate-classic-to-rhsm --no-auto; expect \"*Username:\"; send qa@redhat.com\n; expect \"*Password:\"; send CHANGE-ME\n; expect \"*Username:\"; send qa@redhat.com\n; expect \"*Password:\"; send CHANGE-ME\n;  interact; catch wait reason; exit [lindex \$reason 3]"

# parse arguments to this script
set options			[lindex $argv 0]
set rhnUsername		[lindex $argv 1]
set rhnPassword		[lindex $argv 2]
set seUsername		[lindex $argv 3]
set sePassword		[lindex $argv 4]
set serviceLevel	[lindex $argv 5]

# configure expect to abort when blocking on a prompt that does not come
set timeout 10


# launch rhn-migrate-classic-to-rhsm with options
spawn rhn-migrate-classic-to-rhsm $options

# now respond to the interactive prompts from rhn-migrate-classic-to-rhsm ...


# Not sure if this is a good idea
#expect {
#	"^RHN Username:" { send $rhnUsername\n; expect Password: {send $rhnPassword\n}}
#	"^System Engine Username:" { send $seUsername\n; expect Password: {send $sePassword\n}}
#}

expect "RHN Username:" {send "${rhnUsername}\r"}
expect "Password:" {send "${rhnPassword}\r"}
expect "System Engine Username:" {send "${seUsername}\r"}
expect "Password:" {send "${sePassword}\r"}


if {serviceLevel != "null"} {
	expect "Please select a service level agreement for this system." {send "${serviceLevel}\r"}
}


interact
catch wait reason
exit [lindex $reason 3]