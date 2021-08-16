#!/bin/bash

now=$(date)
log="/var/log/cron.log"

{
  echo "----------------------------------------------------"
  echo "Current date is : $now"
  echo "Will start charging process....."
  echo "----------------------------------------------------"
} >> $log

result=$(curl -s http://localhost:7000/rest/v1/billing | python -mjson.tool)

if [[ $result == "[]" ]]; then
  echo "No more invoices left for payment" >> $log
else
  echo "$result" >> $log
fi