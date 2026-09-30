// ============================================================
// MILLENNIUM POS — préparation du projet Android (après « cap add android »)
// - installe le plugin Bluetooth classique et la MainActivity
// - ajoute les autorisations Bluetooth au manifeste
// - numéro de version automatique (numéro de build GitHub)
// - signature de l'APK si la clé est fournie
// ============================================================
import { copyFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const android = join(root, 'android');
if (!existsSync(android)) {
  console.error('Dossier android introuvable : lancez d\'abord « npx cap add android ».');
  process.exit(1);
}

// 1. Code natif
const javaDir = join(android, 'app/src/main/java/ht/milleniumgroup/pos');
mkdirSync(javaDir, { recursive: true });
copyFileSync(join(root, 'native/MainActivity.java'), join(javaDir, 'MainActivity.java'));
copyFileSync(join(root, 'native/BluetoothPrinterPlugin.java'), join(javaDir, 'BluetoothPrinterPlugin.java'));
console.log('✔ Plugin Bluetooth installé');

// 2. Autorisations
const manifestPath = join(android, 'app/src/main/AndroidManifest.xml');
let manifest = readFileSync(manifestPath, 'utf8');
const perms = [
  '<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />',
  '<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />',
  '<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />',
  '<uses-feature android:name="android.hardware.bluetooth" android:required="false" />',
];
const missing = perms.filter((p) => !manifest.includes(p.split('"')[1]));
if (missing.length) {
  manifest = manifest.replace('<application', `${missing.join('\n    ')}\n\n    <application`);
  writeFileSync(manifestPath, manifest);
}
console.log('✔ Autorisations Bluetooth ajoutées');

// 3. Version + signature
const gradlePath = join(android, 'app/build.gradle');
let gradle = readFileSync(gradlePath, 'utf8');
const build = Number(process.env.BUILD_NUMBER || 1);
gradle = gradle.replace(/versionCode\s+\d+/, `versionCode ${build}`);
gradle = gradle.replace(/versionName\s+"[^"]*"/, `versionName "1.0.${build}"`);

if (process.env.KEYSTORE_PATH && !gradle.includes('millenniumRelease')) {
  gradle += `

// --- Signature Millennium POS (ajoutée automatiquement) ---
android {
    signingConfigs {
        millenniumRelease {
            storeFile file(System.getenv("KEYSTORE_PATH"))
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS") ?: "millennium"
            keyPassword System.getenv("KEYSTORE_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.millenniumRelease
        }
    }
}
`;
  console.log('✔ Signature configurée');
} else if (!process.env.KEYSTORE_PATH) {
  console.log('⚠ Pas de clé de signature : un APK de test (debug) sera construit.');
}
writeFileSync(gradlePath, gradle);
console.log(`✔ Version 1.0.${build}`);
