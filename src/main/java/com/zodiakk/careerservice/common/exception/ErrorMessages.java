package com.zodiakk.careerservice.common.exception;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ErrorMessages {

    public static final String CONCURRENT_MODIFICATION = "Объект был изменён другим пользователем. Обновите данные и повторите попытку";

    public static final String INTERNAL_SERVER_ERROR = "Внутренняя ошибка сервера. Попробуйте позднее.";

    public static final String DATA_INTEGRITY_VIOLATION = "Нарушено ограничение целостности данных";

    public static final String INVALID_REQUEST_BODY = "Некорректное тело запроса";

    public static final String INVALID_PARAMETER = "Некорректное значение параметра: %s";

    public static final String MISSING_PARAMETER = "Обязательный параметр отсутствует: %s";

    public static final String PAYLOAD_TOO_LARGE = "Размер загружаемого файла превышает допустимый";

    public static final String UNSUPPORTED_MEDIA_TYPE = "Неподдерживаемый тип содержимого запроса";

    public static final String ACCESS_DENIED = "Доступ запрещён";

    public static final String UNAUTHORIZED = "Требуется аутентификация";

    public static final String INVALID_INTERNAL_API_KEY = "Некорректный внутренний API-ключ";
}
