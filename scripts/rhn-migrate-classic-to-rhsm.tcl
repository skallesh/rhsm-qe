#!/usr/bin/expect

### Author: John Sefler
### This is a tcl expect script used to help automate the execution of rhn-migrate-classic-to-rhsm.
### The arguments to this script are passed as responses to the interactive input prompts by rhn-migrate-classic-to-rhsm.

# parse arguments to this script
set tool rhn-migrate-classic-to-rhsm
set options      [lindex $argv 0]
set rhnUsername  [lindex $argv 1]
set rhnPassword  [lindex $argv 2]
set regUsername  [lindex $argv 3]
set regPassword  [lindex $argv 4]
set slaIndex     [lindex $argv 5]
if {$argc != 6} {
  puts "Usage: ${argv0} ${tool}-options rhnUsername rhnPassword regUsername regPassword slaIndex"
  puts "(pass \"null\" for argument values that you do not expect $tool to interactively prompt for)"
  exit -1
}

# debugging
#puts options=$options
#puts rhnUsername=$rhnUsername
#puts rhnPassword=$rhnPassword
#puts regUsername=$regUsername
#puts regPassword=$regPassword
#puts slaIndex=$slaIndex

# launch rhn-migrate-classic-to-rhsm with options
if {$options != "null"} {
  eval spawn $tool $options;   # use eval to expand the $options into multiple argument options to the tool
} else {
  spawn $tool
}

# configure what to do when an unexpected eof occurs before or after the next expect call (MUST BE DEFINED AFTER spawn)
expect_after eof exit
expect_before eof exit
set timeout 180


# now respond to the interactive prompts from rhn-migrate-classic-to-rhsm ...

if {$rhnUsername != "null"} {
  #expect "RHN Username:" {send "${rhnUsername}\r"} timeout {exit -1}	# changed by bug 847380
  expect "Red Hat account:" {send "${rhnUsername}\r"} timeout {exit -1}
  expect "Password:" {send "${rhnPassword}\r"} timeout {exit -1}
}
if {$regUsername != "null"} {
  expect "System Engine Username:" {send "${regUsername}\r"} timeout {exit -1}
  expect "Password:" {send "${regPassword}\r"} timeout {exit -1}
}
if {$slaIndex != "null"} {
  expect "Please select a service level agreement for this system." {send "${slaIndex}\r"} timeout {exit -1}
}


# interact with the program again so as to catch the remaining stdout and exit code from the program, but use a few minutes for timeout in case there is an unexpected interactive prompt
interact timeout 180 {exit -1}
catch wait reason
exit [lindex $reason 3]
