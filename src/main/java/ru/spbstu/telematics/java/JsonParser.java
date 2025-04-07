package ru.spbstu.telematics.java;

import java.io.File;
import java.lang.reflect.*;
//import java.time.Instant;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

class JsonException extends Exception {
    public JsonException(String message) {
        super(message);
    }
}
class InvalidJsonStringException extends JsonException  {
    public InvalidJsonStringException(String message) {
        super(message);
    }
}
class InvalidPairParsingException extends JsonException  {
    public InvalidPairParsingException(String message) {
        super(message);
    }
}
class MappingException extends JsonException {
    public MappingException(String message) {
        super(message);
    }
}
class TypeMismatchException extends JsonException
{
    public TypeMismatchException(String message)
    {
        super(message);
    }
}
 class FieldNotFoundException extends JsonException {
    public FieldNotFoundException(String s) {
        super(s);
    }
}
class JsonSerializationException extends JsonException {
    public JsonSerializationException(String message) {
        super(message);
    }
}
public class JsonParser {

    // Кэшированные шаблоны для чисел
    private static final Pattern INT_PATTERN = Pattern.compile("-?\\d+");
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("-?\\d+\\.\\d+");

    public static <T> T parseStringToClass(String input, Class<T> targetClass) throws JsonException,
            IllegalArgumentException{
        try{
            Objects.requireNonNull(input, "Input JSON string cannot be null");
            Objects.requireNonNull(targetClass, "Target class cannot be null");
        }catch (NullPointerException e) {
            throw new IllegalArgumentException(e);
        }

        String trimmedInput = input.strip();
        try {
            if (trimmedInput.startsWith("[")) {
                // Обработка массивов
                if (targetClass.isArray()) {
                    List<Object> list = parseStringToList(trimmedInput);
                    return convertListToArray(list, targetClass.getComponentType());
                }
                // Обработка коллекций
                else if (Iterable.class.isAssignableFrom(targetClass)) {
                    return (T) parseStringToList(trimmedInput);
                }
                throw new JsonException("Target class " + targetClass.getName() +
                        " is not an array or Iterable");
            }
            //Обработка объектов
            if (trimmedInput.startsWith("{")) {
                Map<String, Object> parsedMap = parseStringToMap(trimmedInput);
                return setFieldsInClass(parsedMap, targetClass);
            }
            // Обработка примитивных значений
            if (isSimpleValueType(targetClass)) {
                return parseSimpleValue(trimmedInput, targetClass);
            }
            throw new JsonException("Invalid JSON format for target type " +
                    targetClass.getName());
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("Parsing failed");
        }
    }


    public static String parseToString(Object sourceObject) throws IllegalArgumentException {
        if (sourceObject == null) {
            return "null";
        }

        // Обработка строк
        if (sourceObject instanceof String) {
            return "\"" + escapeJson((String) sourceObject) + "\"";
        }

        // Обработка чисел и boolean
        if (sourceObject instanceof Number || sourceObject instanceof Boolean) {
            return sourceObject.toString();
        }

        // Обработка enum
        if (sourceObject.getClass().isEnum()) {
            return "\"" + ((Enum<?>) sourceObject).name() + "\"";
        }

        // Обработка массивов
        if (sourceObject.getClass().isArray()) {
            return arrayToJsonString(sourceObject);
        }

        // Обработка коллекций
        if (sourceObject instanceof Collection) {
            return collectionToJsonString((Collection<?>) sourceObject);
        }

        // Обработка Map
        if (sourceObject instanceof Map) {
            return mapToJsonString((Map<?, ?>) sourceObject);
        }

        // Обработка обычных объектов
        return objectToJsonString(sourceObject);
    }

    private static String arrayToJsonString(Object array) {
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(parseToString(Array.get(array, i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String collectionToJsonString(Collection<?> collection) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(parseToString(item));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String mapToJsonString(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
            sb.append(parseToString(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String objectToJsonString(Object obj) {
        Map<String, Object> fieldMap = collectAllFields(obj);
        return mapToJsonString(fieldMap);
    }


    private static <T> Map<String, Object> collectAllFields(T sourceObject) {
        Map<String, Object> map = new LinkedHashMap<>(); // Для сохранения порядка
        Class<?> currentClass = sourceObject.getClass();

        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(Modifier.isStatic(field.getModifiers()) ? null : sourceObject);
                    if (value != null) {
                        map.put(field.getName(), value);
                    }
                } catch (IllegalAccessException e) {
                    continue;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return map;
    }

    private static String buildJsonString(Map<String, Object> map) {
        StringBuilder jsonBuilder = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                jsonBuilder.append(",");
            }
            first = false;

            jsonBuilder.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();

            if (value == null) {
                jsonBuilder.append("null");
            } else if (value instanceof String) {
                jsonBuilder.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                jsonBuilder.append(value);
            } else {
                // Рекурсивная обработка вложенных объектов
                jsonBuilder.append(objectToJsonString(value));
            }
        }

        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

// Вспомогательные методы:
    private static <T> T convertListToArray(List<Object> list, Class<?> componentType)
            throws JsonException, ClassNotFoundException, NoSuchFieldException {
        Object array = Array.newInstance(componentType, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, convertValue(list.get(i), componentType));
        }
        @SuppressWarnings("unchecked")
        T result = (T) array;
        return result;
    }

    private static boolean isSimpleValueType(Class<?> type) {
        return type == String.class || type.isPrimitive() ||
                Number.class.isAssignableFrom(type) ||
                type == Boolean.class || type.isEnum();
    }

    private static <T> T parseSimpleValue(String value, Class<T> targetType)
            throws JsonException {
        Object parsed = parseValue(value);
        // Специальная обработка для enum
        if (targetType.isEnum()) {
            try {
                // Удаляем кавычки если они есть
                String enumValue = value.startsWith("\"") && value.endsWith("\"")
                        ? value.substring(1, value.length() - 1)
                        : value;
                return (T) Enum.valueOf((Class<Enum>) targetType, enumValue);
            } catch (IllegalArgumentException e) {
                throw new JsonException("Invalid enum value: " + value);
            }
        }
        if (targetType.isInstance(parsed)) {
            return targetType.cast(parsed);
        }
        throw new JsonException("Value " + parsed + " cannot be converted to " +
                targetType.getName());
    }
    public static <T> T setFieldsInClass(Map<String, Object> data, Class<T> targetClass) throws MappingException {
        try {

            T instance = createInstance(targetClass);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                Field field = findField(targetClass, fieldName);
                field.setAccessible(true);
                // Обработка коллекций
                if (value instanceof Collection && !Collection.class.isAssignableFrom(field.getClass()) && !field.getType().isArray()) {
                    value = convertValue(value, getGenericType(field));
                }
                else{
                    value = convertValue(value, field.getType());
                }

                field.set(instance, value);

            }
            return instance;
        } catch (ReflectiveOperationException | IllegalArgumentException | TypeMismatchException |
                 FieldNotFoundException e) {
            throw new MappingException("Failed to map field: " + e.getMessage());
        }
        catch (JsonException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Class<?>> findSubclasses(Type parentType, String packageName) {
        List<Class<?>> subclasses = new ArrayList<>();

        // Получаем raw type (если parentType является ParameterizedType)
        Class<?> parentClass = getRawType(parentType);

        if (parentClass == null) {
            throw new IllegalArgumentException("Invalid parent type provided");
        }

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());

                if (directory.exists()) {
                    for (File file : directory.listFiles()) {
                        if (file.getName().endsWith(".class")) {
                            String className = packageName + '.' +
                                    file.getName().substring(0, file.getName().length() - 6);
                            try {
                                Class<?> clazz = Class.forName(className);

                                if (parentClass.isAssignableFrom(clazz) && !clazz.equals(parentClass)) {
                                    subclasses.add(clazz);
                                }
                            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                                // Пропускаем классы, которые не могут быть загружены
                                continue;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return subclasses;
    }

    // Вспомогательный метод для получения raw type из Type
    private static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getRawType(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            // Для массивов возвращаем Object.class (можно изменить при необходимости)
            return Object.class;
        } else if (type instanceof TypeVariable<?>) {
            // Для type variables возвращаем верхнюю границу
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length > 0 ? getRawType(bounds[0]) : Object.class;
        } else if (type instanceof WildcardType) {
            // Для wildcard возвращаем верхнюю границу
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            return upperBounds.length > 0 ? getRawType(upperBounds[0]) : Object.class;
        }
        return null;
    }

    private static Class<?> getGenericType(Field field) throws JsonException {
        Type genericType = field.getGenericType();

        // Если тип не параметризованный, возвращаем Object.class
        if (!(genericType instanceof ParameterizedType)) {
            return Object.class;
        }

        ParameterizedType pType = (ParameterizedType) genericType;
        Type[] actualTypeArguments = pType.getActualTypeArguments();


        // Берем первый аргумент (для List<T> это будет T)
        if (actualTypeArguments.length == 0) {
            return Object.class;
        }

        Type actualType = actualTypeArguments[0];
        // Получаем raw type (если parentType является ParameterizedType)
        Class<?> parentClass = getRawType(actualType);

        // Обрабатываем разные варианты Type
        if (actualType instanceof Class) {
            return (Class<?>) actualType;
        } else if (actualType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) actualType).getRawType();
            if (rawType instanceof Class) {
                return (Class<?>) rawType;
            }
        } else if (actualType instanceof WildcardType) {
            // Обработка wildcard-типов (? extends SomeClass)
            Type[] upperBounds = ((WildcardType) actualType).getUpperBounds();
            if (upperBounds.length > 0 && upperBounds[0] instanceof Class) {
                return (Class<?>) upperBounds[0];
            }
        }

        throw new JsonException("Cannot determine generic type for field: " + field.getName());
    }

    private static Field  findField(Class<?> clazz, String name) throws FieldNotFoundException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new FieldNotFoundException("Field '" + name + "' not found in class " + clazz.getName());
    }


    private static <T> T createInstance(Class<T> clazz) throws ReflectiveOperationException {
        // Сначала попробуем найти конструктор по умолчанию
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            // Если нет конструктора по умолчанию, ищем любой конструктор
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            if (constructors.length == 0) {
                throw new ReflectiveOperationException("No constructors found for " + clazz.getName());
            }

            // Берем первый конструктор (можно добавить логику выбора лучшего конструктора)
            Constructor<?> ctor = constructors[0];
            ctor.setAccessible(true);

            // Создаем массив параметров с null/значениями по умолчанию
            Object[] params = new Object[ctor.getParameterCount()];
            Class<?>[] paramTypes = ctor.getParameterTypes();

            // Заполняем параметры значениями по умолчанию
            for (int i = 0; i < params.length; i++) {
                params[i] = getDefaultValue(paramTypes[i]);
            }

            @SuppressWarnings("unchecked")
            T instance = (T) ctor.newInstance(params);
            return instance;
        }
    }

    // Метод для получения значений по умолчанию для разных типов
    private static Object getDefaultValue(Class<?> type) throws ReflectiveOperationException {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte)0;
        if (type == short.class) return (short)0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\0';
        // Коллекции
        if (List.class.isAssignableFrom(type)) return Collections.emptyList();
        if (Map.class.isAssignableFrom(type)) return Collections.emptyMap();

        // Массивы
        if (type.isArray()) return Array.newInstance(type.getComponentType(), 0);

        // Enum
        if (type.isEnum()) return type.getEnumConstants()[0];
        // Пользовательские классы - пытаемся создать через рефлексию
        try {
            return createInstance(type);
        } catch (Exception e) {
            return null; // Фолбэк
        }
    }
    private static Object convertToArray(Object value, Class<?> arrayType) throws JsonException, ClassNotFoundException, NoSuchFieldException {
        Class<?> componentType = arrayType.getComponentType();

        if (value instanceof Object[]) {
            Object[] source = (Object[]) value;
            Object result = Array.newInstance(componentType, source.length);
            for (int i = 0; i < source.length; i++) {
                Array.set(result, i, convertValue(source[i], componentType));
            }
            return result;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            Object result = Array.newInstance(componentType, length);
            for (int i = 0; i < length; i++) {
                Array.set(result, i, Array.get(value, i));
            }
            return result;
        }

        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            Object result = Array.newInstance(componentType, collection.size());
            int i = 0;
            for (Object element : collection) {
                Array.set(result, i++, convertValue(element, componentType));
            }
            return result;
        }

        throw new JsonException("Cannot convert to array: " + value.getClass());
    }

    private static Object convertValue(Object value, Class<?> targetType) throws JsonException, ClassNotFoundException, NoSuchFieldException {
        if (value == null) return null;

        // Обработка массивов
        if (targetType.isArray() && value instanceof Collection) {
            return convertToArray(value, targetType);
        }

        // Обработка Map
        if (value instanceof Map map) {
            String type = (String) map.remove("@type");
            Class<?> actualType = type != null ? Class.forName(type) : targetType;

            // Создаем копию map, чтобы не изменять оригинал
            Map<String, Object> fieldMap = new HashMap<>(map);
            var classList = findSubclasses(actualType, getRawType(actualType).getPackageName());
            var valuesFieldSet = new HashSet<>(fieldMap.keySet());

            for (var subclass : classList) {
                // Собираем все поля класса, включая родительские
                var subclassFieldSet = new HashSet<>();
                Class<?> currentClass = subclass;
                while (currentClass != null && currentClass != Object.class) {
                    Arrays.stream(currentClass.getDeclaredFields())
                            .map(Field::getName)
                            .forEach(subclassFieldSet::add);
                    currentClass = currentClass.getSuperclass();
                }

                if (valuesFieldSet.equals(subclassFieldSet)) {
                    return setFieldsInClass(fieldMap, subclass);
                }
            }

            // Если не нашли подходящий подкласс, пробуем создать экземпляр targetType
            try {
                return setFieldsInClass(fieldMap, targetType);
            } catch (Exception e) {
                throw new NoSuchFieldException("No matching subclass found and cannot create instance of " + targetType.getName());
            }
        }

        if (value instanceof Collection && !Collection.class.isAssignableFrom(targetType)) {
            try {


                List<Object> result = new ArrayList<>();
                for (Object item : (Collection<?>) value) {
                   // System.out.println(item);
                    result.add(convertValue(item, targetType));
                }
                return result;
            } catch (Exception e) {
                throw new JsonException("Failed to parse collection: " + e.getMessage());
            }
        }



        // Обработка Iterable (кроме массивов)
        if (value instanceof Iterable && !(value instanceof Collection)) {
            value = new ArrayList<>((Collection) value);
        }

//        // Обработка коллекций
//        if (value instanceof Collection) {
//            return convertToCollection(value, targetType);
//        }

        // Обработка примитивов и строк
        if (targetType.isPrimitive() || targetType == String.class) {
            return convertPrimitive(value, targetType);
        }

        // Обработка boolean
        if (targetType == Boolean.class) {
            return convertPrimitive(value, targetType);
        }

        // Обработка enum
        if (targetType.isEnum()) {
            try {
                return Enum.valueOf((Class<Enum>) targetType, value.toString());
            } catch (IllegalArgumentException e) {
                throw new JsonException("Invalid enum value");
            }
        }

        // Проверка типа
        if (!targetType.isInstance(value)) {
            throw new TypeMismatchException(
                    "Type mismatch for " + targetType.getName() +
                            ". Expected: " + targetType + ", actual: " + value.getClass());
        }

        return value;
    }

    // Преобразование примитивных типов
    private static Object convertPrimitive(Object value, Class<?> targetClass) throws JsonException {
        String strValue = value.toString();
        if (targetClass.equals(Integer.class) || targetClass.equals(int.class)) {
            return Integer.parseInt(value.toString());
        }
        if (targetClass.equals(Double.class) || targetClass.equals(double.class)) {
            return Double.parseDouble(value.toString());
        }
        if (targetClass.equals(Boolean.class) || targetClass.equals(boolean.class)) {
            if ("true".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue)) {
                return Boolean.parseBoolean(strValue);
            }
            throw new JsonException("Invalid boolean value: " + strValue);
        }

        if (targetClass.equals(String.class)) return value.toString();
        throw new JsonException("Unsupported primitive type: " + targetClass);
    }

    private static Iterable<?> convertToCollection(Object value, Class<?> collectionType)
            throws JsonException {
        try {
            if (collectionType == null) {
                throw new NullPointerException("collectionType cannot be null");
            }

            // Проверяем, что value является Iterable (например, List)
            if (!(value instanceof Iterable<?>)) {
                throw new JsonException("Value must be an Iterable");
            }
            Class<?> elementType = collectionType.getComponentType();// getGenericType(field); // Нужно определить тип элементов
            List<Object> result = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                result.add(convertValue(item, elementType));
            }
            return result;

//            // Если collectionType — интерфейс (например, List), используем ArrayList
//            Class<?> concreteType = collectionType.isInterface()
//                    ? ArrayList.class
//                    : collectionType;
//
//            // Создаём экземпляр коллекции
//            Collection<Object> result;
//            try {
//                result = (Collection<Object>) concreteType.getDeclaredConstructor().newInstance();
//            } catch (NoSuchMethodException e) {
//                throw new JsonException("Collection type must have a no-args constructor: " + concreteType.getName());
//            }
//
//            // Копируем элементы
//            for (Object item : (Iterable<?>) value) {
//                result.add(item);
//            }

            //return result;
        } catch (Exception e) {
            throw new JsonException("Failed to create collection of type " + collectionType.getName() + ": " + e.getMessage());
        }
    }


    public static Map<String, Object> parseStringToMap(String input) throws JsonException, NullPointerException {
        if (input == null) throw new NullPointerException();
        if(!input.startsWith("{") || !input.endsWith("}"))
            throw new InvalidJsonStringException("JSON object start syntax violation");


        HashMap<String, Object> resultMap = new HashMap<>();
        input = input.substring(1, input.length() - 1).strip();
        if (input.isEmpty()) { // если только скобочки.
            return resultMap;
        }

        //основной парсинг
        String[] listOfPairs = splitJsonPairs(input);

        for (String pairString : listOfPairs) {
            String[] keyValuePair = splitKeyValue(pairString);
            if (!keyValuePair[0].startsWith("\"")) throw new InvalidJsonStringException("Key provided without quotes");
            String key = keyValuePair[0].replaceAll("^\"|\"$", "").strip();
            String valueStr = keyValuePair[1].strip();

            Object value = parseValue(valueStr);
            resultMap.put(key, value);
        }
        return resultMap;
    }

    public static List<Object> parseStringToList(String input) throws JsonException
    {
            if (!input.startsWith("[") || !input.endsWith("]")) {
                throw new InvalidJsonStringException("JSON array syntax violation");
            }

            String content = input.substring(1, input.length() - 1).strip();
            if (content.isEmpty()) {
                return new ArrayList<Object>();
            }

            List<Object> result = new ArrayList<>();
            String[] elements = splitJsonPairs(content);

            for (String element : elements) {
                result.add(parseValue(element.strip()));
            }

            return result;
    }

    // Разбивает содержимое JSON объекта на пары ключ-значение
    private static String[] splitJsonPairs(String content) {
        int level = 0, splitIndex = 0;
        List<String> pairs = new ArrayList<>();

        for (int i = 0; i < content.length(); i++) {
            switch(content.charAt(i))
            {
                case ('{'),('[') -> level++;
                case ('}'),(']') -> level--;
                case (',') -> {
                    if(level == 0)
                    {
                        pairs.add(content.substring(splitIndex, i).strip());
                        splitIndex = i + 1;
                    }
                }
            }
        }
        pairs.add(content.substring(splitIndex).strip());
        return pairs.toArray(new String[0]);
    }
    // Разбивает пару на ключ и значение
    private static String[] splitKeyValue(String pair) throws InvalidPairParsingException, InvalidJsonStringException {
        int colonIndex = -1, level = 0, levelOfQuotes=0;
        for (int i = 0; i < pair.length(); i++) {
            switch (pair.charAt(i)) {
                case ('{'),('[') -> level++;
                case ('}'),(']') -> level--;
                case ('"') -> levelOfQuotes ++;
                case (':') -> {
                    if(level == 0) {
                        colonIndex = i;
                    }
                }
            }
        }
        if (levelOfQuotes %2 != 0) throw new InvalidJsonStringException("Key provided without quotes");
        if (colonIndex == -1) throw new InvalidPairParsingException("No colon found in pair: " + pair);
        return new String[]{
                pair.substring(0, colonIndex).strip(),
                pair.substring(colonIndex + 1).strip()
        };
    }

    static Object parseValue(String valueStr) throws JsonException {
        if (valueStr == null) return null;
        valueStr = valueStr.trim();

        // Обработка null
        if (valueStr.equalsIgnoreCase("null")) {
            return null;
        }

        // Обработка вложенных структур
        if (valueStr.startsWith("{")) {
            return parseStringToMap(valueStr);
        }
        if (valueStr.startsWith("[")) {
            return parseStringToList(valueStr);
        }

        // Обработка boolean
        if (isBoolean(valueStr)) {
            return Boolean.parseBoolean(valueStr.toLowerCase());
        }

        // Обработка строк (только в кавычках)
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }

        // Обработка чисел
        return parseNumber(valueStr);
    }

    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static Number parseNumber(String valueStr) throws JsonException {
        try {
            if (INT_PATTERN.matcher(valueStr).matches()) {
                return Integer.parseInt(valueStr);
            } else if (DOUBLE_PATTERN.matcher(valueStr).matches()) {
                return Double.parseDouble(valueStr);
            }
            throw new JsonException("Invalid number format: " + valueStr);
        } catch (NumberFormatException e) {
            throw new JsonException("Number parsing error");
        }
    }

}


