#! /bin/bash

SCRIPT_DIR=`dirname $0 | sed -e "s|^\./|$PWD/|"`

# Install MongoDB
MONGO_HOME="$HOME/mongodb-linux-x86_64-amazon-4.2.2"

if [ ! -x "$MONGO_HOME/bin/mongod" ]; then
    if [ -d "$MONGO_HOME" ]; then
      rm -rf "$MONGO_HOME"
    fi

    cd "$HOME"
    curl -s -o - https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-amazon-4.2.2.tgz | tar -xzf -
    chmod u+x "$MONGO_HOME/bin/mongod"
fi

export PATH="$MONGO_HOME/bin:$PATH"

# OpenSSL
if [ ! -L "$HOME/ssl/lib/libssl.so.1.0.0" ] && [ ! -f "$HOME/ssl/lib/libssl.so.1.0.0" ]; then
  echo "[INFO] Building OpenSSL"

  cd /tmp
  curl -s -o - https://www.openssl.org/source/old/1.0.1/openssl-1.0.1u.tar.gz | tar -xzf -
  cd openssl-1.0.1u
  rm -rf "$HOME/ssl" && mkdir "$HOME/ssl"
  ./config -shared enable-ssl2 --prefix="$HOME/ssl" > /dev/null
  make depend > /dev/null
  make install > /dev/null

  ln -s "$HOME/ssl/lib/libssl.so.1.0.0" "$HOME/ssl/lib/libssl.so.10"
  ln -s "$HOME/ssl/lib/libcrypto.so.1.0.0" "$HOME/ssl/lib/libcrypto.so.10"
fi

export LD_LIBRARY_PATH="$HOME/ssl/lib:$LD_LIBRARY_PATH"
export LD_LIBRARY_PATH

# MongoDB configuration
MONGO_CONF="$SCRIPT_DIR/mongod.conf"

mkdir /tmp/mongodb
cp "$MONGO_CONF" /tmp/mongod.conf

MAX_CON=`ulimit -n`

if [ $MAX_CON -gt 1024 ]; then
    MAX_CON=`expr $MAX_CON - 1024`
fi

echo "  maxIncomingConnections: $MAX_CON" >> /tmp/mongod.conf

echo "# Configuration:"
cat /tmp/mongod.conf

# MongoDB startup

cat > /tmp/validate-env.sh <<EOF
PATH="$PATH"
LD_LIBRARY_PATH="$LD_LIBRARY_PATH"
EOF
