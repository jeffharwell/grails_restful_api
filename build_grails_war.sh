#!/bin/bash

GRAILS_REPO="GrailsRestfulAPIService"
GRAILS_NAME="apiservice"

. ~/.profile
cd ~/$GRAILS_REPO/$GRAILS_NAME
grails war
