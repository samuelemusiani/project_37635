#!/bin/bash
input="./board_configurations.txt"
OUTPUT=""

if [[ -z $1 ]]; then
  echo "Testing: (for verbose output use -v)"
fi

while IFS= read -r line
do
  tmp_out=$(java -jar ./build/libs/connectx.jar ${line} connectx.L0.L0 connectx.L1.L1)
  
  if [[ -n $1 ]]; then
    if [[ $1 = "-v" ]]; then 
      echo "Game $line":
      echo "$tmp_out"
    fi
  else 
    echo -n "-"
  fi

  OUTPUT+=$tmp_out
  OUTPUT+='\n'
done < "$input"

echo "|"
echo -e $OUTPUT | awk 'BEGIN {
  L0_score = 0; L0_won = 0; L0_lost = 0; L0_draw = 0; L0_error = 0; \
  L1_score = 0; L1_won = 0; L1_lost = 0; L1_draw = 0; L1_error = 0} \
  /L0/ {L0_score += $3; L0_won += $5; L0_lost += $7; L0_draw += $9; L0_error += $11} \
  /L1/ {L1_score += $14; L1_won += $16; L1_lost += $18; L1_draw += $20; L1_error += $11} \
  END {print "\t\tPoints\tWon\tLosted\tDrawed\tErrors"; \
    print "Final L0:\t"L0_score"\t"L0_won"\t"L0_lost"\t"L0_draw"\t"L0_error; \
    print "Final L1:\t"L1_score"\t"L1_won"\t"L1_lost"\t"L1_draw"\t"L1_error}'
