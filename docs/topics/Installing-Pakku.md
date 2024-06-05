# Installing Pakku

You can install Pakku [using Scoop](#install-scoop) for Windows,
[using Brew](#install-brew) for macOS (or Linux)
or [manually](#install-manually).

Pakku also requires Java to be installed on your computer,
so please, before installing, check whether you have Java installed on your computer.
If not, install it from [here](https://www.java.com/en/download/).

<procedure title="Install Pakku using Scoop for Windows" id="install-scoop">

1. Check whether you have Scoop installed on your computer.
   If not, check the installation instructions from [here](https://scoop.sh/).
2. In your terminal/CMD, run this Scoop command:
   ```
   scoop install https://juraj-hrivnak.github.io/Pakku/install/pakku.json
   ```
   {prompt="$"}
3. In your modpack folder, run Pakku from your terminal/CMD:
   ```
   pakku
   ```
   {prompt="$"}

</procedure>

<procedure title="Install Pakku using Brew for macOS (or Linux)" id="install-brew">

1. Check whether you have Brew installed on your computer.
   If not, check the installation instructions from [here](https://brew.sh/#install).
2. In your terminal, run this Brew command:
   ```
   brew install juraj-hrivnak/pakku/pakku
   ```
   {prompt="$"}
3. In your modpack folder, run Pakku from your terminal:
   ```
   pakku
   ```
   {prompt="$"}

</procedure>

<procedure title="Install Pakku manually" id="install-manually">

1. Download the `pakku.jar` from [GitHub releases]
   and place it into your modpack folder.

2. In your modpack folder, run Pakku locally from your terminal/CMD:
   ```
   java -jar pakku.jar
   ```
   {prompt="$"}

   > To get to your modpack folder, use the [`cd` command](https://en.wikipedia.org/wiki/Cd_(command)).
   {style="note"}

</procedure>

<note>
Currently, the only option to run Pakku is using Java.
This requires Java Runtime to be installed on your computer.

It is not possible to release Pakku as native executable
because there is no library for compression for Kotlin Native.
If this ever changes in the future, there might be release of Pakku
as native executable.
</note>

   
[GitHub releases]: https://github.com/juraj-hrivnak/Pakku/releases/latest