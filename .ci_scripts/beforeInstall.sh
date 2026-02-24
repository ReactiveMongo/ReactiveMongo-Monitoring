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
SSL_MAJOR="1.0.0"
SSL_SUFFIX="10"
SSL_RELEASE="1.0.2"
SSL_FULL_RELEASE="1.0.2u"
SSL_GH_TAG="OpenSSL_1_0_2u"
SSL_DL_URL="https://github.com/openssl/openssl/releases/download/${SSL_GH_TAG}/openssl-${SSL_FULL_RELEASE}.tar.gz"
SSL_HOME="$HOME/ssl"
SSL_LIB="$SSL_HOME/lib"

if [ ! -f "$SSL_LIB/libssl.so.$SSL_MAJOR" ] || [ ! -f "$SSL_LIB/libcrypto.so.$SSL_MAJOR" ]; then
  echo "[INFO] Building OpenSSL $SSL_MAJOR ..."

  cd /tmp

  echo "[INFO] Downloading OpenSSL from $SSL_DL_URL ..."
  curl -fL -s -o - "$SSL_DL_URL" | tar -xzf -

  cd "openssl-${SSL_FULL_RELEASE}"
  rm -rf "$SSL_HOME" && mkdir "$SSL_HOME"

  echo "[INFO] Configuring OpenSSL build ..."
  ./config -shared enable-ssl2 --prefix="$SSL_HOME" > /dev/null

  echo "[INFO] Resolving dependencies for OpenSSL build ..."
  make depend > /dev/null

  echo "[INFO] Building and installing OpenSSL ..."
  make install > /dev/null
fi

if [ ! -d "$SSL_LIB" ] || [ ! -f "$SSL_LIB/libssl.so.$SSL_MAJOR" ] || [ ! -f "$SSL_LIB/libcrypto.so.$SSL_MAJOR" ]; then
  echo "[ERROR] OpenSSL libraries are missing in $SSL_LIB"
  exit 1
fi

ln -sf "$SSL_LIB/libssl.so.$SSL_MAJOR" "$SSL_LIB/libssl.so.$SSL_SUFFIX"
ln -sf "$SSL_LIB/libcrypto.so.$SSL_MAJOR" "$SSL_LIB/libcrypto.so.$SSL_SUFFIX"

export PATH="$SSL_HOME/bin:$PATH"
export LD_LIBRARY_PATH="$SSL_LIB:${LD_LIBRARY_PATH:-}"

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
