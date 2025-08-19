package com.bluelotuscoding.eidolonunchained.codex;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class CodexCategory {
    private final String key;
    private final ItemStack icon;
    private final int color;
    private final List<CodexChapter> chapters = new ArrayList<>();

    public CodexCategory(String key, ItemStack icon, int color) {
        this.key = key;
        this.icon = icon;
        this.color = color;
    }

    public String getKey() { return key; }
    public ItemStack getIcon() { return icon; }
    public int getColor() { return color; }
    public List<CodexChapter> getChapters() { return chapters; }
    public void addChapter(CodexChapter chapter) { chapters.add(chapter); }
}
