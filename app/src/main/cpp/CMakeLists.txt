cmake_minimum_required(VERSION 3.22.1)
project(freshjvm C)

add_library(freshjvm SHARED jre_launcher.c)
target_link_libraries(freshjvm log dl)
