#!/bin/bash

sed -i~ "/<servers>/ a\
<server>\
  <id>Serene-release</id>\
  <username>${MAVEN_USERNAME}</username>\
  <password>${MAVEN_PASSWORD}</password>\
</server>\
<server>\
  <id>Serene-snapshot</id>\
  <username>${MAVEN_USERNAME}</username>\
  <password>${MAVEN_PASSWORD}</password>\
</server>" /usr/share/maven/conf/settings.xml

sed -i~ "/<servers>/ a\
<server>\
  <id>Serene-release</id>\
  <username>${MAVEN_USERNAME}</username>\
  <password>${MAVEN_PASSWORD}</password>\
</server>\
<server>\
  <id>Serene-snapshot</id>\
  <username>${MAVEN_USERNAME}</username>\
  <password>${MAVEN_PASSWORD}</password>\
</server>" $HOME/.m2/settings.xml