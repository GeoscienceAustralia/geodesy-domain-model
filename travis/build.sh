#!/usr/bin/env nix-shell
#!nix-shell ../shell.nix -i bash

set -e

# A local installation of maven prefers to run the global installation, if available.
sudo rm -f /etc/mavenrc

# We redirect maven test output to file, because Travis CI limits stdout log size to 4MB.

mvn --settings ./travis/maven-settings.xml -U install -DskipTests > /dev/null

if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
    mvn --settings ./travis/maven-settings.xml deploy -pl '!gws-system-test' -DredirectTestOutputToFile
    mvn --settings ./travis/maven-settings.xml deploy -pl gws-system-test -DskipTests
    mvn --settings ./travis/maven-settings.xml site -DskipTests -pl gws-core
    cp ./gws-webapp/target/geodesy-web-services.war ./gws-system-test/target/gws-system-test.jar ./aws/codedeploy-WebServices/
    aws configure set aws_access_key_id "${TRAVIS_AWS_ACCESS_KEY_ID}" --profile geodesy
    aws configure set aws_secret_access_key "${TRAVIS_AWS_SECRET_KEY_ID}" --profile geodesy
    aws configure set region ap-southeast-2 --profile geodesy
    aws configure set output json --profile geodesy

    aws configure set aws_access_key_id "${AWS_ACCESS_KEY_ID_GNSS_METADATA}" --profile gnss-metadata
    aws configure set aws_secret_access_key "${AWS_SECRET_ACCESS_KEY_GNSS_METADATA}" --profile gnss-metadata
    aws configure set region ${AWS_DEFAULT_REGION} --profile gnss-metadata
    aws configure set output json --profile gnss-metadata

    # Default TMPDIR is in /run, which is in memory, so we change it to disk to
    # avoid running out of space. `aws codedeploy push` writes the codedeploy
    # zip bundle to $TMPDIR.

    # TODO: Temporarily disable automatic deployments.
    export TMPDIR=/tmp
    case "${TRAVIS_BRANCH}" in
        "release-0.2.0")
            ./aws/deploy.sh test
            ./aws/codedeploy-WebServices/deploy.sh test
        ;;
        "master")
            ./aws/deploy.sh dev
            mvn --settings ./travis/maven-settings.xml site-deploy -DskipTests -pl gws-core
            ./aws/codedeploy-WebServices/deploy.sh dev
            ./aws/codedeploy-GeoServer/deploy.sh dev
        ;;
    esac
else
    mvn --settings ./travis/maven-settings.xml verify -pl '!gws-system-test' -DredirectTestOutputToFile
    ./aws/deploy.sh dev --dry-run
fi
