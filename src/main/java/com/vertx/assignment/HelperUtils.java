package com.vertx.assignment;


import org.apache.commons.lang3.StringUtils;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.NoSuchElementException;


class HelperUtils {

    private static final Random random = new Random();
    private static final int ASCII_VAL = 96;
    private static final int MIN_DISTANCE = 1;
    static Set<String> wordBase = new HashSet<>();

    /*Map keys are the summation of characters for (a=1, b=2, etc).
      Map values are Sets of strings with that total character value */
    static Map<Integer, Set<String>> charValueMap = new HashMap<>();


    /**
     * Iterate over chars in word and extract each char's Ascii value - 96
     * to compute total char value.
     * @param word: Word to compute char value for.
     * @return character value (int).
     * */
    static int computeCharactersValue(String word) {
        int charValue = 0;
        int length = word.length();
        for (int i = 0; i < length; i++) {
        /* Parameter validation is being handled before this function is called, thus
           making only possible chars being ascii lowercase & uppercase. */
        charValue += ((int) Character.toLowerCase(word.charAt(i)) - ASCII_VAL);
        }
        return charValue;
    }

    /**
     * Find a matching valued word (or closest possible) by character values.
     * from `charValueMap` HashMap (keys are Integers, values are Sets of strings).
     * Starts at the POST request param's char value and traverses up & down until it finds the closest matched key.
     * Lower stop point: charValue - margin = 0.
     * Upper stop point: charValue + margin > maxKey.
     *
     * @param charValue: character value to compare to.
     * @return A random word from Sets of strings whose corresponding key is an int
     * with a matching or closest value to @param `charValue`.
     * */
    static int getIndexOfMatchingValuedWordSet(int charValue) {
        int index = -1;
        int margin = 1;
        try {
            int maxKey = Collections.max(charValueMap.keySet());
            while (charValue - margin > 0 || charValue + margin <= maxKey) {
                int lowerIndex = charValue - margin;
                int upperIndex = charValue + margin;
                if (lowerIndex > 0 && charValueMap.get(lowerIndex) != null) {
                    index = lowerIndex;
                    break;
                } else if (upperIndex <= maxKey && charValueMap.get(upperIndex) != null) {
                    index = upperIndex;
                    break;
                }
                margin++;
            }
        }  catch (NoSuchElementException exception) {
            return -1;
        }
        return index;
    }

    /**
     * Cache word in wordBase, add word's char values as a key in charValueMap,
     * add word to that char value's Set.
     * @param word: Word sent in POST request.
     * @param charValues: Word's computed character values.
     *  */
    static void cacheWord(String word, int charValues) {
        wordBase.add(word);
        charValueMap.computeIfAbsent(charValues, k -> new HashSet<>());
        charValueMap.get(charValues).add(word);
    }

    /**
     * Returns random word from Set for key `charValue` if charValueMap contains that key, else null
     * @param charValue: Key to check if a Set exists for in charValueMap
     * @return Random string from Set of strings or null.
     * */
    static Object randomWordFromSet(int charValue) {
        Object word = null;
        Set wordSet = charValueMap.get(charValue);
        if (wordSet != null) {
        /* Unfortunately, couldn't find anything better than O(n) for getting a random element
           from a Set. O(n/2) on average =/  */
            int randElement = random.nextInt(wordSet.size());
            int i = 0;
            for (Object obj : wordSet) {
                if (i == randElement) {
                    word = obj;
                    break;
                }
                i++;
            }
        }
        return word;
    }

    /**
     * Iterate over a set of all words POSTed to the server so far and find the closest one.
     * to @param `word` by Levenshtein distance.
     * @param  word: Word to compare Levenshtein distance to.
     * @return Closest word if there are any, else null.
     * */
    static Object findClosestWord(String word) {
        /* I'm guessing less than MAX_VALUE will suffice, but if someone wants to send a word with
           a 100 Z's in it, he can ^_^ */
        int distance = Integer.MAX_VALUE;
        Object closest = null;
        for (String element : wordBase) {
            int currentDistance = StringUtils.getLevenshteinDistance(element, word);
            if (currentDistance < distance) {
                distance = currentDistance;
                closest = element;

            /*  If currentDistance <= 1, This is as close as it gets in Levenshtein distance
                (unless it is the same word, which I'm guessing is not a wanted result).
                We can return the current variable `closest`. No need to iterate further. */
                if (currentDistance <= MIN_DISTANCE) {
                    return closest;
                }
            }
        }
        return closest;
    }
}
