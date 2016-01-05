package qbt.manifest;

import com.google.gson.JsonElement;

public interface JsonSerializer<V> {
    JsonElement toJson(V v);
    V fromJson(JsonElement e);
}
