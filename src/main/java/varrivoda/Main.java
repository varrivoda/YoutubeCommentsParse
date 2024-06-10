package varrivoda;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws UnirestException, ClassCastException {
        String URL = "https://youtube.com/watch?v=e_M1cWqjhXQ";
        //py: 'ytcfg\.set\s*\(\s*({.+?})\s*\)\s*;'
        String RE_YTCFG ="(?:ytcfg\\.set\\s*\\(\\s*()\\{.+?\\})\\s*\\)\\s*;";
        //py: '(?:window\s*\[\s*["\']ytInitialData["\']\s*\]|ytInitialData)\s*=\s*({.+?})\s*;\s*(?:var\s+meta|</script|\n)'
        //экранировать {} не надо!
        //java: String RE_DATA = "(?:window\\s*\\[\\s*[\"']ytInitialData[\"']\\s*\\]|ytInitialData)\\s*=\\s*({.+?})\\s*;\\s*(?:var\\s+meta|</script|\\n)";
        //упростим
        String RE_DATA = "(?:ytInitialData)\s*=\s*(.+?)\s*;\s*(?:</script|\n)";

        GetRequest getRequest = Unirest.get(URL);
        String html = getRequest.asString().getBody();

        //CONFIG
        String ytConfigString = regexSearch(html, RE_YTCFG);
        //TODO переделать в регулярку
        ytConfigString = ytConfigString.substring(10,ytConfigString.length()-2);
        JsonReader reader = new JsonReader(new StringReader(ytConfigString));
        reader.setLenient(true);
        JsonElement ytConfigJsonElement = JsonParser.parseReader(reader);

        //INITIAL DATA
        String ytInitialData = regexSearch(html, RE_DATA);
        //TODO переделать регулярку
        ytInitialData = ytInitialData.substring(16,ytInitialData.length()-9);
        reader = new JsonReader(new StringReader(ytInitialData));
        JsonElement ytInitialDataJsonElement = JsonParser.parseReader(reader);
//        System.out.println(new GsonBuilder().setPrettyPrinting().setLenient().create().toJson(ytInitialData));

        //конвертируем в Map., необязательно
        //parseJsonElementAndPutToMap(jsonElement, new HashMap<String, Object>());
        //parseJsonElementAndPutToMap(ytInitialDataJsonElement, new HashMap<String,Object>());

        // меняяем язык (необязательно)
        //python: if language:
        //            ytcfg['INNERTUBE_CONTEXT']['client']['hl'] = language
        //mergeJsonElements(jsonElement, JsonParser.parseString("{\"INNERTUBE_CONTEXT\":{\"client\":{\"hl\":\"RU\"}}}"));
        // Теперь разбираем DATA
        List<JsonElement> itemSections = searchDict(
                ytInitialDataJsonElement, new ArrayList<>(),"itemSectionRenderer");
        itemSections.forEach((json)-> System.out.println("\n result "+ json));
    }

    private static List<JsonElement> searchDict(JsonElement json, List<JsonElement> results, String key) {
        if(json.isJsonObject()){
            JsonObject jsonObject = json.getAsJsonObject();
            for (String k : jsonObject.keySet()) {
                JsonElement value = jsonObject.get(k);
                if(k.equals(key)) {
                    results.add(value);
                    System.out.println(key+" has found");
                }
                searchDict(value, results, key);
            }
        } else if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                searchDict(element, results, key);
            }
        }else if (json.isJsonPrimitive()) {
            //nothing to do
        }
        return results;
    }

    //JSON to Map
    private static Map<String, Map> parseJsonElementAndPutToMap(JsonElement json, Map map) {
        if(json.isJsonObject()){
            JsonObject jsonObject = json.getAsJsonObject();
            for (String key : jsonObject.keySet()) {
                JsonElement value = jsonObject.get(key);
                //System.out.print("Key: " + key + ", Value: ");
                map.put(key, value);
                parseJsonElementAndPutToMap(value, map);
            }
        } else if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                parseJsonElementAndPutToMap(element, map);
            }
        }else if (json.isJsonPrimitive()){
            //System.out.println("Element " + jsonElenemt.getAsString());
            map.put(json.getAsString(),null);
        }
        return map;
    }

    private static String regexSearch(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        while (m.find())
            return m.group();
        return null;
    }


}