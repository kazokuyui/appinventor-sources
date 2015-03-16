#!/bin/bash
cd appinventor/
echo "Starting AppEngine..."
dev_appserver.sh --port=8888 --address=0.0.0.0 appengine/build/war
echo "AppEngine started!"
