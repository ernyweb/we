cmake_minimum_required(VERSION 3.18.1)

project("audioprocessor")

add_library(audioprocessor SHARED
    audioprocessor.cpp)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(audioprocessor
    ${log-lib}
    ${android-lib})
