# JSON Parser Library

## Описание

Библиотека `JsonParser` предоставляет функциональность для сериализации и десериализации Java-объектов в JSON и обратно. Реализация поддерживает работу с примитивными типами, объектами, коллекциями, массивами и enum'ами.

## Основные возможности

- **Сериализация** Java-объектов в JSON-строку
- **Десериализация** JSON-строк в Java-объекты
- Поддержка **вложенных объектов** и **массивов**
- Обработка **пользовательских классов**
- Поддержка **коллекций** (List, Map)
- Работа с **enum'ами** и **примитивными типами**
- Гибкая система **обработки ошибок**

## Использование

### Зависимости

Просто добавьте файл `JsonParser.java` в ваш проект.

### Примеры использования

#### 1. Сериализация объекта в JSON

```java
public class User {
    private String name;
    private int age;
    private List<String> hobbies;
    
    // конструкторы, геттеры, сеттеры
}

User user = new User("John", 30, Arrays.asList("Reading", "Swimming"));
String json = JsonParser.parseToString(user);
System.out.println(json);
// Вывод: {"name":"John","age":30,"hobbies":["Reading","Swimming"]}
