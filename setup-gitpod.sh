#!/bin/bash
set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

log() { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }
header() { echo -e "${PURPLE}$1${NC}"; }

clear
header "🚀 FLIPKART AUTOMATION - AUTO BUILD SYSTEM"
header "============================================="
echo ""

start_time=$(date +%s)

# Step 1: System Setup
header "📋 STEP 1: System Preparation"
log "Installing build tools..."
sudo apt-get update -qq
sudo apt-get install -y -qq openjdk-11-jdk wget unzip git curl

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

success "✅ System ready"

# Step 2: Android SDK
header "📱 STEP 2: Android SDK Setup"
ANDROID_HOME="$HOME/android-sdk"
mkdir -p "$ANDROID_HOME"

cd "$HOME"
SDK_ZIP="commandlinetools-linux-8512546_latest.zip"

if [ ! -f "$SDK_ZIP" ]; then
    log "Downloading Android SDK (50MB)..."
    wget -q --show-progress "https://dl.google.com/android/repository/$SDK_ZIP"
fi

if [ ! -d "$ANDROID_HOME/cmdline-tools" ]; then
    log "Setting up Android SDK..."
    unzip -q "$SDK_ZIP"
    mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
    mv cmdline-tools/* "$ANDROID_HOME/cmdline-tools/latest/"
    rm -rf cmdline-tools
fi

export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

log "Installing Android components..."
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null 2>&1

success "✅ Android SDK configured"

# Step 3: Project Setup
header "📂 STEP 3: Project Configuration"
cd "$GITPOD_REPO_ROOT" 2>/dev/null || cd /workspace

log "Configuring build environment..."

# Create local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Download Gradle wrapper if missing
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    log "Downloading Gradle wrapper..."
    mkdir -p gradle/wrapper
    cd gradle/wrapper
    wget -q "https://services.gradle.org/distributions/gradle-8.0-bin.zip" -O gradle.zip
    unzip -q gradle.zip
    cp gradle-8.0/lib/gradle-launcher-8.0.jar gradle-wrapper.jar
    rm -rf gradle-8.0 gradle.zip
    cd ../..
fi

# Make gradlew executable
chmod +x gradlew 2>/dev/null || {
    log "Creating Gradle wrapper..."
    gradle wrapper --gradle-version=8.0
    chmod +x gradlew
}

success "✅ Project configured"

# Step 4: Build APK
header "🔨 STEP 4: Building APK"
log "Starting Android build (this may take 5-10 minutes)..."

# Gradle performance settings
export GRADLE_OPTS="-Xmx3g -XX:+UseParallelGC -XX:MaxMetaspaceSize=512m"

# Clean and build
./gradlew clean >/dev/null 2>&1 || warning "Clean step had issues (continuing...)"

log "🏗️ Compiling release APK..."
if ./gradlew assembleRelease --no-daemon --quiet; then
    success "🎉 BUILD SUCCESSFUL!"
    
    # Find and prepare APK
    APK_PATH=$(find . -name "*.apk" -path "*/release/*" | head -1)
    if [ -n "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        cp "$APK_PATH" "./FlipkartAutomation.apk"
        
        # Build completion info
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        
        header "🎯 BUILD COMPLETED SUCCESSFULLY!"
        echo ""
        success "📱 APK Ready: FlipkartAutomation.apk"
        success "📊 Size: $APK_SIZE"
        success "⏱️ Build Time: ${duration} seconds"
        echo ""
        header "📋 NEXT STEPS:"
        echo "1. 📁 Find 'FlipkartAutomation.apk' in file explorer (left panel)"
        echo "2. 🖱️ Right-click → Download"
        echo "3. 📲 Install on Android device"
        echo "4. ⚙️ Enable in Settings > Accessibility > Flipkart Automation"
        echo ""
        warning "⚠️ Use responsibly - Follow Flipkart's Terms of Service!"
        
        # Create install instructions
        cat > INSTALL_INSTRUCTIONS.txt << 'EOF'
📱 INSTALLATION GUIDE
===================

1. DOWNLOAD APK:
   - Right-click "FlipkartAutomation.apk" in Gitpod file explorer
   - Select "Download"

2. INSTALL ON PHONE:
   - Transfer APK to your Android phone
   - Open APK file (allow "Unknown sources" if prompted)
   - Tap "Install"

3. ENABLE SERVICE:
   - Go to Settings > Accessibility
   - Find "Flipkart Automation"
   - Toggle ON

4. CONFIGURE:
   - Open Flipkart app
   - The automation will activate automatically

⚠️ IMPORTANT:
- Use only for legitimate purposes
- Follow Flipkart's Terms of Service
- Be aware of legal implications

🔧 TROUBLESHOOTING:
- If app doesn't install: Enable "Install from unknown sources"
- If service not working: Check accessibility permissions
- For errors: Check Android device logs
EOF
        
        success "📄 Instructions saved: INSTALL_INSTRUCTIONS.txt"
        
    else
        error "APK file not found after build"
        exit 1
    fi
else
    error "❌ BUILD FAILED!"
    echo "Debug info:"
    ./gradlew assembleRelease --stacktrace
    exit 1
fi

header "✅ AUTOMATION BUILD COMPLETE!"
