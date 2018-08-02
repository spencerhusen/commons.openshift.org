#!/bin/bash
cd $COMMONS_PATH/resizerprogram/src
curl -H "Authorization: token $GIT_TOKEN" https://api.github.com/repos/spencerhusen/commons.openshift.org/issues | java -cp .:commons/commons-io-2.6.jar commons.ImageResizer
