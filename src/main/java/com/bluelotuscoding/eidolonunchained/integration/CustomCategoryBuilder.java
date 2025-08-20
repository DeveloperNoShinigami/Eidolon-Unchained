package com.bluelotuscoding.eidolonunchained.integration;

import elucent.eidolon.codex.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder class to make creating custom categories and chapters easier
 */
public class CustomCategoryBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private String key;
    private ItemStack icon;
    private int color;
    private List<ChapterEntry> chapters = new ArrayList<>();
    
    public static class ChapterEntry {
        public final Chapter chapter;
        public final ItemStack icon;
        
        public ChapterEntry(Chapter chapter, ItemStack icon) {
            this.chapter = chapter;
            this.icon = icon;
        }
    }
    
    public CustomCategoryBuilder(String key) {
        this.key = key;
    }
    
    public CustomCategoryBuilder icon(ItemStack icon) {
        this.icon = icon;
        return this;
    }
    
    public CustomCategoryBuilder color(int color) {
        this.color = color;
        return this;
    }
    
    /**
     * Add a chapter with its icon to this category
     */
    public CustomCategoryBuilder addChapter(String titleKey, String fallbackTitle, ItemStack chapterIcon) {
        Chapter chapter = new Chapter(titleKey);
        
        // Add default pages
        chapter.addPage(new TitlePage(fallbackTitle));
        chapter.addPage(new TextPage("This chapter was created by Eidolon Unchained."));
        
        chapters.add(new ChapterEntry(chapter, chapterIcon));
        return this;
    }
    
    /**
     * Add a pre-built chapter to this category
     */
    public CustomCategoryBuilder addChapter(Chapter chapter, ItemStack chapterIcon) {
        chapters.add(new ChapterEntry(chapter, chapterIcon));
        return this;
    }
    
    /**
     * Build and return the category, also registering items in the lookup map
     */
    public Category build() {
        if (chapters.isEmpty()) {
            LOGGER.warn("Building category '{}' with no chapters!", key);
        }
        
        // Create index entries
        IndexPage.IndexEntry[] entries = new IndexPage.IndexEntry[chapters.size()];
        for (int i = 0; i < chapters.size(); i++) {
            ChapterEntry entry = chapters.get(i);
            entries[i] = new IndexPage.IndexEntry(entry.chapter, entry.icon);
        }
        
        // Create index page
        Index index = new Index("eidolonunchained.codex.category." + key, new IndexPage(entries));
        
        // Create and return category
        Category category = new Category(key, icon, color, index);
        
        LOGGER.info("✅ Built category '{}' with {} chapters", key, chapters.size());
        return category;
    }
    
    /**
     * Quick method to create a chapter with common pages
     */
    public static Chapter createChapterWithPages(String titleKey, String fallbackTitle, String... textPages) {
        Chapter chapter = new Chapter(titleKey);
        
        chapter.addPage(new TitlePage(fallbackTitle));
        
        for (String text : textPages) {
            chapter.addPage(new TextPage(text));
        }
        
        return chapter;
    }
}
