# Installing Pakku

You can install Pakku using:

- [**Scoop** for Windows](#install-scoop)
- [**Brew** for macOS (or Linux)](#install-brew)
- [**Nix** for Linux, macOS or Windows WSL](#install-nix): [![latest packaged version(s)](https://repology.org/badge/latest-versions/pakku-minecraft.svg)](https://repology.org/project/pakku-minecraft/versions)
- **Yay** for Arch Linux: [![latest packaged version(s)](https://repology.org/badge/latest-versions/pakku-mc.svg)](https://repology.org/project/pakku-mc/versions)

or [**manually**](#install-manually).

Pakku also requires Java to be installed on your computer,
so please, before installing, check whether you have Java installed on your computer.
If not, you _can_ install it from [here](https://www.java.com/en/download/).

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

<procedure title="Install Pakku using Nix" id="install-nix">

<a href="https://repology.org/project/pakku-minecraft/versions">
    <img src="https://repology.org/badge/vertical-allrepos/pakku-minecraft.svg" alt="Packaging status" align="right"/>
</a>

Pakku is available from [nixpkgs](https://github.com/NixOS/nixpkgs) as `pakku`.

1. Check whether you have Nix installed on your computer. NixOS comes with it preinstalled.
   If not, check the installation instructions from [here](https://nixos.org/download/) for your OS.
2. If using just the nix package manager you can install Pakku with these commands:
   ```nix
   # without flakes:
   nix-env -iA nixpkgs.pakku
   
   # with flakes:
   nix profile install nixpkgs#pakku
   ```
3. For NixOS and Home Manager you can install by adding `pakku` to your system or user packages.
   
   NixOS:
   ```nix
   {
      environment.systemPackages = [
         pkgs.pakku
      ];
   }
   ```
   
   Home Manager:
   ```nix
   {
      home.packages = [
         pkgs.pakku
      ];
   }
   ```
4. In your modpack folder, run Pakku from your terminal
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
   
[GitHub releases]: https://github.com/juraj-hrivnak/Pakku/releases/latest