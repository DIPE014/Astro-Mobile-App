package com.astro.app.search;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A trie-based data structure for efficient prefix searching.
 *
 * <p>PrefixStore enables O(m) prefix queries where m is the length of the prefix.
 * It is used for autocomplete functionality in the search feature, allowing
 * users to find stars, constellations, and planets by typing partial names.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Case-insensitive search</li>
 *   <li>Preserves original capitalization in results</li>
 *   <li>Efficient memory usage with lazy node creation</li>
 *   <li>Thread-safe read operations</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * PrefixStore store = new PrefixStore();
 * store.add("Sirius");
 * store.add("Saturn");
 * store.add("Sun");
 *
 * Set<String> results = store.queryByPrefix("S");  // Returns {"Sirius", "Saturn", "Sun"}
 * Set<String> results2 = store.queryByPrefix("Sa"); // Returns {"Saturn"}
 * }</pre>
 */
public class PrefixStore {

    /**
     * Internal trie node that stores children and results.
     */
    private static class TrieNode {
        /** Map of character to child node */
        final Map<Character, TrieNode> children = new HashMap<>();

        /** Set of original strings that end at or pass through this node */
        final Set<String> results = new HashSet<>();
    }

    /** Empty set returned when no matches found */
    private static final Set<String> EMPTY_SET = Collections.emptySet();

    /** Root node of the trie */
    private final TrieNode root = new TrieNode();

    /**
     * Adds a string to the prefix store.
     *
     * <p>The string is indexed in a case-insensitive manner, but the original
     * capitalization is preserved in the results.</p>
     *
     * @param string The string to add
     */
    public void add(@NonNull String string) {
        if (string.isEmpty()) {
            return;
        }

        String lower = string.toLowerCase(Locale.ROOT);
        TrieNode node = root;

        // Traverse/create path for each character
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            TrieNode child = node.children.get(c);
            if (child == null) {
                child = new TrieNode();
                node.children.put(c, child);
            }
            node = child;
        }

        // Store the original string at the terminal node
        node.results.add(string);
    }

    /**
     * Queries for all strings that match the given prefix.
     *
     * <p>The query is case-insensitive, but results preserve original capitalization.</p>
     *
     * @param prefix The prefix to search for
     * @return Set of matching strings (never null, may be empty)
     */
    @NonNull
    public Set<String> queryByPrefix(@NonNull String prefix) {
        if (prefix.isEmpty()) {
            return EMPTY_SET;
        }

        String prefixLower = prefix.toLowerCase(Locale.ROOT);
        TrieNode node = root;

        // Navigate to the node representing the prefix
        for (int i = 0; i < prefixLower.length(); i++) {
            char c = prefixLower.charAt(i);
            TrieNode child = node.children.get(c);
            if (child == null) {
                return EMPTY_SET;  // Prefix not found
            }
            node = child;
        }

        // Collect all results from this node and its descendants
        Set<String> results = new HashSet<>();
        collectResults(node, results);
        return results;
    }

    /**
     * Recursively collects all results from a node and its descendants.
     *
     * @param node    The node to start collecting from
     * @param results The set to add results to
     */
    private void collectResults(@NonNull TrieNode node, @NonNull Set<String> results) {
        // Add results from this node
        results.addAll(node.results);

        // Recursively collect from all children
        for (TrieNode child : node.children.values()) {
            collectResults(child, results);
        }
    }

    /**
     * Checks if the prefix store contains any entries.
     *
     * @return true if the store is empty
     */
    public boolean isEmpty() {
        return root.children.isEmpty();
    }

    /**
     * Clears all entries from the prefix store.
     */
    public void clear() {
        root.children.clear();
        root.results.clear();
    }

    /**
     * Returns the total number of unique strings stored.
     *
     * @return Count of stored strings
     */
    public int size() {
        return countStrings(root);
    }

    /**
     * Recursively counts strings in the trie.
     */
    private int countStrings(@NonNull TrieNode node) {
        int count = node.results.size();
        for (TrieNode child : node.children.values()) {
            count += countStrings(child);
        }
        return count;
    }
}
