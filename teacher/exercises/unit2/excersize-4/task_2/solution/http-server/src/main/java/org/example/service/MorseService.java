package org.example.service;

import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class MorseService {
    private static final Map<Character, String> ENCODE = Map.ofEntries(
            entry('А', ".-"), entry('Б', "-..."), entry('В', ".--"),
            entry('Г', "--."), entry('Д', "-.."), entry('Е', "."),
            entry('Ж', "...-"), entry('З', "--.."), entry('И', ".."),
            entry('Й', ".---"), entry('К', "-.-"), entry('Л', ".-.."),
            entry('М', "--"), entry('Н', "-."), entry('О', "---"),
            entry('П', ".--."), entry('Р', ".-."), entry('С', "..."),
            entry('Т', "-"), entry('У', "..-"), entry('Ф', "..-."),
            entry('Х', "...."), entry('Ц', "-.-."), entry('Ч', "---."),
            entry('Ш', "----"), entry('Щ', "--.-"), entry('Ъ', "--.--"),
            entry('Ь', "-..-"), entry('Ю', "..--"), entry('Я', ".-.-"),
            entry('0', "-----"), entry('1', ".----"), entry('2', "..---"),
            entry('3', "...--"), entry('4', "....-"), entry('5', "....."),
            entry('6', "-...."), entry('7', "--..."), entry('8', "---.."),
            entry('9', "----."), entry(',', "--..--"), entry('?', "..--.."),
            entry('.', ".-.-.-")
    );
    private static final Map<String, Character> DECODE = ENCODE.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

    public String encode(String text) {
        requireInput(text);
        StringJoiner words = new StringJoiner(" / ");
        for (String word : text.toUpperCase(Locale.ROOT).split(" ", -1)) {
            if (word.isEmpty()) {
                throw new IllegalArgumentException("Separate words with a single space; no surrounding spaces");
            }
            StringJoiner letters = new StringJoiner(" ");
            for (char letter : word.toCharArray()) {
                String code = ENCODE.get(letter);
                if (code == null) {
                    throw new IllegalArgumentException("Unsupported character: " + letter);
                }
                letters.add(code);
            }
            words.add(letters.toString());
        }
        return words.toString();
    }

    public String decode(String morse) {
        requireInput(morse);
        StringJoiner words = new StringJoiner(" ");
        for (String word : morse.split(" / ", -1)) {
            StringBuilder letters = new StringBuilder();
            for (String token : word.split(" ", -1)) {
                Character letter = DECODE.get(token);
                if (letter == null) {
                    throw new IllegalArgumentException("Unknown or malformed Morse token: " + token
                            + ". Use single spaces between letters and ' / ' between words");
                }
                letters.append(letter);
            }
            words.add(letters.toString());
        }
        return words.toString();
    }

    private void requireInput(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input must not be blank");
        }
    }
}
