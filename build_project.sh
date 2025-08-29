#!/bin/bash
# Build script for Eidolon Unchained

echo "🚀 Starting Eidolon Unchained Build Process..."
echo "📍 Working Directory: $(pwd)"
echo ""

# Check if we're in the right directory
if [ ! -f "build.gradle" ]; then
    echo "❌ Error: build.gradle not found. Please run from project root."
    exit 1
fi

echo "🧹 Cleaning previous build artifacts..."
./gradlew clean

echo ""
echo "🔧 Compiling Java sources..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "📦 Building complete project..."
    ./gradlew build
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "🎉 Build completed successfully!"
        echo "📁 Build artifacts can be found in: build/libs/"
        ls -la build/libs/
    else
        echo "❌ Build failed during resource processing."
        exit 1
    fi
else
    echo "❌ Compilation failed. Check errors above."
    exit 1
fi

echo ""
echo "✅ Build process complete!"
