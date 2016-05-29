# Settings
KEYFILE ?= ~/.android/android.p12
KEYTYPE ?= pkcs12
KEYNAME ?= android
ANDROID ?= /opt/android-sdk-linux/platforms/android-23/android.jar
SDKLIB  ?= /opt/android-sdk-linux/tools/lib/sdklib.jar
TOOLS   ?= /opt/android-sdk-linux/build-tools/23.0.2

# Variables
PATH    := $(PATH):$(TOOLS)
DIR     := $(subst .,/,$(PACKAGE))
RES     := $(wildcard res/*/*.*)
SRC     := $(wildcard src/*.java src/$(DIR)/*.java)
GEN     := gen/$(DIR)/R.java
OBJ     := obj/$(DIR)/R.class
APK     := java -classpath $(SDKLIB) \
                com.android.sdklib.build.ApkBuilderMain

# Targets
debug: bin/$(PROGRAM)-dbg.apk

release: bin/$(PROGRAM).apk

compile: $(OBJ)

clean:
	rm -rf bin gen obj

# ADB targets
logcat:
	adb $(ADBFLAGS) logcat $(PROGRAM):D AndroidRuntime:E '*:S'

run: bin/install.stamp
	adb $(ADBFLAGS) shell am start -W     \
		-a android.intent.action.MAIN \
		-n $(PACKAGE)/.$(ACTIVITY)

install bin/install.stamp: bin/$(PROGRAM)-dbg.apk
	adb $(ADBFLAGS) install -r $+
	touch bin/install.stamp

uninstall:
	adb $(ADBFLAGS) uninstall $(PACKAGE)
	rm -f bin/install.stamp

# Emulators
create-avd:
	android create avd --target android-18 --name virtual

start-avd:
	emulator -avd virtual &

run-avd:
	$(MAKE) run ADBFLAGS="-s emulator-5554"

# Graphics targets
graphics:
	git checkout graphics  --    \
	        'opt/drawable/*.svg' \
	        'opt/drawable/*.xcf' \
	        'res/drawable/*.png' \
	        'res/drawable/*.jpg' \
	        || true
	git reset HEAD --            \
	        'opt/drawable/*.svg' \
	        'opt/drawable/*.xcf' \
	        'res/drawable/*.png' \
	        'res/drawable/*.jpg' \
	        || true

convert:
	for svg in opt/drawable/*.svg; do        \
	        png=$${svg/svg/png};             \
	        png=$${png/opt/res};             \
	        rsvg-convert -w 2048 -h 2048     \
	                $$svg -o $$png;          \
	        convert -trim                    \
	                -resize      '254x254!'  \
	                -bordercolor '#00000000' \
	                -border      '1x1'       \
	                $$png $$png;             \
	done

# Rules
%-dbg.apk: %.dex %.res | bin
	@echo "APK    $@.in"
	@$(APK) $@.in -f $*.dex -z $*.res
	@echo "ALIGN  $@"
	@zipalign -f 4 $@.in $@

%.apk: %.dex %.res | bin
	@echo "APKU   $@.in"
	@$(APK) $@.in -u -f $*.dex -z $*.res
	@echo "SIGN   $@.in"
	@jarsigner -storetype $(KEYTYPE)  \
	           -keystore  $(KEYFILE)  \
	           $@.in      $(KEYNAME)
	@echo "ALIGN  $@"
	@zipalign -f 4 $@.in $@

%.dex: $(OBJ) makefile | bin
	@echo "DEX    $@"
	@dx --dex --output $@ obj

%.res: AndroidManifest.xml $(RES) | bin
	@echo "RES    $@"
	@aapt package -f -m               \
		-I $(ANDROID)             \
		-M AndroidManifest.xml    \
		-S res                    \
		-F $*.res

$(OBJ): $(SRC) $(GEN) makefile | obj
	@echo "JAVAC  obj/*.class"
	@javac -g                         \
		-Xlint:unchecked          \
		-Xlint:deprecation        \
		-bootclasspath $(ANDROID) \
		-encoding      UTF-8      \
		-source        1.7        \
		-target        1.7        \
		-classpath     obj        \
		-d             obj        \
		$(filter-out makefile,$+)

$(GEN): AndroidManifest.xml $(RES) | gen
	@if ! [ -d "res/drawable" ]; then \
		echo Please run           \
		     \'make graphics\';   \
		exit 1;                   \
	fi
	@echo "GEN    $@"
	@aapt package -f -m               \
		-I $(ANDROID)             \
		-M AndroidManifest.xml    \
		-S res                    \
		-J gen

# Directories
bin gen obj:
	@mkdir -p $@

# Keep intermediate files
.SECONDARY:
