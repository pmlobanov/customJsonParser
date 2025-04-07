package ru.spbstu.telematics.java;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {


        String str_1 = "{\"user\":\"Alice\",\"age\":30,\"address\":{\"street\":\"Main St\",\"city\":\"NY\",\"coordinates\":{\"lat\":40.7128,\"lon\":-74.0060}}}";
        String str = "[{\"street\":\"Main St\",\"city\":\"NY\"}, {\"lat\":40.7128,\"lon\":-74.0060}]";
        try {
            System.out.println(JsonParser.parseStringToMap(str_1).toString());
        } catch (JsonException e) {
            throw new RuntimeException(e);
        }
    }
}
