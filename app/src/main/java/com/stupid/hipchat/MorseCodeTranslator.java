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

    public MorseCodeTranslator(Resources res) throws IOException {
        initDictionary(res);
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

    private void initDictionary(Resources res) throws IOException {
        try {
            dictionary = new HashMap<>();
            AssetManager assetManager = res.getAssets();
            InputStream is = assetManager.open(DICT_FILENAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                String[] words = line.split(" ");
                dictionary.put(words[1], words[0]);
            }
        } catch (IOException ioE) {
            throw new IOException("Unable to initialize morse code dictionary", ioE);
        }
    }

}
