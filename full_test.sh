#!/bin/bash
input="./board_configurations.txt"

print_usage() {
  echo -e "\nThis script is used to test two player among all the combination of\n \
the game that are required (and stored in ${input})\n"
  echo -e "\nUSAGE:"
  echo -e "\t${0} [flags] player0 player1"
  echo -e "\n"
  echo -e "The program require at least two arguments: player0 and player1 are \n \
mandatory! Es. L0 and L1\n"
  echo -e "\t -v: Verbose flag to see each game result"
}


if [[ $# -lt 2 ]]; then
  echo "Wrong number of argumets. Please insert at least two players!"
  print_usage
  exit
fi

VERBOSE=false
while getopts v: flag
do
  case "${flag}" in
    v) VERBOSE=true;;
    *) echo "Unkown flag used!!"
      print_usage
      exit;;
  esac
done

if [[ ${VERBOSE} = false ]]; then
  echo "Testing: (for verbose output use -v)"
fi

PLAYER0=${*: -2:1}
PLAYER1=${*:$#}

OUTPUT=""
while IFS= read -r line
do
  if [[ $VERBOSE  = true ]]; then
    echo "Game $line":
  fi

  tmp_out=$(java -jar ./build/libs/connectx.jar ${line} connectx.${PLAYER0}.${PLAYER0} connectx.${PLAYER1}.${PLAYER1})

  if [[ $VERBOSE  = true ]]; then
    echo "$tmp_out"
  else 
    echo -n "-"
  fi


  OUTPUT+=$tmp_out
  OUTPUT+='\n'
done < "$input"

echo "|"
echo -e :$OUTPUT | awk -v pat0=${PLAYER0} -v pat1=${PLAYER1} 'BEGIN {
  P0_score = 0; P0_won = 0; P0_lost = 0; P0_draw = 0; P0_error = 0; \
  P1_score = 0; P1_won = 0; P1_lost = 0; P1_draw = 0; P1_error = 0} \
  {P0_score += $3; P0_won += $5; P0_lost += $7; P0_draw += $9; P0_error += $11} \
  {P1_score += $14; P1_won += $16; P1_lost += $18; P1_draw += $20; P1_error += $22} \
  END {print "\t\tPoints\tWon\tLosted\tDrawed\tErrors"; \
    print "Final P0:\t"P0_score"\t"P0_won"\t"P0_lost"\t"P0_draw"\t"P0_error; \
    print "Final P1:\t"P1_score"\t"P1_won"\t"P1_lost"\t"P1_draw"\t"P1_error}'
