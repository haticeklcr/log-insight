package com.hatice.loginsight.exception;

/**
 * Hiçbir parser dosyayı güvenli şekilde işleyemediğinde fırlatılır —
 * PLAIN_TEXT her satırı "kabul ettiği" için bu, pratikte sadece dosyada
 * hiç anlamlı (boş olmayan) satır bulunamadığında oluşur. errorCode:
 * LOG_FORMAT_COULD_NOT_BE_DETECTED (job'a bu şekilde yansıtılacak, Faz 9).
 */
public class LogFormatCouldNotBeDetectedException extends RuntimeException {

    public LogFormatCouldNotBeDetectedException(String message) {
        super(message);
    }
}