#!/bin/bash

# Thrifty
#
# Copyright (c) Microsoft Corporation
#
# All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the License);
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# THIS CODE IS PROVIDED ON AN #AS IS* BASIS, WITHOUT WARRANTIES OR
# CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
# WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
# FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
#
# See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.

if [ "$CI" != "true" ]
then
  echo "Not a CI build"
  exit 1
fi

# Travis is sensitive to casing in repo names, but github
# is rather loose.  The capital 'M' in Microsoft needs to
# be handled carefully.
if ! [[ $TRAVIS_REPO_SLUG =~ [Mm]icrosoft/thrifty ]]
then
  echo "Wrong repo"
  exit 1
fi

if [ "$TRAVIS_BRANCH" != "master" ]
then
  echo "Wrong branch"
  exit 1
fi

if [ "$TRAVIS_PULL_REQUEST" != "false" ]
then
  echo "Pull request"
  exit 1
fi

if [ ! -z $(awk '/^VERSION=/ && !/SNAPSHOT$/' gradle.properties) ]
then
  echo "Not a snapshot"
  exit 1
fi

if ! [[ $(javac -version 2>&1) =~ 1.8 ]]
then
  echo "Wrong JDK"
  exit 1
fi

./gradlew uploadArchives

