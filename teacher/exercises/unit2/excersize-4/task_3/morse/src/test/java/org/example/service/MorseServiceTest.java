package org.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MorseServiceTest {
    private static final String ALPHABET = "абвгдежзийклмнопрстуфхцчшщъьюя";
    private static final String ALPHABET_MORSE =
            ".- -... .-- --. -.. . ...- --.. .. .--- -.- .-.. -- -. --- "
                    + ".--. .-. ... - ..- ..-. .... -.-. ---. ---- --.- --.-- -..- ..-- .-.-";
    private static final String DIGITS_MORSE =
            "----- .---- ..--- ...-- ....- ..... -.... --... ---.. ----.";

    private final MorseService service = new MorseService();

    static Stream<Arguments> symbols() {
        return Stream.of(
                Arguments.of("А", ".-"),
                Arguments.of("Б", "-..."),
                Arguments.of("В", ".--"),
                Arguments.of("Г", "--."),
                Arguments.of("Д", "-.."),
                Arguments.of("Е", "."),
                Arguments.of("Ж", "...-"),
                Arguments.of("З", "--.."),
                Arguments.of("И", ".."),
                Arguments.of("Й", ".---"),
                Arguments.of("К", "-.-"),
                Arguments.of("Л", ".-.."),
                Arguments.of("М", "--"),
                Arguments.of("Н", "-."),
                Arguments.of("О", "---"),
                Arguments.of("П", ".--."),
                Arguments.of("Р", ".-."),
                Arguments.of("С", "..."),
                Arguments.of("Т", "-"),
                Arguments.of("У", "..-"),
                Arguments.of("Ф", "..-."),
                Arguments.of("Х", "...."),
                Arguments.of("Ц", "-.-."),
                Arguments.of("Ч", "---."),
                Arguments.of("Ш", "----"),
                Arguments.of("Щ", "--.-"),
                Arguments.of("Ъ", "--.--"),
                Arguments.of("Ь", "-..-"),
                Arguments.of("Ю", "..--"),
                Arguments.of("Я", ".-.-"),
                Arguments.of("0", "-----"),
                Arguments.of("1", ".----"),
                Arguments.of("2", "..---"),
                Arguments.of("3", "...--"),
                Arguments.of("4", "....-"),
                Arguments.of("5", "....."),
                Arguments.of("6", "-...."),
                Arguments.of("7", "--..."),
                Arguments.of("8", "---.."),
                Arguments.of("9", "----."),
                Arguments.of(",", "--..--"),
                Arguments.of("?", "..--.."),
                Arguments.of(".", ".-.-.-")
        );
    }

    @ParameterizedTest(name = "{0} encodes as {1}")
    @MethodSource("symbols")
    void encodesEverySupportedSymbolInEitherCase(String text, String expected) {
        assertEquals(expected, service.encode(text));
        assertEquals(expected, service.encode(text.toLowerCase(Locale.ROOT)));
    }

    @ParameterizedTest(name = "{1} decodes as {0}")
    @MethodSource("symbols")
    void decodesEverySupportedSymbol(String expected, String morse) {
        assertEquals(expected, service.decode(morse));
    }

    @Test
    void handlesTheWholeAlphabetWithExactlyOneSpaceBetweenLetters() {
        assertEquals(ALPHABET_MORSE, service.encode(ALPHABET));
        assertEquals(ALPHABET_MORSE, service.encode(ALPHABET.toUpperCase(Locale.ROOT)));
        assertEquals(ALPHABET.toUpperCase(Locale.ROOT), service.decode(ALPHABET_MORSE));
    }

    @Test
    void handlesAllDigitsAndPunctuationWithoutWordSeparators() {
        String morse = DIGITS_MORSE + " --..-- ..--.. .-.-.-";
        assertEquals(morse, service.encode("0123456789,?."));
        assertEquals("0123456789,?.", service.decode(morse));
    }

    @Test
    void preservesWordBoundariesAndNormalizesMixedCase() {
        String morse = ".- --.. / -.-. .- .-. --.-- - / ----- .---- --..-- / -.. .- ..--.. .-.-.-";
        assertEquals(morse, service.encode("Аз ЦаРъТ 01, Да?."));
        assertEquals("АЗ ЦАРЪТ 01, ДА?.", service.decode(morse));
    }

    @Test
    void distinguishesHardSignFromSoftSignAndSimilarLetters() {
        String morse = "--.-- -..- --.- --.. .-.- ..--";
        assertEquals(morse, service.encode("ЪЬЩЗЯЮ"));
        assertEquals("ЪЬЩЗЯЮ", service.decode(morse));
    }

    @Test
    @ResourceLock("java.util.Locale.default")
    void normalizationDoesNotDependOnTheDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(".--- --.-- --.. .-.-", service.encode("ЙъЗя"));
            assertEquals("ЙЪЗЯ", service.decode(".--- --.-- --.. .-.-"));
            assertThrows(IllegalArgumentException.class, () -> service.encode("I"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "   ", "\t", "\n", "\r\n", "\u00a0", "\u2003",
            " а", "а ", " а ", "а  б", "а   б",
            "а\tб", "а\nб", "а\rб", "а\r\nб", "а\fб", "а\u000bб",
            "а\u00a0б", "а\u2003б", "а\u202fб", "а\u200bб", "\ufeffа",
            "A", "hello", "аAб", "а!", "а:б", "а;б", "а-б", "а/б",
            "а_б", "а@б", "а🙂б", "ы", "э", "ё", "ѝ", "и\u0300", "а\u0000б"
    })
    void rejectsBlankUnsupportedAndImproperlySpacedPlainText(String text) {
        assertThrows(IllegalArgumentException.class, () -> service.encode(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "   ", "\t", "\n", "\r\n", "\u00a0", "\u2003",
            " .-", ".- ", " .- ", ".-  -...", ".-   -...",
            ".-\t-...", ".-\n-...", ".-\r-...", ".-\r\n-...", ".-\f-...",
            ".-\u000b-...", ".-\u00a0-...", ".-\u2003-...", ".-\u202f-...",
            ".-\u200b-...", "\ufeff.-",
            "/", " / ", "/ .-", ".- /", " / .-", ".- / ",
            ".-/-...", ".- /-...", ".-/ -...", ".-  / -...", ".- /  -...",
            ".- // -...", ".- / / -...", ".- /  / -...",
            "......", "------", ".- ......", "...... / .-",
            ".-x", "а", ".- -x", "·−", ".−", ".—", ".- !", ".-\u0000"
    })
    void rejectsBlankUnknownAndMalformedMorseWithoutStripping(String morse) {
        assertThrows(IllegalArgumentException.class, () -> service.decode(morse));
    }
}
