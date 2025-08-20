package com.bluelotuscoding.eidolonunchained.research.tasks;

import com.bluelotuscoding.eidolonunchained.research.conditions.WeatherCondition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Task requiring a particular weather state.
 */
public class WeatherTask extends ResearchTask {
    private final WeatherCondition.WeatherType weather;

    public WeatherTask(String weather) {
        super(TaskType.WEATHER);
        String w = weather.toLowerCase();
        if (w.equals("rain") || w.equals("raining")) {
            this.weather = WeatherCondition.WeatherType.RAIN;
        } else if (w.equals("thunder") || w.equals("thunderstorm")) {
            this.weather = WeatherCondition.WeatherType.THUNDER;
        } else {
            this.weather = WeatherCondition.WeatherType.CLEAR;
        }
    }

    public WeatherCondition.WeatherType getWeather() {
        return weather;
    }

    @Override
    public boolean isComplete(Player player) {
        if (player == null) return false;
        Level level = player.level();
        return switch (weather) {
            case CLEAR -> !level.isRaining();
            case RAIN -> level.isRaining() && !level.isThundering();
            case THUNDER -> level.isThundering();
        };
    }
}

