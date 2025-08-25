#!/bin/bash
# Bu betik, PTP4L Configuration Tool projesini macOS için build eder.

echo "Building PTP4L Configuration Tool for macOS..."

# Önceki build'leri temizle
echo "Cleaning previous builds..."
rm -rf target
rm -rf dist

# Maven ile derle (Bu komut platformdan bağımsızdır)
echo "Compiling with Maven..."
mvn clean package

# Build'in başarılı olup olmadığını kontrol et
if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo ""
echo "JAR file created successfully at: target/ptpapp-1.0.0.jar"
echo ""

# jpackage ile macOS uygulaması (.dmg) oluştur
echo "Creating macOS application with jpackage..."

# Kullanıcının Maven depo yolunu al ($HOME, macOS'taki kullanıcı klasörüdür)
MAVEN_REPO="$HOME/.m2/repository"

# JavaFX modüllerinin varlığını kontrol et
if [ ! -d "$MAVEN_REPO/org/openjfx/javafx-controls" ]; then
    echo "JavaFX modules not found in Maven repository. Installing dependencies..."
    mvn dependency:copy-dependencies -DoutputDirectory=target/lib
    if [ $? -ne 0 ]; then
        echo "Failed to copy dependencies!"
        exit 1
    fi
    
    # İndirilen bağımlılıkları kullan
    jpackage --input target \
             --name PTP4L-Config-Tool \
             --main-jar ptpapp-1.0.0.jar \
             --main-class com.ptpapp.Main \
             --type dmg \
             --dest dist \
             --module-path "target/lib" \
             --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base
else
    # Maven deposundaki modülleri kullan (yol ayırıcısı olarak ':' kullanıldığına dikkat edin)
    jpackage --input target \
             --name PTP4L-Config-Tool \
             --main-jar ptpapp-1.0.0.jar \
             --main-class com.ptpapp.Main \
             --type dmg \
             --dest dist \
             --module-path "$MAVEN_REPO/org/openjfx/javafx-controls/17.0.2:$MAVEN_REPO/org/openjfx/javafx-fxml/17.0.2:$MAVEN_REPO/org/openjfx/javafx-graphics/17.0.2:$MAVEN_REPO/org/openjfx/javafx-base/17.0.2" \
             --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base
fi

# jpackage'in başarılı olup olmadığını kontrol et
if [ $? -ne 0 ]; then
    echo "jpackage failed!"
    echo "Check if JavaFX modules are available." [cite: 4]
    exit 1
fi

echo ""
echo "✅ DMG created successfully!"
echo "Location: dist/PTP4L-Config-Tool-1.0.dmg"
echo ""
echo "To install: Open the DMG file and drag the application to your Applications folder."
echo ""