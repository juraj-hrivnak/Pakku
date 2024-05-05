#!/usr/bin/env bash

# Do not stop workflows on errors from this script
set +e

# Get latest tag
latest_tag=$(git describe --tags --abbrev=0)

# Exit if latest tag is not version tag
if [[ $latest_tag != v* ]]; then
  exit 1
fi

# Create '../build/install/' directory
[ -d 'build/install/' ] || mkdir build/install/

# Remove 'v' from tag
latest_version="${latest_tag//v}"

# -- BREW --

tar_url="https://github.com/juraj-hrivnak/Pakku/releases/download/$latest_tag/Pakku-$latest_version.tar"
tar_hash=$(curl -sL $tar_url | shasum -a 256 | cut -d ' ' -f1)

# Creates $brewform variable
read -r -d '' brewform << EOM
class Pakku < Formula
  desc "Multiplatform modpack manager for Minecraft: Java Edition"
  homepage "https://juraj-hrivnak.github.io/Pakku/"
  url "$tar_url"
  hash "$tar_hash"
  license "EUPL-1.2"

  def install
    rm_f Dir["bin/*.bat"]
    libexec.install %w[bin lib]
    (bin/"pakku").write_env_script libexec/"bin/Pakku", Language::Java.overridable_java_home_env
  end

  test do
    assert_match "Could not read 'pakku-lock.json'", shell_output(bin/"pakku add jei", 1)
  end
end

EOM

echo "$brewform" >> build/install/pakku.rb

# -- SCOOP --

zip_url="https://github.com/juraj-hrivnak/Pakku/releases/download/$latest_tag/Pakku-$latest_version.zip"
zip_hash=$(curl -sL $zip_url | shasum -a 256 | cut -d ' ' -f1)

# Creates $scoopform variable
read -r -d '' scoopform << EOM
{
  "version": "$latest_version",
  "description": "A multiplatform modpack manager for Minecraft: Java Edition.",
  "homepage": "https://juraj-hrivnak.github.io/Pakku/",
  "license": "EUPL-1.2-or-later",
  "suggest": {
    "JDK": [
      "java/openjdk"
    ]
  },
  "url": "$zip_url",
  "hash": "$zip_hash",
  "extract_dir": "Pakku-$latest_version",
  "bin": "bin/Pakku.bat"
}

EOM

echo "$scoopform" >> build/install/pakku.json
