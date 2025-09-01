# Tips & Best Practices

**Advanced tips for creating professional content with Eidolon Unchained**

## Pro Tips

### Advanced JSON Techniques

#### Smart Translation Keys
Create logical hierarchies for easier maintenance:

```json
// Instead of long repetitive keys:
"very_long_mod_name.codex.entry.extremely_detailed_name.subsection"

// Use shorter, logical hierarchies:
"mod.undead.zombie.anatomy"
"mod.undead.zombie.behavior"
"mod.undead.skeleton.tactics"
"mod.magic.flame.basic"
"mod.magic.flame.advanced"
```

#### NBT Data in Research
Use NBT matching for sophisticated item requirements:

```json
{
  "type": "item",
  "item": "minecraft:enchanted_book",
  "count": 1,
  "nbt": "{StoredEnchantments:[{id:\"minecraft:mending\",lvl:1s}]}"
}
```

#### Multi-Reward Research
Grant multiple signs and items for major achievements:

```json
"rewards": [
  {"type": "sign", "sign": "death"},
  {"type": "sign", "sign": "soul"},
  {"type": "item", "item": "eidolon:soul_gem", "count": 3},
  {"type": "item", "item": "minecraft:experience_bottle", "count": 10}
]
```

### Content Organization Strategies

#### Thematic Grouping
Organize content by magical schools or themes:

```
data/mymod/
├── codex_entries/
│   ├── necromancy/           # Death magic theme
│   ├── pyromancy/            # Fire magic theme  
│   ├── cryomancy/            # Ice magic theme
│   └── general/              # Basic concepts
└── eidolon_research/
    ├── necromancy/
    ├── pyromancy/
    ├── cryomancy/
    └── general/
```

#### Progressive Difficulty
Structure content from beginner to expert:

```
basic_undead.json       # 1-2 stars, common mobs
intermediate_spirits.json # 3 stars, uncommon encounters  
advanced_liches.json    # 4-5 stars, rare/boss entities
```

#### Cross-Referencing
Link related content through shared targeting:

```json
// Research targets zombies
"triggers": ["entity:minecraft:zombie"]

// Codex entry also targets zombies
"targets": ["entity:minecraft:zombie"]

// Both discovered together naturally
```

### Performance Optimization

#### Efficient Targeting
Use specific targets rather than broad categories:

```json
// Good - specific targeting
"targets": ["entity:minecraft:zombie", "entity:minecraft:skeleton"]

// Avoid - overly broad (if not intended)
"targets": ["entity:*"]
```

#### Reasonable Task Requirements
Keep research tasks achievable but meaningful:

```json
// Good - reasonable progression
"tasks": {
  "1": [{"type": "item", "item": "minecraft:rotten_flesh", "count": 5}],
  "2": [{"type": "xp", "levels": 3}]
}

// Avoid - excessive grinding
"tasks": {
  "1": [{"type": "item", "item": "minecraft:diamond", "count": 64}]
}
```

## Common Patterns

### Discovery Chain Pattern
Create natural progression through related content:

1. **Kill common mob** → Basic research → Simple reward
2. **Use reward item** → Intermediate codex entry → New recipes
3. **Craft advanced item** → Advanced research → Rare rewards

### Knowledge Gate Pattern  
Use research as prerequisites for advanced content:

1. **Basic research** → Grants understanding of concepts
2. **Intermediate research** → Requires basic knowledge + new materials
3. **Advanced research** → Combines multiple prerequisites

### Multi-Stage Learning
Break complex topics into digestible parts:

**Page 1**: Basic concepts and theory  
**Page 2**: Practical applications  
**Page 3**: Advanced techniques  
**Page 4**: Expert-level mastery

## Quality Assurance

### Testing Checklist

#### Research Testing
- [ ] Research triggers correctly on intended actions
- [ ] All conditions work as expected  
- [ ] Task progression flows logically
- [ ] Rewards are granted properly
- [ ] Action bar notifications appear

#### Codex Testing  
- [ ] Entries appear in correct categories
- [ ] Target discovery works reliably
- [ ] All page types render correctly
- [ ] Translation keys resolve properly
- [ ] Images/entities display as intended

#### Integration Testing
- [ ] Research and codex entries complement each other
- [ ] Progressive unlocking works smoothly  
- [ ] No content conflicts or overwrites
- [ ] Performance remains stable with full content

### Debug Strategies

#### Isolate Issues
Test individual files before combining:

1. **Single research file** - Does it trigger and reward correctly?
2. **Single codex entry** - Does it appear and display properly?
3. **Combined systems** - Do they work together as intended?

#### Validate JSON
Use online JSON validators to catch syntax errors:
- Check bracket matching
- Verify comma placement  
- Confirm quote consistency
- Test nested structure validity

#### Check Logs
Monitor game logs for error messages:
- Mod loading issues
- JSON parsing errors
- Missing translation keys
- Resource loading failures

## Best Practices Summary

### Design Philosophy
1. **Start Simple** - Build complexity gradually
2. **Be Consistent** - Use similar patterns throughout  
3. **Think Progressively** - Each piece should lead to the next
4. **Test Thoroughly** - Verify everything works as intended

### Content Creation
1. **Plan Your Theme** - Know your magical focus before creating
2. **Create Templates** - Reuse successful patterns
3. **Document Everything** - Keep notes on your design decisions
4. **Iterate and Improve** - Refine based on testing and feedback

### Technical Execution  
1. **Follow Structure** - Use correct folder organization
2. **Validate Syntax** - Check JSON before testing
3. **Test Incrementally** - Add and test small pieces at a time
4. **Monitor Performance** - Watch for slowdowns or conflicts

### Community Compatibility
1. **Use Unique IDs** - Avoid conflicts with other content
2. **Respect Namespaces** - Don't override other mods' content  
3. **Document Dependencies** - List required mods clearly
4. **Provide Examples** - Help others understand your content

## Troubleshooting

### Common Issues

**Research not triggering**: Check trigger syntax and entity IDs  
**Codex not appearing**: Verify target syntax and file location  
**Translation missing**: Confirm lang file exists and keys match  
**NBT not matching**: Test NBT syntax in game first  
**Performance issues**: Reduce content complexity or optimize targeting,
      "Lore": [
        "{\"text\":\"A book of forbidden knowledge\",\"color\":\"gray\"}"
      ]
    }
  }
}
```

### Performance Optimization

#### Translation Caching
- **Preload common keys** - Cache frequently used translations
- **Batch loading** - Load related content together
- **Smart invalidation** - Only reload when necessary

#### Content Streaming
```json
{
  "lazy_load": true,
  "priority": "low",
  "cache_policy": "aggressive"
}
```

#### Memory Management
- **Reference sharing** - Reuse common page elements
- **Compression** - Minimize JSON file sizes
- **Smart indexing** - Quick content lookup

### Debugging Techniques

#### JSON Validation
```bash
# Use online validators or local tools
cat entry.json | python -m json.tool
```

#### In-Game Testing
```
/reload - Reload datapacks
/datapack list - Show loaded packs
/advancement grant @s everything - Unlock all research
```

#### Log Analysis
```
[INFO] Eidolon Unchained: Loading entry 'crystal_basics'
[WARN] Missing translation key: mod.entry.missing
[ERROR] Invalid recipe ID: invalid:recipe
```

## Advanced Content Patterns

### Research Progression Trees

```
Basic Studies → Intermediate → Advanced → Mastery
     ↓              ↓           ↓         ↓
  Zombies       Skeletons   Wraiths   Liches
  Spiders       Witches     Spirits   Archdevils
  Creepers      Endermen    Phantoms  Ancient Ones
```

### Thematic Content Packs

#### "Necromancer's Guide" Pack
```
├── fundamentals/
│   ├── death_magic_theory.json
│   └── soul_manipulation.json
├── creatures/
│   ├── undead_anatomy.json
│   └── spirit_summoning.json
├── rituals/
│   ├── basic_necromancy.json
│   └── advanced_binding.json
└── mastery/
    └── lichdom_research.json
```

#### "Crystal Sage" Pack
```
├── crystal_types/
├── resonance_theory/
├── crafting_techniques/
└── master_applications/
```

### Multi-Mod Integration

#### Recipe Cross-References
```json
{
  "type": "crafting",
  "recipe": "thermal:machine_frame",
  "fallback": "minecraft:iron_block"
}
```

#### Conditional Loading
```json
{
  "mod_requirements": ["thermal_expansion", "eidolon"],
  "content": "thermal_eidolon_integration.json"
}
```

## Planned Features

### 🚧 In Development

#### Enhanced Research System
- **Research Prerequisites** - Unlock chains
- **Team Research** - Multiplayer progression
- **Research Categories** - Organized discovery
- **Progress Tracking** - Partial completion states

#### Advanced Task Types
```json
{
  "type": "enter_dimension",
  "dimension": "minecraft:nether",
  "duration": 300
}
```

```json
{
  "type": "kill_entity",
  "entity": "eidolon:wraith",
  "count": 5,
  "conditions": ["nighttime", "raining"]
}
```

```json
{
  "type": "craft_item",
  "recipe": "eidolon:soul_gem",
  "count": 3
}
```

#### Smart Content Discovery
- **Adaptive Difficulty** - Content scales with player progress
- **Interest Tracking** - Show content based on player actions
- **Dynamic Prerequisites** - Requirements change based on world state

### 🔮 Future Releases

#### Visual Enhancements
- **Custom Page Layouts** - Advanced formatting options
- **Interactive Elements** - Clickable diagrams
- **Animation Support** - Moving illustrations
- **3D Models** - Custom entity displays

#### Advanced Integration
- **Spell Creation** - Custom spell definitions
- **Ritual Designer** - Visual ritual builder
- **Mod Bridge API** - Easy third-party integration
- **Resource Pack Support** - Custom textures and sounds

#### Content Management
- **Version Control** - Track content changes
- **Conflict Resolution** - Handle overlapping content
- **Dependency Management** - Automatic requirement checking
- **Distribution Platform** - Share content packs easily

### 📋 Community Requests

#### Quality of Life
- [ ] **Auto-completion** - JSON schema support
- [ ] **Live Preview** - See changes in real-time
- [ ] **Error Highlighting** - Better debugging tools
- [ ] **Content Templates** - Quick start options

#### Advanced Features
- [ ] **Scripting Support** - Lua/JavaScript integration
- [ ] **Database Backend** - Store content in databases
- [ ] **API Endpoints** - Web-based content management
- [ ] **Analytics** - Track content usage

## Community Contributions

### How to Contribute

#### Documentation
- **Examples** - Share working content packs
- **Tutorials** - Write guides for specific techniques
- **Translations** - Help with multi-language support
- **Bug Reports** - Report issues with detailed info

#### Code Contributions
- **Feature Requests** - Suggest new capabilities
- **Pull Requests** - Contribute code improvements
- **Testing** - Help verify new features
- **Integration** - Bridge with other mods

### Content Sharing

#### Distribution Platforms
- **CurseForge** - Official mod platform
- **Modrinth** - Modern distribution
- **GitHub** - Open source sharing
- **Discord** - Community hub

#### Best Practices
- **Clear Documentation** - How to install and use
- **Version Compatibility** - Which versions work
- **Screenshots** - Show your content in action
- **License Information** - Usage permissions

## Getting Help

### Resources
- **GitHub Issues** - Bug reports and feature requests
- **Discord Community** - Real-time help and discussion
- **Documentation Wiki** - Comprehensive guides
- **Example Repository** - Working code samples

### Common Support Questions

#### "My content isn't loading"
1. Check JSON syntax with validator
2. Verify file paths are correct
3. Ensure datapack is loaded (`/datapack list`)
4. Look for errors in game logs

#### "Translations aren't working"
1. Confirm language file location
2. Check translation key spelling
3. Use `%%` for literal percent signs
4. Verify pack is in assets folder

#### "Recipe pages show air"
1. Verify recipe ID exists
2. Check required mods are loaded
3. Ensure recipe type matches page type
4. Test recipe in crafting interface

## Version Roadmap

### Version 1.1 (Current)
- ✅ Translation system improvements
- ✅ Research auto-discovery
- ✅ 9 page types supported
- ✅ NBT data support

### Version 1.2 (Next)
- 🚧 Advanced research progression
- 🚧 Multi-language improvements
- 🚧 Performance optimizations
- 🚧 Enhanced debugging tools

### Version 1.3 (Planned)
- 📋 Visual content editor
- 📋 Spell creation system
- 📋 Advanced ritual support
- 📋 Third-party mod bridges

### Version 2.0 (Future)
- 🔮 Complete visual overhaul
- 🔮 Web-based management
- 🔮 Cloud content sharing
- 🔮 AI-assisted content creation

---

**Stay tuned for updates!** Follow the project on GitHub for the latest developments and feature announcements.
