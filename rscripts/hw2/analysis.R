library(tidyverse)
library(matsim)
library(sf)

unique_modes <- function(table){
  table_modes = table %>% mutate(uniq = strsplit(modes,"-"))
  table_modes = table_modes %>% mutate(uniq = sapply(uniq,unique)) %>% mutate(len = sapply(uniq,length))
  return(table_modes)
}

plotIntermodalModalShare <- function(table){
  
}

opened_trips_csv = readTripsTable("opened")
closed_trips_csv = readTripsTable("closed")

###uniq modes
opened_uniq = unique_modes(opened_trips_csv)
closed_uniq = unique_modes(closed_trips_csv)
 
opened_uniq = opened_uniq %>% mutate(uniq = sapply(uniq,str_sort))
opened_uniq = opened_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))
closed_uniq = closed_uniq %>% mutate(uniq = sapply(uniq,str_sort))
closed_uniq = closed_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))

closed_uniq = closed_uniq %>% mutate(uniq = sapply(uniq,str_sort))
closed_uniq = closed_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))



closed_uniq = closed_uniq %>% mutate(uniq = sapply(uniq,str_sort))
closed_uniq = closed_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))

closed_uniq = closed_uniq %>% mutate(uniq = sapply(uniq,str_sort))
closed_uniq = closed_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))


###filtering of agents with shape file
shapeTable = st_read("shape")
opened_home_activitites = opened_trips_csv %>% mutate(start_activity_type = sapply(strsplit(start_activity_type,"_"),"[[",1)) %>% filter(start_activity_type == "home")
closed_home_activitites = closed_trips_csv %>% mutate(start_activity_type = sapply(strsplit(start_activity_type,"_"),"[[",1)) %>% filter(start_activity_type == "home")
opened_living_persons = filterByRegion(opened_home_activitites,shapeTable = shapeTable,crs = 31468,start.inshape = TRUE)
closed_living_persons = filterByRegion(closed_home_activitites,shapeTable = shapeTable,crs = 31468,start.inshape = TRUE)

###all trips of agents within shapefile
opened_allLivingTrips = filter(opened_trips_csv, person %in% opened_living_persons$person)
closed_allLivingTrips = filter(closed_trips_csv, person %in% closed_living_persons$person)

write.csv(opened_allLivingTrips,"opened_allLivingTrips.csv", row.names = FALSE)
write.csv(closed_allLivingTrips,"closed_allLivingTrips.csv", row.names = FALSE)

###overview scenario comparison
opened_in_berlin = filterByRegion(opened_trips_csv,st_read("bezirke"),31468)
closed_in_berlin = filterByRegion(closed_trips_csv,st_read("bezirke"),31468)

anzahl_in_berlin = nrow(distinct(opened_in_berlin %>% select(person)))

opened_distance_berlin = sum(opened_in_berlin$traveled_distance)
opened_distance_zone = sum(opened_allLivingTrips$traveled_distance)
closed_distance_berlin = sum(closed_in_berlin$traveled_distance)
closed_distance_zone = sum(closed_allLivingTrips$traveled_distance)

opened_time_berlin = sum(opened_in_berlin$trav_time)
opened_time_zone = sum(opened_allLivingTrips$trav_time)
closed_time_berlin = sum(closed_in_berlin$trav_time)
closed_time_zone = sum(closed_allLivingTrips$trav_time)

#temp_opened = opened_living_persons %>% mutate(main_mode == uniq_mode)
#main_mode then == uniq_mode

#agent_trips_base = base_output %>% filter(person == 397017401)
#agent_trips_policy = policy_output %>% filter(person == 397017401)
#agent_trips_base %>% select(person,trip_number,dep_time,trav_time,traveled_distance,main_mode,start_activity_type,end_activity_type)
#agent_trips_policy %>% select(person,trip_number,dep_time,trav_time,traveled_distance,main_mode,start_activity_type,end_activity_type)

#living agents
opened_living_uniq = unique_modes(opened_allLivingTrips)
closed_living_uniq = unique_modes(closed_allLivingTrips)



opened_living_uniq = opened_living_uniq %>% mutate(uniq = sapply(uniq,str_sort))
opened_living_uniq = opened_living_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))
opened_living_uniq$main_mode = opened_living_uniq$new_mode

closed_living_uniq = closed_living_uniq %>% mutate(uniq = sapply(uniq,str_sort))
closed_living_uniq = closed_living_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))
closed_living_uniq$main_mode = closed_living_uniq$new_mode




#in berlin agents
opened_in_berlin_uniq = unique_modes(opened_in_berlin)
closed_in_berlin_uniq = unique_modes(closed_in_berlin)


opened_in_berlin_uniq = opened_in_berlin_uniq %>% mutate(uniq = sapply(uniq,str_sort))
opened_in_berlin_uniq = opened_in_berlin_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))
opened_in_berlin_uniq$main_mode = opened_in_berlin_uniq$new_mode

closed_in_berlin_uniq = closed_in_berlin_uniq %>% mutate(uniq = sapply(uniq,str_sort))
closed_in_berlin_uniq = closed_in_berlin_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))
closed_in_berlin_uniq$main_mode = closed_in_berlin_uniq$new_mode