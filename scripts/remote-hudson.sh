#!/bin/bash

URL="http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/rhsm-beaker-on-premises/build"

#rhel6-x86_64
JSON="{\"parameter\": [{\"name\": \"CLIENT1_ARCH\",         \"value\": \"x86_64\"} , \
                       {\"name\": \"CLIENT1_DistroFamily\", \"value\": \"RedHatEnterpriseLinux6\"}], \"\": \"\"}"
                       
echo "Queing Job:  curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json='$JSON'"
curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json="$JSON"
echo
sleep 10

#rhel6-i386
JSON="{\"parameter\": [{\"name\": \"CLIENT1_ARCH\",         \"value\": \"i386\"} , \
                       {\"name\": \"CLIENT1_DistroFamily\", \"value\": \"RedHatEnterpriseLinux6\"}], \"\": \"\"}"
echo "Queing Job:  curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json='$JSON'"
curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json="$JSON"
echo
sleep 10

#rhel6-ia64
JSON="{\"parameter\": [{\"name\": \"CLIENT1_ARCH\",         \"value\": \"ia64\"} , \
                       {\"name\": \"CLIENT1_DistroFamily\", \"value\": \"RedHatEnterpriseLinux6\"}], \"\": \"\"}"
echo "Queing Job:  curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json='$JSON'"
curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json="$JSON"
echo
sleep 10

#rhel6-ppc64
JSON="{\"parameter\": [{\"name\": \"CLIENT1_ARCH\",         \"value\": \"ppc64\"} , \
                       {\"name\": \"CLIENT1_DistroFamily\", \"value\": \"RedHatEnterpriseLinux6\"}], \"\": \"\"}"
echo "Queing Job:  curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json='$JSON'"
curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json="$JSON"
echo
sleep 10

#rhel6-s390x
JSON="{\"parameter\": [{\"name\": \"CLIENT1_ARCH\",         \"value\": \"s390x\"} , \
                       {\"name\": \"CLIENT1_DistroFamily\", \"value\": \"RedHatEnterpriseLinux6\"}], \"\": \"\"}"
echo "Queing Job:  curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json='$JSON'"
curl -X POST $URL -d token=hudsonbeaker-remote --data-urlencode json="$JSON"
echo
sleep 10
echo
