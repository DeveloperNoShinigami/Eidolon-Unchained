# AI Command Execution Debugging System

## 🎯 **Overview**
The Enhanced Debugging System provides comprehensive tracking and monitoring of AI-executed commands in Eidolon Unchained. Server administrators can now monitor what commands the AI is running, why it's running them, and whether they succeed or fail.

## 🔧 **Features**

### **1. Automatic Command Logging**
- Every command executed by AI deities is automatically logged
- Success/failure status tracked
- Error messages captured for failed commands
- All data stored in world data for persistence

### **2. AI Decision Tracking**
- Logs WHY the AI decided to run commands
- Captures reputation, health, and context factors
- Records the decision-making process for analysis

### **3. Real-time Debugging Commands**
- View live command execution history
- Generate statistical reports
- Monitor AI behavior patterns

## 🎮 **Debug Commands**

### **View Command History**
```
/eidolon-unchained debug commands <player> <deity>
```
Shows the last 10 command executions for a specific player-deity interaction.

**Example:**
```
/eidolon-unchained debug commands Steve eidolonunchained:nature_deity
```

### **Generate Command Report**
```
/eidolon-unchained debug report <player> <deity>
```
Generates statistical analysis of command success rates and AI decision patterns.

**Example:**
```
/eidolon-unchained debug report Steve eidolonunchained:nature_deity
```

### **Toggle Debug Mode**
```
/eidolon-unchained debug toggle
```
Enables/disables enhanced debug logging throughout the system.

## 📊 **What Gets Logged**

### **Command Execution Events:**
- ✅ **SUCCESS**: `give Steve minecraft:diamond 1` → Successfully granted diamond
- ❌ **FAILURE**: `invalid_command` → Unknown command error
- 🧠 **AI_DECISION**: Auto-judgment triggered (Reputation: 25, Health: 15) → [give, effect]

### **Example Debug Output:**
```
§6=== Command Execution History ===
§ePlayer: Steve
§eDeity: eidolonunchained:nature_deity

§a[14:32:15] SYSTEM_DEBUG: COMMAND_EXECUTION: give Steve minecraft:bread 2 | SUCCESS: true | RESULT: Command executed successfully
§b[14:32:15] AI_DECISION: TYPE: AUTO_JUDGMENT | REASONING: Reputation: 15, Health: 8 | COMMANDS: give Steve minecraft:bread 2, effect give Steve minecraft:regeneration 30 1
§a[14:32:16] SYSTEM_DEBUG: COMMAND_EXECUTION: effect give Steve minecraft:regeneration 30 1 | SUCCESS: true | RESULT: Command executed successfully
```

## 📈 **Statistical Reports**

Reports provide insights into AI behavior:
- **Total Commands**: How many commands AI has executed
- **Success Rate**: Percentage of commands that worked
- **AI Decisions**: How often AI intervenes automatically
- **Failure Analysis**: What commands are failing and why

**Example Report:**
```
§6=== Command Execution Report ===
§ePlayer: Steve
§eDeity: eidolonunchained:nature_deity
§aTotal Commands: 15
§2Successful: 13
§cFailed: 2
§bAI Decisions: 5
§fSuccess Rate: 86.7%
```

## 🗂️ **Data Storage**

All debugging data is stored in the world's saved data:
- **Location**: `world/data/eidolon_unchained_conversations.dat`
- **Persistence**: Survives server restarts
- **Integration**: Part of conversation history system
- **Cleanup**: Respects message limits (default 1000 per deity)

## 🛠️ **For Server Administrators**

### **Monitoring AI Behavior:**
1. Enable debug mode: `/eidolon-unchained debug toggle`
2. Monitor specific players: `/eidolon-unchained debug commands <player> <deity>`
3. Check success rates: `/eidolon-unchained debug report <player> <deity>`
4. Review server logs for detailed command execution

### **Troubleshooting Failed Commands:**
1. Check command history for error messages
2. Verify command syntax in AI configurations
3. Ensure deity has proper permissions
4. Review placeholder replacements (`{player}`, `{x}`, etc.)

### **Performance Monitoring:**
- Debug entries are automatically limited by conversation history settings
- Older debug entries are cleaned up with normal conversation cleanup
- No significant performance impact on normal gameplay

## 🔍 **Log File Analysis**

Check `latest.log` for detailed information:
- `✅ AI Command Success` - Successful command executions
- `❌ AI Command Failed` - Failed command executions with error details
- `🧠 AI Decision` - AI decision-making process logs

## 🎯 **Use Cases**

### **Debugging AI Misbehavior:**
- AI giving too many/few items? Check decision logs
- Commands failing? Check execution history
- Players complaining about unfair treatment? Review statistical reports

### **Balancing AI Responses:**
- Monitor success rates to identify problematic commands
- Analyze AI decision patterns to adjust thresholds
- Track player-specific interactions for personalized balancing

### **Server Administration:**
- Verify AI is working as intended
- Identify and fix broken command configurations
- Monitor for potential abuse or exploitation

## 🚀 **Integration with Hybrid Configuration**

The debugging system integrates seamlessly with the hybrid configuration system:
- JSON configurations provide baseline debugging settings
- Server overrides can adjust logging levels
- World data stores all debugging information
- No external files needed for debug data

This debugging system provides complete visibility into AI command execution while maintaining the performance and ease-of-use of the overall Eidolon Unchained system! 🎮✨
