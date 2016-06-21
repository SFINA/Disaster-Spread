#!/bin/bash

   for (( i = 1 ; i <= 1; i++ ))

   do

  	 bsub -n 4 -W 10 java -cp dist/DisasterSpread.jar disasterspread.experiment.TotalDamagedNodes $i
  

   done


