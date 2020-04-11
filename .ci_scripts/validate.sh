#! /usr/bin/env bash

set -e

SCRIPT_DIR=`dirname $0 | sed -e "s|^\./|$PWD/|"`

cd "$SCRIPT_DIR/.."

sbt ++$SCALA_VERSION scalariformFormat test:scalariformFormat
git diff --exit-code || (
  echo "ERROR: Scalariform check failed, see differences above."
  echo "To fix, format your sources using ./build scalariformFormat test:scalariformFormat before submitting a pull request."
  echo "Additionally, please squash your commits (eg, use git commit --amend) if you're going to update this pull request."
  false
)

source "$SCRIPT_DIR/jvmopts.sh"

export JVM_OPTS
export SBT_OPTS

TEST_ARGS=";mimaReportBinaryIssues"

if [ ! `echo "n$SCALA_VERSION" | sed -e 's/2.13.*/o/'` = "no" ]; then
  TEST_ARGS=";scapegoat $TEST_ARGS"
fi

# Check Mongo connection
source /tmp/validate-env.sh
export LD_LIBRARY_PATH

PRIMARY_HOST="localhost:27017"
MONGOSHELL_OPTS="$PRIMARY_HOST/FOO"

MONGOSHELL_OPTS="$MONGOSHELL_OPTS --eval"
MONGODB_NAME=`mongo $MONGOSHELL_OPTS 'db.getName()' 2>/dev/null | tail -n 1`

if [ ! "x$MONGODB_NAME" = "xFOO" ]; then
    echo -n "\nERROR: Fails to connect using the MongoShell\n"
    mongo $MONGOSHELL_OPTS 'db.getName()'
    tail -n 100 /tmp/mongod.log
    exit 2
fi

# Run tests
TEST_ARGS="$TEST_ARGS ;testOnly; doc"

cat > /dev/stdout <<EOF
- JVM options: $JVM_OPTS
- SBT options: $SBT_OPTS
- Test arguments: $TEST_ARGS
EOF

sbt ++$SCALA_VERSION "$TEST_ARGS"
