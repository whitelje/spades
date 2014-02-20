-include config.mk

# Settings
PROGRAM ?= Spades
PACKAGE ?= org.pileus.spades
KEYFILE ?= ~/.android/android.p12
KEYTYPE ?= pkcs12
KEYNAME ?= android
ANDROID ?= /opt/android-sdk-update-manager/platforms/android-18/android.jar
SDKLIB  ?= /opt/android-sdk-update-manager/tools/lib/sdklib.jar
TOOLS   ?= /opt/android-sdk-update-manager/build-tools/19.0.1

# Variables
PATH    := $(PATH):$(TOOLS)
DIR     := $(subst .,/,$(PACKAGE))
RES     := $(wildcard res/*/*.*)
SRC     := $(wildcard src/$(DIR)/*.java)
GEN     := gen/$(DIR)/R.java
OBJ     := obj/$(DIR)/R.class
APK     := java -classpath $(SDKLIB) \
                com.android.sdklib.build.ApkBuilderMain

# Targets
debug: bin/$(PROGRAM).dbg

release: bin/$(PROGRAM).apk

compile: $(OBJ)

clean:
	rm -rf bin gen obj

# ADB targets
logcat:
	adb logcat $(PROGRAM):D AndroidRuntime:E '*:S'

run: bin/install.stamp
	adb shell am start -W                 \
		-a android.intent.action.MAIN \
		-n $(PACKAGE)/.Main

install bin/install.stamp: bin/$(PROGRAM).dbg
	adb install -r $+
	touch bin/install.stamp

uninstall:
	adb uninstall $(PACKAGE)
	rm bin/install.stamp

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
	        convert -trim -resize '256x256!' \
	                $$png $$png;             \
	done

# Rules
%.dbg: %.dex %.res | bin
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

%.dex: $(OBJ) | bin
	@echo "DEX    $@ "
	@dx --dex --output $@ obj

%.res: AndroidManifest.xml $(RES) | bin
	@echo "RES    $@"
	@aapt package -f -m               \
		-I $(ANDROID)             \
		-M AndroidManifest.xml    \
		-S res                    \
		-F $*.res

$(OBJ): $(SRC) $(GEN) | obj
	@echo "JAVAC  obj/*.class"
	@javac -g -Xlint:unchecked        \
		-bootclasspath $(ANDROID) \
		-encoding      UTF-8      \
		-source        1.5        \
		-target        1.5        \
		-classpath     obj        \
		-d             obj        \
		$+

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
