#!/bin/bash

###############################################################################
#
# This script runs first the RUBiS browsing mix, then the bidding mix 
# for each rubis.properties_XX specified where XX is the number of emulated
# clients. Note that the rubis.properties_XX files must be configured
# with the corresponding number of clients.
# In particular set the following variables in rubis.properties_XX:
# httpd_use_version = Servlets
# workload_number_of_clients_per_node = XX/number of client machines
# workload_transition_table = yourPath/RUBiS/workload/transitions.txt 
#
# This script should be run from the RUBiS/bench directory on the local 
# client machine. 
# Results will be generated in the RUBiS/bench directory.
#
################################################################################

SERVLETDIR='/home/abhi/voldormort/RUBiS/Servlets'
echo $SERVLETDIR

# Go back to RUBiS root directory
cd ..

# Browse only mix

cp ./workload/browse_only_transitions_7.txt ./workload/transitions.txt

# rubis.properties_100 rubis.properties_200 rubis.properties_300 rubis.properties_400 rubis.properties_500 rubis.properties_600 rubis.properties_700 rubis.properties_800 rubis.properties_900 rubis.properties_1000 rubis.properties_1100 rubis.properties_1200 rubis.properties_1300 rubis.properties_1400 rubis.properties_1500 rubis.properties_1600 rubis.properties_1700 rubis.properties_1800 rubis.properties_1900 rubis.properties_2000

for i in rubis.properties_200; do
  cp bench/MyServlets/$i Client/rubis.properties
  echo ${SERVLETDIR}/tomcat_stop.sh
  ssh localhost -n -l abhi ${SERVLETDIR}/tomcat_stop.sh 
  sleep 4
  ssh localhost ${SERVLETDIR}/update_ids.sh
  ssh localhost -n -l abhi ${SERVLETDIR}/tomcat_start.sh &
  sleep 4
  bench/flush_cache 190000
  ssh localhost RUBiS/bench/flush_cache 190000	# remote client
  ssh localhost RUBiS/bench/flush_cache 780000 	# servlet server
  ssh localhost RUBiS/bench/flush_cache 780000 	# web server
  ssh localhost RUBiS/bench/flush_cache 780000	# database
  make emulator
done

# Bidding mix

cp ./workload/default_transitions_7.txt ./workload/transitions.txt

# rubis.properties_100 rubis.properties_200 rubis.properties_300 rubis.properties_400 rubis.properties_500 rubis.properties_600 rubis.properties_700 rubis.properties_800 rubis.properties_900 rubis.properties_1000 rubis.properties_1100 rubis.properties_1200 rubis.properties_1300 rubis.properties_1400 rubis.properties_1500 rubis.properties_1600 rubis.properties_1700 rubis.properties_1800 rubis.properties_1900 rubis.properties_2000

for i in rubis.properties_200; do
  cp bench/MyServlets.properties/$i Client/rubis.properties
  ssh localhost -n -l abhi ${SERVLETDIR}/tomcat_stop.sh 
  sleep 4
  ssh localhost ${SERVLETDIR}/update_ids.sh
  ssh localhost -n -l abhi ${SERVLETDIR}/tomcat_start.sh &
  bench/flush_cache 190000
  ssh localhost RUBiS/bench/flush_cache 190000	# remote client
  ssh localhost RUBiS/bench/flush_cache 780000 	# servlet server
  ssh localhost RUBiS/bench/flush_cache 780000 	# web server
  ssh localhost RUBiS/bench/flush_cache 780000	# database
  make emulator
done

sleep 4

