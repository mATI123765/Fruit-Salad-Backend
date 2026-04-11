# Super Idol Clicker

A Cookie Clicker-style idle game with Super Idol theme, developed by **Fruit Salad Ltd.**

## Team Members
- Jorge Ferrando (JFerrando)
- Joel Acosta
- Iker Molla
- Zakaria Hdouri

## Screenshots

The game features:
- Animated menu with particle effects
- Cookie Clicker-style game interface
- Dark theme with red/gold accents
- Sound effects and background music

## Project Structure

```
Super-Idol-Clicker/
├── build.gradle                    # Gradle build configuration
├── settings.gradle                 # Project settings
├── README.md                       # This file
└── src/main/
    ├── java/com/fruitsalad/
    │   ├── Main.java               # Application entry point
    │   ├── audio/
    │   │   └── SoundManager.java   # Audio management (singleton)
    │   ├── database/
    │   │   ├── DatabaseConnection.java  # MySQL connection
    │   │   └── DatabaseManager.java     # Database operations
    │   ├── game/
    │   │   ├── GamePanel.java      # Main game UI
    │   │   └── GameState.java      # Game logic and data
    │   └── ui/
    │       └── MenuPanel.java      # Animated login menu
    └── resources/
        ├── images/
        │   ├── main-image-clicker.png    # Main clicker image
        │   ├── game_background.png       # Background image
        │   ├── effects/                  # Social credit effects
        │   ├── upgrades/                 # Upgrade images (1-5)
        │   └── shop/                     # Shop item images (1-4)
        └── sounds/
            ├── menu-background_music.wav
            ├── background_music.wav
            ├── upgrade-buy_item-effect.wav
            ├── -999999_social_credit-effect.wav
            └── easter egg-willyrex_paradise.wav
```

## Requirements

- Java 17 or higher
- MySQL Server 8.0+
- Gradle 7.0+ (or use the wrapper)

## Database Setup

1. Make sure MySQL is running
2. Create the database and tables:

```sql
CREATE DATABASE IF NOT EXISTS new_super_idol_u;
USE new_super_idol_u;

-- Create user table if not exists
CREATE TABLE IF NOT EXISTS user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert test users
INSERT INTO user (username, email, password_hash) VALUES
('Jorgito', 'jorge@fruitsalad.com', 'hashed_password_1'),
('Joel', 'joel@fruitsalad.com', 'hashed_password_2'),
('Iker', 'iker@fruitsalad.com', 'hashed_password_3'),
('Zakaria', 'zakaria@fruitsalad.com', 'hashed_password_4')
ON DUPLICATE KEY UPDATE username=username;
```

3. Update database credentials in `DatabaseConnection.java`:
```java
private static final String PASSWORD = "your_password_here";
```

## How to Run

### Using Gradle Wrapper (Recommended)

```bash
# On Linux/Mac
./gradlew run

# On Windows
gradlew.bat run
```

### Build JAR

```bash
./gradlew jar
java -jar build/libs/super-idol-clicker-1.0.0.jar
```

## Game Features

### Upgrades (9 total)

| Upgrade | Type | Bonus | Base Cost |
|---------|------|-------|-----------|
| Microphone | Click | +1 CPC | 15 |
| Stage Lights | Click | +3 CPC | 100 |
| Water Bottle 105C | Click | +10 CPC | 500 |
| Golden Smile | Click | +25 CPC | 2,500 |
| Backup Dancers | Passive | +1 CPS | 50 |
| Fan Club | Passive | +5 CPS | 300 |
| Music Video | Passive | +20 CPS | 1,500 |
| Record Label | Passive | +100 CPS | 8,000 |
| World Tour | Passive | +500 CPS | 50,000 |

### Achievements (17 total)

- Click achievements: First Click, 100 Clicks, 1000 Clicks, etc.
- Credit achievements: +15 Social Credit, Good Citizen, Model Citizen, etc.
- Upgrade achievements: First Upgrade, Upgrade Enthusiast
- Playtime achievements: Loyal Fan, Devoted Follower, etc.

### Easter Egg

Click **105 times** in the **top-left corner** of the game screen to hear Willyrex singing "Paradise"!

## Controls

- **Left Click** on Super Idol to earn credits
- **Buy Upgrades** in the right panel to increase earnings
- **Music/Sound** toggles in the left panel

## Technical Details

- Built with Java Swing
- Uses MySQL with JDBC for data persistence
- Gradle for build management
- Cookie Clicker-inspired dark theme UI
- 60 FPS animation with particle effects

## License

This project is part of the Intermodular Project for DAM (Desarrollo de Aplicaciones Multiplataforma).

---

*The smile sweeter than honey, the love hotter than 105 degrees.*
