# ShortestPath

This software helps you in finding the shortest path between two routes and also provides the distance and time. It uses the efficient algorithms to calculate the routes quickly using GraphHopper routing library which internally uses OpenStreetMap for the maps.

The roads in the maps are distributed into graphs as separate nodes, which are then passed through Dijkstra's Algorithm, which calculates the shortest route.