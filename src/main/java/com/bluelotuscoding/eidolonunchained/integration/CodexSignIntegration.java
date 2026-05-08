package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.DatapackSignManager;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.codex.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.*;

/**
 * Registers datapack-loaded Signs into Eidolon's codex as a new "Datapack Signs" category.
 *
 * <p>Each sign gets:
 * <ul>
 *   <li>A detail Chapter containing a TitlePage + SignPage</li>
 *   <li>An entry tile in a SignIndexPage grid (2 columns, up to 6 per page)</li>
 * </ul>
 * The entire index is wrapped in a new Category added to {@link CodexChapters#categories}.
 */
@OnlyIn(Dist.CLIENT)
public class CodexSignIntegration {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SIGNS_PER_PAGE = 6;

    public static void registerSigns() {
        Map<ResourceLocation, Sign> loadedSigns = DatapackSignManager.getLoadedSigns();
        if (loadedSigns.isEmpty()) {
            LOGGER.info("No datapack signs to register in codex");
            return;
        }

        LOGGER.info("Registering {} datapack sign(s) with codex", loadedSigns.size());

        // Sort for deterministic ordering
        List<Map.Entry<ResourceLocation, Sign>> sorted = new ArrayList<>(loadedSigns.entrySet());
        sorted.sort(Comparator.comparing(e -> e.getKey().toString()));

        // Build a detail Chapter per sign
        Map<ResourceLocation, Chapter> detailChapters = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Sign> entry : sorted) {
            ResourceLocation id = entry.getKey();
            Sign sign = entry.getValue();
            String titleKey = "eidolonunchained.sign." + id.getPath();
            Chapter detail = new Chapter(titleKey, new TitlePage(titleKey), new SignPage(sign));
            detailChapters.put(id, detail);
        }

        // Chunk into groups of SIGNS_PER_PAGE → one SignIndexPage per chunk
        List<SignIndexPage> indexPages = new ArrayList<>();
        List<Map.Entry<ResourceLocation, Sign>> entries = new ArrayList<>(detailChapters.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), loadedSigns.get(e.getKey())))
                .toList());

        for (int i = 0; i < entries.size(); i += SIGNS_PER_PAGE) {
            int end = Math.min(i + SIGNS_PER_PAGE, entries.size());
            SignIndexPage.SignEntry[] signEntries = new SignIndexPage.SignEntry[end - i];
            for (int j = i; j < end; j++) {
                ResourceLocation id = entries.get(j).getKey();
                Sign sign = entries.get(j).getValue();
                signEntries[j - i] = new SignIndexPage.SignEntry(detailChapters.get(id), sign);
            }
            indexPages.add(new SignIndexPage(signEntries));
        }

        // Build the Index chapter (top-level entry point for this category)
        Index signsIndex = new Index("eidolonunchained.codex.signs",
                indexPages.toArray(new SignIndexPage[0]));

        // Build the Category and add it directly to CodexChapters.categories
        Category signsCategory = new Category(
                "eidolonunchained_signs",
                new ItemStack(Items.ENCHANTED_BOOK),
                0xFF6A3DCC,
                signsIndex
        );

        CodexChapters.categories.add(signsCategory);
        LOGGER.info("Datapack Signs category added to codex ({} sign(s), {} page(s))",
                loadedSigns.size(), indexPages.size());
    }
}
