#!/bin/bash
# Bu betik, PTP4L Configuration Tool projesini macOS için derler ve paketler.

echo "Building and packaging PTP4L Configuration Tool for macOS..."

# Maven'ı çalıştır. Tüm derleme ve jpackage mantığı pom.xml içindedir.
mvn clean package

# Build'in başarılı olup olmadığını kontrol et
if [ $? -ne 0 ]; then
    echo "Build failed! Check Maven output for errors."
    exit 1
fi

echo ""
echo "✅ macOS application created successfully!"
echo "Location: target/dist/"
echo ""