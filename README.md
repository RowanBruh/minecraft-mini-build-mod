# Minecraft Mini Build Mod

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.16.5-green)
![Forge Version](https://img.shields.io/badge/Forge-36.2.39-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

An innovative Minecraft mod that introduces the ability to create interactive miniature replicas of structures within your Minecraft world. Perfect for architects, builders, and anyone who wants to design and test structures in a more intuitive way.

<p align="center">
  <img src="docs/minibuilds_banner.png" alt="Mini Builds Mod Banner" width="600" />
</p>

## Features

- **Create Mini Replicas**: Select any structure (up to 32 blocks in any dimension) and create a scaled-down interactive miniature version at 1/10th the size.
- **Bidirectional Synchronization**: Changes made to the miniature structure affect the real-world structure and vice versa in real-time.
- **Special Interactions**: 
  - Toggle wall visibility (shift-right-click) to see inside structures
  - Show yourself as a giant in the sky (right-click) above the miniature structure
- **Persistence**: Miniature structures save with your world and maintain all their functionality between play sessions.

## Screenshots

<table>
  <tr>
    <td><img src="docs/screenshot1.png" alt="Creating a Mini Structure" /></td>
    <td><img src="docs/screenshot2.png" alt="Wall Visibility Toggle" /></td>
  </tr>
  <tr>
    <td><img src="docs/screenshot3.png" alt="Giant Player in Sky" /></td>
    <td><img src="docs/screenshot4.png" alt="Editing a Mini Structure" /></td>
  </tr>
</table>

## Installation

1. Install [Minecraft Forge for Minecraft 1.16.5](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.16.5.html)
2. Download the latest release JAR file from the [Releases page](https://github.com/RowanBruh/minecraft-mini-build-mod/releases)
3. Place the JAR file in your Minecraft mods folder:
   - Windows: `%APPDATA%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
4. Launch Minecraft with the Forge profile

## Usage

1. Craft the Mini Build Creator item (recipe below)
2. Select the first corner of your structure by right-clicking on a block
3. Select the second corner by shift-right-clicking on another block
4. The miniature structure will be created where you clicked
5. Interact with the miniature structure:
   - Right-click to toggle giant player visibility
   - Shift-right-click to toggle wall visibility
   - Modify blocks to see changes reflected in the real structure

### Crafting Recipe

```
D   G   D
  E   E  
D   G   D
```

Where:
- D = Diamond
- G = Glass Pane
- E = Ender Pearl

## Development

### Requirements
- Java Development Kit (JDK) 11
- Gradle 7.5.1
- Minecraft Forge 1.16.5-36.2.39

### Setting Up the Development Environment

1. Clone this repository:
   ```bash
   git clone https://github.com/RowanBruh/minecraft-mini-build-mod.git
   cd minecraft-mini-build-mod
   ```

2. Set up the Forge development environment:
   ```bash
   ./gradlew genEclipseRuns    # For Eclipse
   # OR
   ./gradlew genIntellijRuns   # For IntelliJ IDEA
   ```

3. Import the project into your IDE

### Building the Mod
```bash
./gradlew build
```

The built JAR file will be located in `build/libs/`.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Minecraft Forge team for their amazing API
- The Minecraft modding community for inspiration and support