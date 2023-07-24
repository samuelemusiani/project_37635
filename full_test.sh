#!/bin/bash
input="./board_configurations.txt"
OUTPUT=""

while IFS= read -r line
do
  echo "Game $line":
  tmp_out=$(java -jar ./build/libs/connectx.jar ${line} connectx.L0.L0 connectx.L1.L1)
  echo "$tmp_out"
  OUTPUT+=$tmp_out
  OUTPUT+='\n'
done < "$input"

echo -e $OUTPUT | awk 'BEGIN {
  L0_score = 0; L0_won = 0; L0_lost = 0; L0_draw = 0; L0_error = 0; \
  L1_score = 0; L1_won = 0; L1_lost = 0; L1_draw = 0; L1_error = 0} \
  /L0/ {L0_score += $3; L0_won += $5; L0_lost += $7; L0_draw += $9; L0_error += $11} \
  /L1/ {L1_score += $14; L1_won += $16; L1_lost += $18; L1_draw += $20; L1_error += $11} \
  END {print "Final count for L0:", L0_score, L0_won, L0_lost, L0_draw, L0_error; \
  print "Final count for L1:", L1_score, L1_won, L1_lost, L1_draw, L1_error}'
