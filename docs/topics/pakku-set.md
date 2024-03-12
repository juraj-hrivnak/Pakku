# pakku set

Set various properties of your pack or projects.

## Usage

```
./pakku set [<options>] [<projects>]...
```
{prompt="$"}

## Options

`-s`, `--side=(client|server|both)`                   
: Change the side of a project

`-u`, `--update-strategy=(latest|none)`               
: Change the update strategy of a project

`-r`, `--redistributable=true|false`                  
: Change whether the project can be redistributed

`-t`, `--target=(curseforge|modrinth|multiplatform)`  
: Change the target of the pack

`-v`, `--mc-versions=<text>...`                       
: Change the minecraft versions

`-l`, `--loaders=<name>=<version>`                           
: Change the mod loaders

`-h`, `--help`                                        
: Show help message and exit
