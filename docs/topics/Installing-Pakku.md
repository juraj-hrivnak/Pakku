# Installing Pakku

[//]: # (To run Pakku, you have two options:)

Currently, the only option to install and run Pakku is [using Java](#install-java).
This requires Java to be installed on your computer.

[//]: # (2. [Run Pakku as native executable]&#40;#running_pakku_as_native_executable&#41;.)

[//]: # (    - Depending on your operating system, you will need to use a different version of native Pakku.)

<procedure title="Install and run Pakku using Java" id="install-java">

1. Check whether you have Java installed on your computer.
   If not, install it from [here](https://www.java.com/en/download/).

2. Download the `pakku.jar` from [GitHub releases]
   and place it into your modpack folder.

3. In your modpack folder, run Pakku from the Terminal/CMD:
   ```
   java -jar pakku.jar
   ```
   {prompt="$"}

   > To get to your modpack folder, use the [`cd` command](https://en.wikipedia.org/wiki/Cd_(command)).
   {style="note"}

</procedure>

[//]: # (<procedure title="Running Pakku as native executable" id="running_pakku_as_native_executable">)

[//]: # ()
[//]: # (1. Download the [respective native executable]&#40;#exe&#41; from [GitHub releases])

[//]: # (   and place it into your modpack folder.)

[//]: # ()
[//]: # (   | Operating System | Native Executable |)

[//]: # (   |------------------|-------------------|)

[//]: # (   | Linux            | `pakku`           |)

[//]: # (   | macOS            | `pakku-macos`     |)

[//]: # (   | Windows          | `pakku.exe`       |)

[//]: # (   {id="exe"})

[//]: # ()
[//]: # (   > For macOS, only versions 11 and higher are supported!)

[//]: # (   {style="warning"})

[//]: # ()
[//]: # (2. In your modpack folder, run Pakku from the Terminal/CMD:)

[//]: # (   <tabs>)

[//]: # (   <tab title="Linux">)

[//]: # ()
[//]: # (   ```)

[//]: # (   ./pakku)

[//]: # (   ```)

[//]: # (   {prompt="$"})

[//]: # ()
[//]: # (   </tab>)

[//]: # (   <tab title="macOS">)

[//]: # ()
[//]: # (   ```)

[//]: # (   ./pakku-macos)

[//]: # (   ```)

[//]: # (   {prompt="$"})

[//]: # ()
[//]: # (   </tab>)

[//]: # (   <tab title="Windows">)

[//]: # ()
[//]: # (   ```)

[//]: # (   .\pakku.exe)

[//]: # (   ```)

[//]: # (   {prompt="$"})

[//]: # ()
[//]: # (   </tab>)

[//]: # (   </tabs>)

[//]: # ()
[//]: # (</procedure>)

<note>
   <p>From onward, running Pakku in CLI will be only referenced
   as <code>%pakku%</code>.</p>
</note>
   
[GitHub releases]: https://github.com/juraj-hrivnak/Pakku/releases/latest