# AI Companion Mod for Minecraft

An innovative Minecraft mod that introduces advanced AI-controlled companion entities with dynamic interaction and management capabilities.

## Features

- **AI-Controlled Companions**: Custom entities with advanced goal-based AI system
- **Command System**: Control companions through in-game commands
- **Admin Panel**: In-game GUI for managing companions
- **Web Interface**: Remote control of AI companions through a browser
- **Skin System**: Customize companion appearance with built-in and custom skins
- **Secure Authentication**: JWT-based security for web interface
- **Real-time Updates**: WebSocket integration for live updates between game and web interface

## Requirements

- Minecraft 1.16.5
- Forge 36.2.39

## Installation

1. Download the latest release from the [Releases](https://github.com/your-username/ai-companion-mod/releases) page
2. Place the JAR file in your Minecraft `mods` folder
3. Start Minecraft with Forge installed

## Web Interface

The mod includes a web server that provides a remote control interface for the AI companions:

- Default URL: `http://localhost:5000`
- Default admin credentials: 
  - Username: `admin`
  - Password: `password`

**Important**: Change the default credentials after first login for security reasons!

## Commands

- `/companion spawn [name]` - Spawn a new AI companion
- `/companion teleport` - Teleport your companion to your location
- `/companion follow` - Make your companion follow you
- `/companion stay` - Make your companion stay in place
- `/companion attack [target]` - Make your companion attack a target
- `/companion mine [x] [y] [z]` - Make your companion mine at the specified coordinates
- `/companion collect` - Make your companion collect nearby items
- `/companion settings` - Open the companion settings GUI

## Development

### Building from Source

1. Clone the repository
2. Run `./gradlew build` to build the mod
3. The compiled JAR will be in `build/libs/`

### Tech Stack

- Java with Minecraft Forge
- Node.js with Express for the web server
- WebSockets for real-time communication

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Credits

- Minecraft Forge team for the modding framework
- Contributors to this project