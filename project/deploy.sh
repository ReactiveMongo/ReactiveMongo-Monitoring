#! /bin/bash

set -e

REPO="https://oss.sonatype.org/service/local/staging/deploy/maven2/"

if [ $# -lt 2 ]; then
    echo "Usage $0 version gpg-key"
    exit 1
fi

#echo "Check the project version"
#VERSION=`sbt ';project ReactiveMongo ;show version' 2>&1 | tail -n 1 | cut -d ' ' -f 2 | sed -e 's/^.*([0-9.]*).*$/$1/'`

VERSION="$1"
KEY="$2"

echo "Password: "
read -s PASS

function deploy {
  BASE="$1"
  POM="$BASE.pom"

  expect << EOF
set timeout 300
spawn mvn gpg:sign-and-deploy-file -DuniqueVersion=false -Dkeyname=$KEY -DpomFile=$POM -Dfile=$BASE.jar -Djavadoc=$BASE-javadoc.jar -Dsources=$BASE-sources.jar $ARG -Durl=$REPO -DrepositoryId=sonatype-nexus-staging
log_user 0
expect "GPG Passphrase:"
send "$PASS\r"
log_user 1
expect "BUILD SUCCESS"
expect eof
EOF
}

SCALA_MODULES="jmx:reactivemongo-jmx"
SCALA_VERSIONS="2.11 2.12 2.13"
BASES=""

for V in $SCALA_VERSIONS; do
    for M in $SCALA_MODULES; do
        B=`echo "$M" | cut -d ':' -f 1`
        N=`echo "$M" | cut -d ':' -f 2`

        if [ `echo "$EXCLUDED" | grep "$B/$V" | wc -l` -ne 0 ]; then
            echo "Skip $B @ $V"
        else
            SCALADIR="$B/target/scala-$V/$N"_$V-$VERSION

            BASES="$BASES $SCALADIR"
        fi
    done
done

for B in $BASES; do
  deploy "$B"
done
