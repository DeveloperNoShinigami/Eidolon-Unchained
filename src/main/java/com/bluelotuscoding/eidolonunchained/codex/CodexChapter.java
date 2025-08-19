package com.bluelotuscoding.eidolonunchained.codex;

import java.util.ArrayList;
import java.util.List;

public class CodexChapter {
    private final String key;
    private final List<CodexEntry> entries = new ArrayList<>();

    public CodexChapter(String key) {
        this.key = key;
    }

    public String getKey() { return key; }
    public List<CodexEntry> getEntries() { return entries; }
    public void addEntry(CodexEntry entry) { entries.add(entry); }
}
