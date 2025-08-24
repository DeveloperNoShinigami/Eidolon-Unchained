# AI Deity Examples: Dark and Light

This document provides two complete examples of AI deities using the proper Eidolon chant system.

## 🌙 Nyxathel the Shadow Lord (Dark Deity)

### Overview
- **ID**: `eidolonunchained:dark_deity`
- **Theme**: Darkness, death, forbidden knowledge
- **Personality**: Mysterious, cryptic, rewards loyalty but punishes weakness
- **Color**: Dark purple (RGB: 123, 85, 140)

### Chant Sequences

#### 1. Dark Communion
**Signs**: Wicked → Death → Blood  
**Purpose**: Basic conversation with the Shadow Lord  
**Requirements**: No reputation required, 5-minute cooldown  

To perform this chant:
1. Hold a wand or staff
2. Right-click in air to cast: **Wicked Sign**
3. Right-click in air to cast: **Death Sign**  
4. Right-click in air to cast: **Blood Sign**
5. The deity will respond to your prayer

#### 2. Shadow's Blessing
**Signs**: Wicked → Wicked → Death  
**Purpose**: Request dark blessings and power  
**Requirements**: 10+ reputation, 15-minute cooldown  

#### 3. Forbidden Knowledge
**Signs**: Death → Blood → Wicked  
**Purpose**: Seek dark wisdom and secrets  
**Requirements**: 25+ reputation, 30-minute cooldown  

### Example Interaction
```
Player performs: Wicked → Death → Blood
Player types: "Great Shadow Lord, I seek your dark wisdom"

Nyxathel responds: "Mortal... you dare disturb my eternal slumber? *chuckles darkly* 
Your devotion to the shadows intrigues me. Tell me, what forbidden knowledge do you seek? 
But know this - all wisdom comes with a price, and the darkness remembers those who serve it well."

*Automatically gives player 2 Soul Shards and Night Vision effect*
```

### Progression Rewards
- **10 Rep (Shadow Initiate)**: Soul Shards + Night Vision
- **25 Rep (Dark Scholar)**: Death Essence + Unholy Symbol  
- **50 Rep (Shadow Master)**: Warped Sprouts + Zombie Heart + Strength

---

## ☀️ Lumina the Sacred Guardian (Light Deity)

### Overview
- **ID**: `eidolonunchained:light_deity`
- **Theme**: Light, healing, protection, redemption
- **Personality**: Warm, compassionate, always offers redemption
- **Color**: Golden yellow (RGB: 255, 230, 117)

### Chant Sequences

#### 1. Sacred Communion
**Signs**: Sacred → Harmony → Warding  
**Purpose**: Basic conversation with the Sacred Guardian  
**Requirements**: No reputation required, 3-minute cooldown  

To perform this chant:
1. Hold a wand or staff
2. Right-click in air to cast: **Sacred Sign**
3. Right-click in air to cast: **Harmony Sign**
4. Right-click in air to cast: **Warding Sign**
5. The deity will respond with warmth and guidance

#### 2. Light's Blessing  
**Signs**: Sacred → Sacred → Harmony  
**Purpose**: Request healing and divine protection  
**Requirements**: 5+ reputation, 10-minute cooldown  

#### 3. Divine Guidance
**Signs**: Harmony → Warding → Sacred  
**Purpose**: Seek wisdom and holy knowledge  
**Requirements**: 15+ reputation, 20-minute cooldown  

#### 4. Purification Rite
**Signs**: Warding → Sacred → Warding  
**Purpose**: Cleanse negative effects and curses  
**Requirements**: 20+ reputation, 25-minute cooldown  

### Example Interaction
```
Player performs: Sacred → Harmony → Warding
Player types: "Blessed Lumina, I need healing and guidance"

Lumina responds: "Welcome, dear child of the light. I can see the weariness in your spirit, 
but also the goodness in your heart. Let my divine radiance heal your wounds and strengthen 
your resolve. Remember, even in the darkest times, the light within you can never be extinguished."

*Automatically gives player Regeneration and Resistance effects*
```

### Progression Rewards
- **10 Rep (Blessed Novice)**: Golden Apples + Regeneration
- **25 Rep (Sacred Devotee)**: Gold Inlay + Holy Symbol + Resistance
- **50 Rep (Light Champion)**: Totem of Undying + Blessed Bone + Absorption

---

## 🎯 How to Use in Game

### Step 1: Setup
1. Build an Effigy for your chosen deity
2. Place it in an appropriate location (dark areas for Nyxathel, bright areas for Lumina)
3. Obtain a wand or staff for casting signs

### Step 2: Perform Chants
1. Stand near the Effigy
2. Cast the required sign sequence using your wand
3. Type your prayer/request in chat
4. Wait for the deity's AI response

### Step 3: Build Reputation
- **For Nyxathel**: Perform dark magic, explore dangerous areas, show cunning
- **For Lumina**: Help others, heal the wounded, protect the innocent, build in light

### Step 4: Unlock Advanced Chants
As your reputation grows, you'll gain access to more powerful chant sequences with greater rewards.

---

## 🔧 Configuration Notes

### For Server Owners
- Adjust `reputation_required` values to control progression speed
- Modify `cooldown_minutes` to balance prayer frequency
- Customize `blessing_commands` and `curse_commands` for your server
- Edit personality text to match your server's theme

### For Datapack Creators
- Use these as templates for creating new deities
- Mix and match different sign combinations for unique chants
- Create themed deity sets (elemental, nature, cosmic, etc.)
- Balance reputation requirements across multiple deities

### API Configuration
- Both deities use slightly different AI settings:
  - **Nyxathel**: Higher temperature (0.8) for more creative/unpredictable responses
  - **Lumina**: Lower temperature (0.6) for more consistent/gentle responses
- Adjust `max_output_tokens` to control response length
- Modify safety settings based on your server's content policies

---

## 🎨 Customization Ideas

### Theme Variations
- **Elemental Deities**: Fire (Flame + Winter signs), Water (Winter + Harmony), Earth (Warding + Soul)
- **Magic Deities**: Arcane (Magic + Mind), Illusion (Mind + Soul), Enchantment (Magic + Harmony)
- **Nature Deities**: Growth (Harmony + Sacred), Decay (Death + Soul), Balance (Warding + Flame)

### Advanced Features
- **Seasonal Behaviors**: Different responses based on in-game seasons
- **Moon Phase Responses**: Special behaviors during full/new moons
- **Weather Reactions**: Enhanced powers during storms, droughts, etc.
- **Player History**: AI remembers past conversations and actions

---

## 🚀 Getting Started

1. **Install the mod** with these configuration files
2. **Start with Lumina** (easier, forgiving) to learn the system
3. **Progress to Nyxathel** once comfortable with chant mechanics
4. **Experiment with different prayer types** to discover all features
5. **Create your own deities** using these examples as templates

The AI deity system transforms static Eidolon deities into dynamic, responsive entities that create unique experiences for every player interaction!
