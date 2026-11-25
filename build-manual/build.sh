#!/bin/bash
# Manual (no-Gradle) build for SONARA v1.10.17 (batch 3: widget home
# screen + reaksi di Jam; basis v1.10.16, tanpa perubahan batch 1/2,
# Jam-sync/RTDB/Firestore) — pola
# TRITUNE v2.1 / Widget Pet:
# aapt2 compile/link -> javac R.java -> kotlinc (K2JVMCompiler + compose
# plugin) -> jar -> d8 -> zipalign -> apksigner.
# Gradle TIDAK dipakai (daemon loopback bermasalah di sandbox); semua
# dependensi diambil dari Gradle cache + AAR yang diunduh via curl.
# Mesin Go MixMusic (.aar) dipakai apa adanya dari
# ~/workspace/tritune-go/mixmusic.aar — JANGAN rebuild/modifikasi.
# v1.5 ARUS (identitas dari prototype yang disetujui user): tema terang
# kertas hangat #FFF8EC + oranye bakar, Garis Arus (gelombang anyaman
# hijau/merah), daftar rapat 72dp, segmen 46dp, poster editorial,
# mini player piringan, detail lagu full-screen + tab UP NEXT/LYRICS/
# RELATED + sheet menu, animasi penuh (lihat DESIGN-SPEC.md §5).
set -euo pipefail

JDK=~/workspace/tools/jdk-25.0.5+8
SDK=~/workspace/android-sdk
BT=$SDK/build-tools/36.0.0
ANDROID_JAR=$SDK/platforms/android-35/android.jar
PROJ=~/workspace/mixtune-app
CACHE=~/workspace/gradle-home/caches/modules-2/files-2.1
DL=$PROJ/build-manual/aar-downloads
W=$PROJ/build-manual
SRC=$PROJ/app/src/main
export PATH=$JDK/bin:$PATH
export JAVA_HOME=$JDK
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"

AAPT2=$BT/aapt2
ZIPALIGN=$BT/zipalign

# ---------------------------------------------------------------- clean
# PELAJARAN Widget Pet v2.5: build script TIDAK PERNAH membersihkan
# $W/classes -> .class basi ikut ke APK -> NoSuchMethodError di HP.
# Setiap build mulai dari intermediate tree KOSONG. Jangan hapus blok ini.
rm -rf "$W/classes" "$W/res-compiled" "$W/dex" "$W/gen" "$W/services" \
  "$W/app.ap_" "$W/app-classes.jar" "$W/aligned.apk" "$W/unsigned.apk" \
  "$W/native-libs"

mkdir -p "$W/libs" "$W/res-compiled" "$W/gen" "$W/classes" "$W/dex"

# ---------------------------------------------------------------- artifacts
# AAR dari Gradle cache (pola Widget Pet): group/artifact/version
AARS=(
  androidx.activity/activity-compose/1.9.2
  androidx.activity/activity-ktx/1.9.2
  androidx.activity/activity/1.9.2
  androidx.annotation/annotation-experimental/1.4.1
  androidx.appcompat/appcompat-resources/1.7.0
  androidx.appcompat/appcompat/1.7.0
  androidx.arch.core/core-runtime/2.2.0
  androidx.autofill/autofill/1.0.0
  androidx.compose.animation/animation-android/1.7.0
  androidx.compose.animation/animation-core-android/1.7.0
  androidx.compose.foundation/foundation-android/1.7.0
  androidx.compose.foundation/foundation-layout-android/1.7.0
  androidx.compose.material/material-android/1.7.0
  androidx.compose.material/material-icons-core-android/1.7.0
  androidx.compose.material/material-icons-extended-android/1.7.0
  androidx.compose.material/material-ripple-android/1.7.0
  androidx.compose.material3/material3-android/1.3.0
  androidx.compose.runtime/runtime-android/1.7.0
  androidx.compose.runtime/runtime-saveable-android/1.7.0
  androidx.compose.ui/ui-android/1.7.0
  androidx.compose.ui/ui-geometry-android/1.7.0
  androidx.compose.ui/ui-graphics-android/1.7.0
  androidx.compose.ui/ui-text-android/1.7.0
  androidx.compose.ui/ui-unit-android/1.7.0
  androidx.compose.ui/ui-util-android/1.7.0
  androidx.core/core-ktx/1.13.1
  androidx.core/core/1.13.1
  androidx.cursoradapter/cursoradapter/1.0.0
  androidx.customview/customview-poolingcontainer/1.0.0
  androidx.customview/customview/1.0.0
  androidx.drawerlayout/drawerlayout/1.0.0
  androidx.emoji2/emoji2-views-helper/1.3.0
  androidx.emoji2/emoji2/1.3.0
  androidx.exifinterface/exifinterface/1.3.7
  androidx.graphics/graphics-path/1.0.1
  androidx.interpolator/interpolator/1.0.0
  androidx.lifecycle/lifecycle-livedata-core-ktx/2.8.5
  androidx.lifecycle/lifecycle-livedata-core/2.8.5
  androidx.lifecycle/lifecycle-livedata/2.8.5
  androidx.lifecycle/lifecycle-process/2.8.5
  androidx.lifecycle/lifecycle-runtime-android/2.8.5
  androidx.lifecycle/lifecycle-runtime-compose-android/2.8.5
  androidx.lifecycle/lifecycle-runtime-ktx-android/2.8.5
  androidx.lifecycle/lifecycle-viewmodel-android/2.8.5
  androidx.lifecycle/lifecycle-viewmodel-compose-android/2.8.5
  androidx.lifecycle/lifecycle-viewmodel-ktx/2.8.5
  androidx.lifecycle/lifecycle-viewmodel-savedstate/2.8.5
  androidx.profileinstaller/profileinstaller/1.3.1
  androidx.savedstate/savedstate-ktx/1.2.1
  androidx.savedstate/savedstate/1.2.1
  androidx.startup/startup-runtime/1.1.1
  androidx.tracing/tracing-ktx/1.2.0
  androidx.tracing/tracing/1.2.0
  androidx.vectordrawable/vectordrawable-animated/1.1.0
  androidx.vectordrawable/vectordrawable/1.1.0
  androidx.versionedparcelable/versionedparcelable/1.1.1
  androidx.viewpager/viewpager/1.0.0
  io.coil-kt/coil-base/2.7.0
  io.coil-kt/coil-compose-base/2.7.0
  io.coil-kt/coil-compose/2.7.0
  io.coil-kt/coil/2.7.0
)
# AAR yang diunduh via curl (tidak ada di cache): nama file di $DL
DL_AARS=(
  media3-common-1.4.1.aar
  media3-container-1.4.1.aar
  media3-database-1.4.1.aar
  media3-datasource-1.4.1.aar
  media3-decoder-1.4.1.aar
  media3-exoplayer-1.4.1.aar
  media3-extractor-1.4.1.aar
  browser-1.8.0.aar
  mixmusic.aar
  # v1.10: Google Sign-In (Play Services Auth 21.2.0 + closure
  # transitifnya) + fragment/loader versi yang diminta POM-nya.
  # Diunduh dari maven.google.com — closure diverifikasi dengan
  # jdeps (tidak ada referensi di luar closure + libs yang ada).
  play-services-auth-21.2.0.aar
  play-services-auth-api-phone-18.0.2.aar
  play-services-auth-base-18.0.10.aar
  play-services-base-18.3.0.aar
  play-services-basement-18.3.0.aar
  play-services-fido-20.0.1.aar
  play-services-tasks-18.1.0.aar
  fragment-1.5.7.aar
  loader-1.1.0.aar
)
# Plain jars: group/artifact/version
# NOTE: listenablefuture DIHAPUS dari daftar Widget Pet — diganti guava
# lengkap (guava sudah berisi ListenableFuture; dua-duanya = kelas duplikat).
JARS=(
  androidx.annotation/annotation-jvm/1.8.1
  androidx.arch.core/core-common/2.2.0
  androidx.collection/collection-jvm/1.4.2
  androidx.collection/collection-ktx/1.4.2
  androidx.concurrent/concurrent-futures-ktx/1.1.0
  androidx.concurrent/concurrent-futures/1.1.0
  androidx.lifecycle/lifecycle-common-java8/2.8.5
  androidx.lifecycle/lifecycle-common-jvm/2.8.5
  androidx.resourceinspection/resourceinspection-annotation/1.0.1
  com.google.errorprone/error_prone_annotations/2.36.0
  com.google.guava/failureaccess/1.0.2
  com.google.guava/guava/33.4.0-jre
  com.google.code.findbugs/jsr305/3.0.2
  com.squareup.okhttp3/okhttp/4.12.0
  com.squareup.okio/okio-jvm/3.9.0
  org.jetbrains/annotations/23.0.0
  org.jetbrains.kotlin/kotlin-stdlib/2.2.10
  org.jetbrains.kotlin/kotlin-stdlib-jdk7/2.2.10
  org.jetbrains.kotlin/kotlin-stdlib-jdk8/2.2.10
  org.jetbrains.kotlinx/kotlinx-coroutines-android/1.8.1
  org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.1
)

KOTLIN_COMPILER=$(find "$CACHE/org.jetbrains.kotlin/kotlin-compiler-embeddable/2.2.10" -name '*.jar' | head -1)
KOTLIN_STDLIB=$(find "$CACHE/org.jetbrains.kotlin/kotlin-stdlib/2.2.10" -name 'kotlin-stdlib-2.2.10.jar' | head -1)
COROUTINES_CORE=$(find "$CACHE/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.1" -name '*.jar' | head -1)
ANNOTATIONS_JAR=$(find "$CACHE/org.jetbrains/annotations/23.0.0" -name '*.jar' | head -1)
COMPOSE_PLUGIN=$(find "$CACHE/org.jetbrains.kotlin/kotlin-compose-compiler-plugin-embeddable/2.2.10" -name '*.jar' | head -1)

echo "== resolving artifacts =="
rm -rf "$W/aar" "$W/libs" && mkdir -p "$W/aar" "$W/libs"
declare -a LIB_JARS=()
i=0
for spec in "${AARS[@]}"; do
  f=$(find "$CACHE/$spec" -name '*.aar')
  n=$(echo "$f" | grep -c . || true)
  [ "$n" = "1" ] || { echo "ERROR: $spec -> $n matches: $f"; exit 1; }
  name=$(echo "$spec" | awk -F/ '{print $(NF-1)}' | tr -cd 'a-zA-Z0-9._-')
  d="$W/aar/lib$i-$name"
  mkdir -p "$d"
  unzip -o -q "$f" -d "$d"
  [ -f "$d/classes.jar" ] || { echo "ERROR: no classes.jar in $spec"; exit 1; }
  cp "$d/classes.jar" "$W/libs/lib$i-$name.jar"
  LIB_JARS+=("$W/libs/lib$i-$name.jar")
  echo "AAR ok: $spec"
  i=$((i+1))
done
for aar in "${DL_AARS[@]}"; do
  f="$DL/$aar"
  [ -f "$f" ] || { echo "ERROR: downloaded AAR missing: $f"; exit 1; }
  name=$(basename "$aar" .aar | tr -cd 'a-zA-Z0-9._-')
  d="$W/aar/dl-$name"
  mkdir -p "$d"
  unzip -o -q "$f" -d "$d"
  [ -f "$d/classes.jar" ] || { echo "ERROR: no classes.jar in $aar"; exit 1; }
  cp "$d/classes.jar" "$W/libs/dl-$name.jar"
  LIB_JARS+=("$W/libs/dl-$name.jar")
  echo "AAR ok (downloaded): $aar"
  i=$((i+1))
done
for spec in "${JARS[@]}"; do
  f=$(find "$CACHE/$spec" -name '*.jar')
  n=$(echo "$f" | grep -c . || true)
  [ "$n" = "1" ] || { echo "ERROR: $spec -> $n matches: $f"; exit 1; }
  name=$(echo "$spec" | awk -F/ '{print $(NF-1)}' | tr -cd 'a-zA-Z0-9._-')
  cp "$f" "$W/libs/jar-$name.jar"
  LIB_JARS+=("$W/libs/jar-$name.jar")
  echo "JAR ok: $spec"
done
echo "total dep jars: ${#LIB_JARS[@]}"

# Slimming v1.6: material-icons-extended (37MB, 11.105 kelas) hanya
# dipakai segelintir ikon. Filter jar-nya ke kelas ikon yang benar-benar
# dirujuk source Kotlin; sisanya dibuang. Jaring pengaman: setiap nama
# ikon yang dipakai HARUS ada di core jar atau di hasil filter — kalau
# ada yang hilang, build GAGAL di sini, bukan crash di HP.
echo "== filter unused material-icons-extended classes (v1.6 slimming) =="
python3 - "$W/libs" "$PROJ/app/src/main/java" <<'PYEOF'
import sys, os, re, zipfile, shutil
libs_dir, src_root = sys.argv[1], sys.argv[2]
pat = re.compile(r"Icons\.(?:AutoMirrored\.)?(?:Filled|Outlined|Rounded|Sharp|TwoTone)\.([A-Za-z0-9]+)")
used = set()
for root, _, files in os.walk(src_root):
    for fn in files:
        if fn.endswith('.kt'):
            with open(os.path.join(root, fn), encoding='utf-8') as f:
                used.update(pat.findall(f.read()))
print(f'icons referenced in sources: {len(used)} -> {sorted(used)}')
def jar_path(fragment):
    hits = [fn for fn in os.listdir(libs_dir) if fragment in fn and fn.endswith('.jar')]
    assert len(hits) == 1, f'{fragment}: {hits}'
    return os.path.join(libs_dir, hits[0])
def kt_basenames(jar):
    names = set()
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if n.endswith('Kt.class'):
                names.add(os.path.basename(n)[:-len('Kt.class')])
    return names
core = kt_basenames(jar_path('material-icons-core'))
ext_jar = jar_path('material-icons-extended')
before = os.path.getsize(ext_jar)
kept, dropped = [], []
tmp = ext_jar + '.filtered'
with zipfile.ZipFile(ext_jar) as zin, zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        base = os.path.basename(item.filename)
        if item.filename.endswith('Kt.class') and not base[:-len('Kt.class')] in used:
            dropped.append(item.filename)
            continue
        zout.writestr(item, zin.read(item.filename))
        kept.append(item.filename)
missing = used - core - {os.path.basename(k)[:-len('Kt.class')] for k in kept}
if missing:
    print('ERROR: icons used but in NEITHER core nor extended jar:', sorted(missing))
    os.remove(tmp); sys.exit(1)
shutil.move(tmp, ext_jar)
after = os.path.getsize(ext_jar)
print(f'material-icons-extended: kept {len(kept)} classes, dropped {len(dropped)}, {before} -> {after} bytes')
PYEOF

echo "== duplicate class check =="
python3 - "$W/libs" <<'PYEOF'
import sys, zipfile, os, collections
d = sys.argv[1]
seen = collections.defaultdict(list)
for fn in sorted(os.listdir(d)):
    if not fn.endswith('.jar'):
        continue
    try:
        z = zipfile.ZipFile(os.path.join(d, fn))
    except Exception as e:
        print('BAD JAR', fn, e); sys.exit(1)
    for n in z.namelist():
        if n.endswith('.class') and not n.startswith('META-INF/'):
            seen[n].append(fn)
dups = {k: v for k, v in seen.items() if len(v) > 1}
print(f'total classes: {len(seen)}, duplicated: {len(dups)}')
for k in sorted(dups)[:40]:
    print('DUP', k, dups[k])
sys.exit(1 if dups else 0)
PYEOF

CP=$(IFS=:; echo "${LIB_JARS[*]}")

# ---------------------------------------------------------------- aapt2
echo "== aapt2 compile (app res) =="
"$AAPT2" compile --dir "$SRC/res" -o "$W/res-compiled/app-res.zip"

echo "== aapt2 compile (lib res) =="
declare -a R_FLAGS=()
j=0
for d in "$W"/aar/*/; do
  if [ -d "${d}res" ] && [ -n "$(ls -A "${d}res" 2>/dev/null)" ]; then
    "$AAPT2" compile --dir "${d}res" -o "$W/res-compiled/lib$j.zip"
    R_FLAGS+=(-R "$W/res-compiled/lib$j.zip")
  fi
  j=$((j+1))
done
R_FLAGS+=(-R "$W/res-compiled/app-res.zip")

echo "== manifest (inject package attr) =="
python3 - "$SRC/AndroidManifest.xml" "$W/AndroidManifest.xml" <<'PYEOF'
import sys
src, dst = sys.argv[1], sys.argv[2]
t = open(src).read()
assert 'package=' not in t.split('>')[0], 'manifest already has package attr'
t = t.replace('<manifest xmlns:android=', '<manifest package="com.sonara.app" xmlns:android=', 1)
open(dst, 'w').write(t)
print('manifest written')
PYEOF

echo "== aapt2 link =="
"$AAPT2" link -o "$W/app.ap_" \
  --manifest "$W/AndroidManifest.xml" \
  -I "$ANDROID_JAR" \
  "${R_FLAGS[@]}" \
  --auto-add-overlay \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 9 --version-name 1.0.8 \
  --java "$W/gen"
ls -la "$W/app.ap_" "$W/gen/com/sonara/app/R.java"

# ---------------------------------------------------------------- library R classes
echo "== generate library R.java (v2.4 fix, Widget Pet) =="
find "$W/gen" -name R.java -not -path "$W/gen/com/sonara/app/R.java" -delete
python3 "$W/gen_lib_r.py" "$W/gen" "$W/aar" --write
echo "library R.java files: $(find "$W/gen" -name R.java | wc -l)"

# ---------------------------------------------------------------- javac R
echo "== javac R.java (app + all library R classes) =="
mapfile -t R_JAVA < <(find "$W/gen" -name R.java | sort)
echo "R.java files to compile: ${#R_JAVA[@]}"
javac --release 17 -cp "$CP:$ANDROID_JAR" -d "$W/classes" "${R_JAVA[@]}"

# ---------------------------------------------------------------- kotlinc
echo "== kotlinc =="
mapfile -t KT_SOURCES < <(find "$SRC/java" -name '*.kt' | sort)
echo "kotlin sources: ${#KT_SOURCES[@]}"
java -Xmx3g -cp "$KOTLIN_COMPILER:$KOTLIN_STDLIB:$COROUTINES_CORE:$ANNOTATIONS_JAR" \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -jvm-target 17 \
  -Xplugin="$COMPOSE_PLUGIN" \
  -cp "$CP:$ANDROID_JAR:$W/classes" \
  -d "$W/classes" \
  "${KT_SOURCES[@]}"

# ---------------------------------------------------------------- d8
echo "== jar app classes =="
cd "$W/classes" && jar --create --file "$W/app-classes.jar" . && cd "$PROJ"
echo "== d8 =="
rm -f "$W/dex/classes"*.dex
java -Xmx2g -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 26 \
  --lib "$ANDROID_JAR" \
  --output "$W/dex" \
  "$W/app-classes.jar" "${LIB_JARS[@]}"
ls -la "$W/dex"

# ---------------------------------------------------------------- package
echo "== package apk =="
rm -f "$W/unsigned.apk"
cp "$W/app.ap_" "$W/unsigned.apk"
cd "$W/dex" && zip -0 -q "$W/unsigned.apk" classes*.dex && cd "$PROJ"

echo "== native libs (gomobile libgojni.so, arm64) =="
SO_SRC="$W/aar/dl-mixmusic/jni/arm64-v8a/libgojni.so"
[ -f "$SO_SRC" ] || { echo "ERROR: libgojni.so missing in mixmusic.aar"; exit 1; }
mkdir -p "$W/native-libs/lib/arm64-v8a"
cp "$SO_SRC" "$W/native-libs/lib/arm64-v8a/libgojni.so"
(cd "$W/native-libs" && zip -0 -q "$W/unsigned.apk" lib/arm64-v8a/libgojni.so)
echo "libgojni.so added: $(stat -c%s "$SO_SRC") bytes"

echo "== META-INF/services (ServiceLoader) =="
SERVICES_DIR="$W/services"
rm -rf "$SERVICES_DIR" && mkdir -p "$SERVICES_DIR"
for jar in "$W/libs"/*.jar; do
  unzip -o -q "$jar" "META-INF/services/*" -d "$SERVICES_DIR" 2>/dev/null || true
done
if [ -d "$SERVICES_DIR/META-INF/services" ]; then
  find "$SERVICES_DIR/META-INF/services" -type f -printf '%P\n' | sort
  (cd "$SERVICES_DIR" && zip -q "$W/unsigned.apk" -r META-INF/services)
  echo "services files added to APK"
else
  echo "WARNING: no META-INF/services found in dep jars"
fi

"$ZIPALIGN" -f -P 16 4 "$W/unsigned.apk" "$W/aligned.apk"

echo "== keystore =="
if [ ! -f "$PROJ/debug.keystore" ]; then
  keytool -genkeypair -v -keystore "$PROJ/debug.keystore" \
    -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
else
  echo "reusing existing keystore $PROJ/debug.keystore"
fi

echo "== sign =="
java -cp "$BT/lib/apksigner.jar" com.android.apksigner.ApkSignerTool sign \
  --ks "$PROJ/debug.keystore" --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias androiddebugkey \
  --out "$PROJ/sonara-debug.apk" "$W/aligned.apk"

echo "== verify =="
"$AAPT2" dump badging "$PROJ/sonara-debug.apk"
java -cp "$BT/lib/apksigner.jar" com.android.apksigner.ApkSignerTool verify --verbose "$PROJ/sonara-debug.apk"
ls -la "$PROJ/sonara-debug.apk"
md5sum "$PROJ/sonara-debug.apk"
sha256sum "$PROJ/sonara-debug.apk"
echo BUILD OK
