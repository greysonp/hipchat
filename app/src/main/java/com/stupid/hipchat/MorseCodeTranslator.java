package com.stupid.hipchat;

import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MorseCodeTranslator {

    private static final String DICT_FILENAME = "morse_code_dict";

    private Map<String, String> dictionary;

    public MorseCodeTranslator(Resources res) throws IOException {
        initDictionary(res);
    }

    // Straight up translate a morse code string
    public String translate(String morseCode) {
        return dictionary.get(morseCode);
    }

    // @param morse '.' or '-'
    public void collect(char morse) {
    }

    // trigger translation from collected morse characters
    // + clear collection buffer
    public String getTranslation() {
        return null;
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
