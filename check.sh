#!/bin/bash

curl -is --max-redirs 10 http://localhost:8080 -L | grep -w "HTTP/1.1 200" > /dev/null
if [ $? -ne "0" ]; then
   echo "============================================================="
   echo "Unable to reach sample springboot application on port 8080 !!"
   echo "============================================================="
else
   echo "================="
   echo "Smoke Test passed"
   echo "================="
fi

grep "CRITICAL" trivyresults.html > /dev/null
if [ $? -ne "0" ]; then
   echo "============================================================="
   echo "Docker Image java-application is ready for testing"
   echo "============================================================="
else
   echo "============================================================="
   echo "Docker Image java-application has vulnerabilities!!"
   echo "============================================================="
fi
