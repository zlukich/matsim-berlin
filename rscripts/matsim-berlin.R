library("matsim")
library("sf")
library("tidyverse")
library("ggalluvial")
base_output = readTripsTable("base_output_trips.csv.gz")
policy_output = readTripsTable("policy_output_trips.csv.gz")
affected_agents = read.csv2("smth.csv")
in_berlin_base = filterByRegion(base_output,st_read("bezirke"),31468)
anzahl_in_berlin = nrow(distinct(in_berlin_base %>% select(person))) # Anzahl personen, die in Berlin fahren
print(anzahl_in_berlin)
filtered_base_output = base_output %>% filter(person %in% affected_agents$AgentId)
filtered_policy_output = policy_output %>% filter(person %in% affected_agents$AgentId)

total_base_kms = sum(filtered_base_output$traveled_distance)
total_policy_kms = sum(filtered_policy_output$traveled_distance)

print(total_base_kms)
print(total_policy_kms)

agent_trips_base = base_output %>% filter(person == 397017401)
agent_trips_policy = policy_output %>% filter(person == 397017401)

agent_trips_base %>% select(person,trip_number,dep_time,trav_time,traveled_distance,main_mode,start_activity_type,end_activity_type)
agent_trips_policy %>% select(person,trip_number,dep_time,trav_time,traveled_distance,main_mode,start_activity_type,end_activity_type)