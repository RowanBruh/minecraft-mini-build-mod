# Minecraft Mini Build Mod

An innovative Minecraft mod that introduces the ability to create interactive miniature replicas of structures within your Minecraft world.

## Features

- **Create Mini Replicas**: Select any structure (up to 32 blocks in any dimension) and create a scaled-down interactive miniature version.
- **Bidirectional Synchronization**: Changes made to the miniature structure affect the real-world structure and vice versa.
- **Special Interactions**: 
  - Toggle wall visibility (shift-right-click) to see inside structures
  - Show yourself as a giant in the sky (right-click) above the miniature structure

## Installation

1. Install Minecraft Forge for Minecraft 1.16.5
2. Download the latest release JAR file
3. Place the JAR file in your Minecraft mods folder
4. Launch Minecraft with the Forge profile

## Usage

1. Craft the Mini Build Creator item
2. Select the first corner of your structure by right-clicking
3. Select the second corner by right-clicking again
4. The miniature structure will be created
5. Interact with the miniature structure:
   - Right-click to toggle giant player visibility
   - Shift-right-click to toggle wall visibility
   - Modify blocks to see changes reflected in the real structure

## Development

### Requirements
- Java Development Kit (JDK) 17
- Gradle 8.0+
- Minecraft Forge 1.16.5-36.2.39

### Building the Mod
```bash
./gradlew build
```

The built JAR file will be located in `build/libs/`.

## License

This project is licensed under the MIT License - see the LICENSE file for details.