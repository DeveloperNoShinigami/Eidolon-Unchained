package com.bluelotuscoding.eidolonunchained.api.condition;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import dev.latvian.mods.kubejs.typings.Info;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A reusable, composable test (Phase 4, spec §7.2). Made by {@code EidolonUnchained.conditions} ({@link Conditions});
 * the entity, item and player forms narrow themselves with chained calls. Every condition carries a readable
 * description (for logs, the codex and later the AI layer) and a predicate over an {@link EventContext}.
 */
public class Condition {
    protected final List<Predicate<EventContext>> parts = new ArrayList<>();
    protected final StringBuilder description;

    protected Condition(String description) {
        this.description = new StringBuilder(description);
    }

    public Condition(String description, Predicate<EventContext> test) {
        this(description);
        parts.add(test);
    }

    /** Adds a narrowing test (chained forms). */
    protected <T extends Condition> T narrow(String what, Predicate<EventContext> test) {
        parts.add(test);
        description.append(' ').append(what);
        @SuppressWarnings("unchecked") T self = (T) this;
        return self;
    }

    @Info("Does it hold for this player, entity, item or event context?")
    public boolean test(Object subject) {
        return matches(EventContext.of(subject));
    }

    public boolean matches(EventContext ctx) {
        try {
            for (var p : parts) if (!p.test(ctx)) return false;
            return true;
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("condition '{}' threw: {}", description, e.toString());
            return false;
        }
    }

    @Info("This and the other")
    public Condition and(Condition other) {
        return new Condition("(" + description + ") and (" + other.describe() + ")", ctx -> matches(ctx) && other.matches(ctx));
    }

    @Info("This or the other")
    public Condition or(Condition other) {
        return new Condition("(" + description + ") or (" + other.describe() + ")", ctx -> matches(ctx) || other.matches(ctx));
    }

    @Info("The opposite: C.dimension('minecraft:the_nether').not()")
    public Condition not() {
        return negate();
    }

    /** A condition from a script function, for builders that take either. */
    public static Condition of(Conditions.ContextTest fn) {
        return new Condition("script test", fn::test);
    }

    public Condition negate() {
        return new Condition("not (" + description + ")", ctx -> !matches(ctx));
    }

    public String describe() {
        return description.toString();
    }

    @Override
    public String toString() {
        return "Condition[" + describe() + "]";
    }
}
