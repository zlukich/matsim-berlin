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

opened = readTripsTable("opened")

opened_uniq = unique_modes(opened)

#opened_uniq$uniq = opened_uniq$uniq[opened_uniq$uniq != "walk"]
opened_uniq = opened_uniq %>% mutate(uniq = sapply(uniq,str_sort))

opened_uniq = opened_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))

closed = readTripsTable("closed")

closed_uniq = unique_modes(closed)

#closed_uniq$uniq = closed_uniq$uniq[closed_uniq$uniq != "walk"]
closed_uniq = closed_uniq %>% mutate(uniq = sapply(uniq,str_sort))

closed_uniq = closed_uniq %>% mutate(new_mode = sapply(uniq,paste,collapse = "-"))

######filtering with shape file
shapeTable = st_read("path to shapefile folder")
table = opened %>% mutate(start_activity_type = sapply(strsplit(start_activity_type,"_"),"[[",1)) %>% filter(start_activity_type == "home")
filtered_living_table = filterByRegion(table,shapeTable = shapeTable,crs = 31468,start.inshape = TRUE)
write_csv2(filtered_living_table,"living_agents.csv")