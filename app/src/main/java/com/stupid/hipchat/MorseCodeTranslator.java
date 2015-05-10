package com.stupid.hipchat;

import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MorseCodeTranslator {

    private static final String DICT_FILENAME = "morse_code_dict";

    private Map<String, String> dictionary;
    private StringBuilder morseCodeCollection;

    public MorseCodeTranslator() {
        initDictionary();
        morseCodeCollection = new StringBuilder();
    }

    // @param morseCode '.' or '-'
    public void collect(char morseCode) {
        morseCodeCollection.append(morseCode);
    }

    public String getTranslation(boolean andClearMorseCollection) {
        String code = getMorseCollection();
        if (andClearMorseCollection) {
            clearMorseCollection();
        }
        return dictionary.get(code);
    }

    public String getMorseCollection() {
        return morseCodeCollection.toString();
    }

    public void clearMorseCollection() {
        morseCodeCollection = new StringBuilder();
    }

    private void initDictionary() {
        dictionary = new HashMap<>();
        dictionary.put(".-", "A");
        dictionary.put("-...", "B");
        dictionary.put("-.-.", "C");
        dictionary.put("-..", "D");
        dictionary.put(".", "E");
        dictionary.put("..-.", "F");
        dictionary.put("--.", "G");
        dictionary.put("....", "H");
        dictionary.put("..", "I");
        dictionary.put(".---", "J");
        dictionary.put("-.-", "K");
        dictionary.put(".-..", "L");
        dictionary.put("--", "M");
        dictionary.put("-.", "N");
        dictionary.put("---", "O");
        dictionary.put(".--.", "P");
        dictionary.put("--.-", "Q");
        dictionary.put(".-.", "R");
        dictionary.put("...", "S");
        dictionary.put("-", "T");
        dictionary.put("..-", "U");
        dictionary.put("...-", "V");
        dictionary.put(".--", "W");
        dictionary.put("-..-", "X");
        dictionary.put("-.--", "Y");
        dictionary.put("--..", "Z");
        dictionary.put("-----", "0");
        dictionary.put(".----", "1");
        dictionary.put("..---", "2");
        dictionary.put("...--", "3");
        dictionary.put("....-", "4");
        dictionary.put(".....", "5");
        dictionary.put("-....", "6");
        dictionary.put("--...", "7");
        dictionary.put("---..", "8");
        dictionary.put("----.", "9");
        dictionary.put(".-.-.-", ".");
        dictionary.put("--..--", ",");
        dictionary.put("---...", ":");
        dictionary.put("..--..", "?");
        dictionary.put(".----.", "'");
        dictionary.put("-....-", "-");
        dictionary.put("-..-.", "/");
        dictionary.put(".-..-.", "\"");
        dictionary.put(".--.-.", "@");
        dictionary.put("-...-", "=");
    }
}
