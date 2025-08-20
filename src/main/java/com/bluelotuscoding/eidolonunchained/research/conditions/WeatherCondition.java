package com.bluelotuscoding.eidolonunchained.research.conditions;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Requires a specific weather state.
 */
public class WeatherCondition implements ResearchCondition {
    public enum WeatherType { CLEAR, RAIN, THUNDER }

    private final WeatherType weather;

    public WeatherCondition(String weather) {
        String w = weather.toLowerCase();
        if (w.equals("rain") || w.equals("raining")) {
            this.weather = WeatherType.RAIN;
        } else if (w.equals("thunder") || w.equals("thunderstorm")) {
            this.weather = WeatherType.THUNDER;
        } else {
            this.weather = WeatherType.CLEAR;
        }
    }

    public WeatherType getWeather() {
        return weather;
    }

    @Override
    public boolean test(Player player) {
        if (player == null) return true;
        Level level = player.level();
        return switch (weather) {
            case CLEAR -> !level.isRaining();
            case RAIN -> level.isRaining() && !level.isThundering();
            case THUNDER -> level.isThundering();
        };
    }
}
