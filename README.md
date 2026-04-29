# StructScript

## Project Identity
- **Name:** StructScript
- **Mod ID:** `structscript`
- **Version:** `${version}` (Resolved at build time)

## Technical Summary
The **StructScript** mod provides a specialized toolkit designed to dynamically load, preview, and place saved structural schematics into the Minecraft world. During initialization, the mod provisions a dedicated directory (`config/structscript/structures/`) intended to house external schematic files. It introduces the `StructureBlueprintItem`, a custom tool that holds NBT data pointing to a specific schematic. Players holding a blueprint can utilize custom client-to-server networking (`RotatePacket`) to adjust the structure's orientation in real-time before finalizing the placement block-by-block.

## Feature Breakdown
- **Dynamic Schematic Management:** Automatically generates and monitors a dedicated `structures/` folder, allowing server admins to drop in new building schematics without restarting or modifying the `.jar`.
- **Physical Blueprint Items:** Introduces the `STRUCTURE_BLUEPRINT` item, which physically represents a schematic in the player's inventory, configured via NBT to load a specific file.
- **Interactive Orientation:** Implements custom networking (`RotatePacket`) that allows players to seamlessly rotate the blueprint in-game prior to placing the structure into the world.
- **Caching System:** Utilizes an internal `SchematicManager` to cache loaded structures in memory for optimal performance, preventing repeated heavy disk reads.

## Command Registry
*Note: All commands require OP Permission Level 2.*

| Command | Description | Permission Level |
| :--- | :--- | :--- |
| `/structscript reload` | Clears the `SchematicManager` internal cache and reloads all structures from the disk. | OP (2) |
| `/structscript give <file> <amount> [<targets>]` | Generates a `STRUCTURE_BLUEPRINT` item bound to the specified `<file>` and gives it to the targeted player(s) or the command executor. | OP (2) |

## Configuration Schema
*Note: This mod does not generate a traditional `config.json` file for settings. Instead, it relies on a physical directory structure. Upon initialization, it creates the following folder where the mod expects schematic files (e.g., `.nbt`) to be stored:*

```text
config/
  └─ structscript/
       └─ structures/
            └─ (Place your schematic files here)
```

## Developer Info
- **Author:** Me! (elthisboy)
- **Platform:** Fabric 1.21.1
