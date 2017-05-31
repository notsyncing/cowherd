package io.github.notsyncing.cowherd.utils;

import io.vertx.core.json.JsonObject;

public class ConfigUtils {
    // From https://stackoverflow.com/a/15070484/6643564
    public static JsonObject merge(JsonObject source, JsonObject target)
    {
        for (String key : source.fieldNames()) {
            Object value = source.getValue(key);

            if (!target.containsKey(key)) {
                // new value for "key":
                target.put(key, value);
            } else {
                // existing value for "key" - recursively deep merge:
                if (value instanceof JsonObject) {
                    JsonObject valueJson = (JsonObject)value;
                    merge(valueJson, target.getJsonObject(key));
                } else {
                    target.put(key, value);
                }
            }
        }
        return target;
    }
}
