#!/bin/bash
cd appinventor/buildserver/
echo "Starting BuildServer..."
ant RunLocalBuildServer
echo "BuildServer started!"
echo "Visit @ localhost:8888."
