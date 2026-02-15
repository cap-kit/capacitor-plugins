const fs = require('fs');
const path = require('path');

// SDK Metadata
const ANDROID_VERSION = '2.4.5';
const ANDROID_GROUP_ID = 'com.redsys.tpvvinapplibrary';
const ANDROID_ARTIFACT_ID = 'redsys-sdk-inApp';
const ANDROID_FILENAME = `${ANDROID_ARTIFACT_ID}-${ANDROID_VERSION}.aar`;
const IOS_FILENAME = 'TPVVInLibrary.xcframework';

// Source Path (Root of the App Host)
const SDKS_SOURCE_DIR = path.join(process.cwd(), 'sdks');

// Target Paths (Native Folders)
const ANDROID_TARGET_BASE = path.join(process.cwd(), 'android/app/libs');
const ANDROID_VERSION_PATH = path.join(ANDROID_TARGET_BASE, ...ANDROID_GROUP_ID.split('.'), ANDROID_ARTIFACT_ID, ANDROID_VERSION);
const ANDROID_METADATA_PATH = path.join(ANDROID_TARGET_BASE, ...ANDROID_GROUP_ID.split('.'), ANDROID_ARTIFACT_ID);

const IOS_TARGET_PATH = path.join(process.cwd(), 'ios/App', IOS_FILENAME);

async function setup() {
  console.log('⚡️ CapKit Redsys SDK Orchestrator Setup\n');

  // 1. Check if source folder exists
  if (!fs.existsSync(SDKS_SOURCE_DIR)) {
    console.error(`❌ Error: 'sdks/' directory not found in project root.`);
    console.log(`Please create it and place your binaries inside.`);
    process.exit(1);
  }

  let androidSuccess = false;
  let iosSuccess = false;

  // --- ANDROID SETUP ---
  const androidSource = path.join(SDKS_SOURCE_DIR, ANDROID_FILENAME);
  if (fs.existsSync(androidSource)) {
    fs.mkdirSync(ANDROID_VERSION_PATH, { recursive: true });
    
    // Copy AAR
    fs.copyFileSync(androidSource, path.join(ANDROID_VERSION_PATH, ANDROID_FILENAME));
    
    // Generate POM
    const pom = `<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>${ANDROID_GROUP_ID}</groupId>
  <artifactId>${ANDROID_ARTIFACT_ID}</artifactId>
  <version>${ANDROID_VERSION}</version>
  <packaging>aar</packaging>
</project>`;
    fs.writeFileSync(path.join(ANDROID_VERSION_PATH, `${ANDROID_ARTIFACT_ID}-${ANDROID_VERSION}.pom`), pom);
    
    // Generate Metadata
    const metadata = `<?xml version="1.0" encoding="UTF-8"?>
<metadata><groupId>${ANDROID_GROUP_ID}</groupId><artifactId>${ANDROID_ARTIFACT_ID}</artifactId>
<versioning><release>${ANDROID_VERSION}</release><versions><version>${ANDROID_VERSION}</version></versions>
<lastUpdated>${new Date().toISOString().replace(/[-:T]/g, '').split('.')[0]}</lastUpdated></versioning></metadata>`;
    fs.writeFileSync(path.join(ANDROID_METADATA_PATH, 'maven-metadata.xml'), metadata);
    androidSuccess = true;
  }

  // --- iOS SETUP ---
  const iosSource = path.join(SDKS_SOURCE_DIR, IOS_FILENAME);
  if (fs.existsSync(iosSource)) {
    if (fs.existsSync(IOS_TARGET_PATH)) {
      fs.rmSync(IOS_TARGET_PATH, { recursive: true, force: true });
    }
    copyFolderRecursiveSync(iosSource, path.dirname(IOS_TARGET_PATH));
    iosSuccess = true;
  }

  printVisualTree(androidSuccess, iosSuccess);
}

function copyFolderRecursiveSync(source, target) {
  const targetFolder = path.join(target, path.basename(source));
  if (!fs.existsSync(targetFolder)) fs.mkdirSync(targetFolder, { recursive: true });
  if (fs.lstatSync(source).isDirectory()) {
    fs.readdirSync(source).forEach((file) => {
      const curSource = path.join(source, file);
      if (fs.lstatSync(curSource).isDirectory()) {
        copyFolderRecursiveSync(curSource, targetFolder);
      } else {
        fs.copyFileSync(curSource, path.join(targetFolder, file));
      }
    });
  }
}

function printVisualTree(androidOk, iosOk) {
  const appName = path.basename(process.cwd());
  console.log('\n✅ Redsys SDKs distribution completed!');
  console.log('\nProject Structure:');
  console.log('------------------');
  console.log(`${appName}/`);
  console.log('├── android/');
  console.log('│   ├── build.gradle');
  console.log('│   └── app/');
  console.log('│       ├── build.gradle');
  console.log(`│       └── libs/  ${androidOk ? '✅' : '❌'}`);
  console.log(`│           └── com/redsys/tpvvinapplibrary/${ANDROID_ARTIFACT_ID}/`);
  console.log('│               ├── maven-metadata.xml');
  console.log(`│               └── ${ANDROID_VERSION}/`);
  console.log(`│                   ├── ${ANDROID_FILENAME}`);
  console.log(`│                   └── ${ANDROID_ARTIFACT_ID}-${ANDROID_VERSION}.pom`);
  console.log('├── ios/');
  console.log('│   └── App/');
  console.log('│       ├── build.gradle');
  console.log(`│       └── ${IOS_FILENAME}  ${iosOk ? '✅' : '❌'}`);
  console.log('------------------');
  
  console.log('\n🚀 Next Steps:');
  if (androidOk) {
    console.log('👉 Android: Ensure "maven { url "${rootProject.projectDir}/app/libs" }" is in your root build.gradle.');
    console.log(`👉 Android: Ensure "implementation '${ANDROID_GROUP_ID}:${ANDROID_ARTIFACT_ID}:${ANDROID_VERSION}'" is in your app/build.gradle.`);
  }
  if (iosOk) {
    console.log('👉 iOS: In Xcode, drag the .xcframework into "Frameworks, Libraries, and Embedded Content" and set to "Embed & Sign".');
  }
}

setup();