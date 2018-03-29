package qbt.script;

import com.google.common.collect.ImmutableMap;
import groovy.lang.GroovyShell;
import java.nio.file.Path;
import java.util.Map;
import misc1.commons.ExceptionUtils;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.SimpleMapStructType;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.ds.StructTypeBuilder;

public final class QbtScriptEngine extends Struct<QbtScriptEngine, QbtScriptEngine.Builder> {
    private QbtScriptEngine(ImmutableMap<StructKey<QbtScriptEngine, ?, ?>, Object> map) {
        super(TYPE, map);
    }

    public static class Builder extends StructBuilder<QbtScriptEngine, Builder> {
        public Builder(ImmutableSalvagingMap<StructKey<QbtScriptEngine, ?, ?>, Object> map) {
            super(TYPE, map);
        }

        public Builder addVariable(String name, Object value) {
            return transform(VARIABLES, v -> v.with(name, value));
        }

        public Builder addClosure(String name, Closure value) {
            return transform(CLOSURES, c -> c.with(name, value));
        }
    }

    private GroovyShell shell() {
        GroovyShell shell = new GroovyShell();
        for(Map.Entry<String, Object> e : get(VARIABLES).map.entrySet()) {
            shell.setVariable(e.getKey(), e.getValue());
        }
        for(Map.Entry<String, Closure> e : get(CLOSURES).map.entrySet()) {
            shell.setVariable(e.getKey(), new groovy.lang.Closure<Object>(null) {
                @Override
                public Object call(Object... args) {
                    return e.getValue().call(args);
                }
            });
        }
        return shell;
    }

    public <T> T eval(Path f) {
        try {
            return (T) shell().evaluate(f.toFile());
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public <T> T eval(String s) {
        try {
            return (T) shell().evaluate(s);
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public static final StructKey<QbtScriptEngine, Variables, Variables.Builder> VARIABLES;
    public static final StructKey<QbtScriptEngine, Closures, Closures.Builder> CLOSURES;
    public static final StructType<QbtScriptEngine, Builder> TYPE;
    static {
        StructTypeBuilder<QbtScriptEngine, Builder> b = new StructTypeBuilder<>(QbtScriptEngine::new, Builder::new);

        VARIABLES = b.key("variables", Variables.TYPE).add();
        CLOSURES = b.key("closures", Closures.TYPE).add();

        TYPE = b.build();
    }

    public static final class Variables extends MapStruct<Variables, Variables.Builder, String, Object, Object> {
        public Variables(ImmutableMap<String, Object> map) {
            super(TYPE, map);
        }

        public static final class Builder extends MapStructBuilder<Variables, Builder, String, Object, Object> {
            public Builder(ImmutableSalvagingMap<String, Object> map) {
                super(TYPE, map);
            }
        }

        public static final SimpleMapStructType<Variables, Builder, String, Object> TYPE = new SimpleMapStructType<Variables, Builder, String, Object>() {
            @Override
            protected Variables create(ImmutableMap<String, Object> map) {
                return new Variables(map);
            }

            @Override
            protected Builder createBuilder(ImmutableSalvagingMap<String, Object> map) {
                return new Builder(map);
            }
        };
    }

    public interface Closure {
        Object call(Object[] args);
    }

    public static final class Closures extends MapStruct<Closures, Closures.Builder, String, Closure, Closure> {
        public Closures(ImmutableMap<String, Closure> map) {
            super(TYPE, map);
        }

        public static final class Builder extends MapStructBuilder<Closures, Builder, String, Closure, Closure> {
            public Builder(ImmutableSalvagingMap<String, Closure> map) {
                super(TYPE, map);
            }
        }

        public static final SimpleMapStructType<Closures, Builder, String, Closure> TYPE = new SimpleMapStructType<Closures, Builder, String, Closure>() {
            @Override
            protected Closures create(ImmutableMap<String, Closure> map) {
                return new Closures(map);
            }

            @Override
            protected Builder createBuilder(ImmutableSalvagingMap<String, Closure> map) {
                return new Builder(map);
            }
        };
    }
}
