#!/bin/sh
find . -name *.java | xargs wc -l | sort -n
