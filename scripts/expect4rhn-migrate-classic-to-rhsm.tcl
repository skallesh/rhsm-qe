#!/usr/bin/expect

### 08/06/2012 jsefler WORK IN PROGRESS to replace implementation in MigrateTests.executeRhnMigrateClassicToRhsmWithOptions
###ORIGINAL CALL  expect -c "spawn rhn-migrate-classic-to-rhsm --no-auto; expect \"*Username:\"; send qa@redhat.com\n; expect \"*Password:\"; send CHANGE-ME\n; expect \"*Username:\"; send qa@redhat.com\n; expect \"*Password:\"; send CHANGE-ME\n;  interact; catch wait reason; exit [lindex \$reason 3]"

# parse arguments to this script
set tool rhn-migrate-classic-to-rhsm
set options      [lindex $argv 0]
set rhnUsername  [lindex $argv 1]
set rhnPassword  [lindex $argv 2]
set seUsername   [lindex $argv 3]
set sePassword   [lindex $argv 4]
set serviceLevel [lindex $argv 5]
if {$argc != 6} {
  puts "Usage: ${argv0} ${tool}-options rhnUsername rhnPassword seUsername sePassword serviceLevel"
  exit -1
}

# debugging
#puts options=$options
#puts rhnUsername=$rhnUsername
#puts rhnPassword=$rhnPassword
#puts seUsername=$seUsername
#puts sePassword=$sePassword
#puts serviceLevel=$serviceLevel

# launch rhn-migrate-classic-to-rhsm with options
if {$options != "null"} {
  spawn $tool $options
} else {
  spawn $tool
}

# configure what to do when an unexpected eof occurs before or after the next expect call (MUST BE DEFINED AFTER spawn)
expect_after eof exit
expect_before eof exit
set timeout 10


# now respond to the interactive prompts from rhn-migrate-classic-to-rhsm ...

if {$rhnUsername != "null"} {
  expect "xRHN Username:" {send "${rhnUsername}\r"} timeout {exit -1}
  expect "Password:" {send "${rhnPassword}\r"} timeout {exit -1}
}
if {$seUsername != "null"} {
  expect "System Engine Username:" {send "${seUsername}\r"} timeout {exit -1}
  expect "Password:" {send "${sePassword}\r"} timeout {exit -1}
}
if {$serviceLevel != "null"} {
  expect "Please select a service level agreement for this system." {send "${serviceLevel}\r"} timeout {exit -1}
}


interact
catch wait reason
exit [lindex $reason 3]
