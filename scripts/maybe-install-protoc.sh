#!/bin/bash
echo start to install protoc
if  test -x  scripts/protoc; then
    echo build is already done.
    exit 0
else
    echo continue
fi

wget https://github.com/google/protobuf/releases/download/v3.1.0/protobuf-cpp-3.1.0.tar.gz
