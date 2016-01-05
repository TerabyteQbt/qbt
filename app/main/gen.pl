#!/usr/bin/perl

$| = 1;

use strict;
use warnings;

use File::Basename ('dirname');

sub map_type {
    return Type::Map->new(@_);
}

sub struct_type {
    return Type::Struct->new(@_);
}

sub external_key {
    return Key::External->new(@_);
}

sub external_value {
    return Value::External->new(@_);
}

sub link_value {
    return Value::Link->new(@_);
}

my @IMPORTS = (
    'com.google.common.collect.ImmutableList',
    'com.google.common.collect.ImmutableMap',
    'com.google.common.collect.ImmutableSet',
    'com.google.gson.JsonElement',
    'com.google.gson.JsonObject',
    'java.util.Map',
    'misc1.commons.Maybe',
    'misc1.commons.ds.ImmutableSalvagingMap',
    'misc1.commons.ds.MapStruct',
    'misc1.commons.ds.MapStructBuilder',
    'misc1.commons.ds.MapStructType',
    'misc1.commons.ds.Struct',
    'misc1.commons.ds.StructBuilder',
    'misc1.commons.ds.StructKey',
    'misc1.commons.ds.StructType',
    'misc1.commons.merge.Merge',
    'misc1.commons.merge.Merges',
    'org.apache.commons.lang3.ObjectUtils',
    'org.apache.commons.lang3.tuple.Pair',
    'qbt.NormalDependencyType',
    'qbt.VcsVersionDigest',
    'qbt.manifest.JsonSerializer',
    'qbt.manifest.JsonSerializers',
    'qbt.manifest.PackageBuildType',
    'qbt.manifest.QbtManifestUtils',
    'qbt.manifest.StringSerializer',
    'qbt.tip.PackageTip',
    'qbt.tip.RepoTip',
);
my @DATA = (
    {
        'prefix' => 'qbt.manifest.v0.',
        'types' => {
            'QbtManifest' => map_type(
                'key' => external_key('RepoTip', 'RepoTip.TYPE.STRING_SERIALIZER'),
                'value' => link_value('RepoManifest'),
            ),
            'RepoManifest' => struct_type(
                'fields' => {
                    'version' => external_value('VcsVersionDigest', 'JsonSerializers.VCS_VERSION_DIGEST'),
                    'packages' => link_value('RepoManifestPackages'),
                },
            ),
            'RepoManifestPackages' => map_type(
                'key' => external_key('String', 'StringSerializer.STRING'),
                'value' => link_value('PackageManifest'),
            ),
            'PackageManifest' => struct_type(
                'fields' => {
                    'metadata' => link_value('PackageMetadata'),
                    'normalDeps' => link_value('PackageNormalDeps'),
                    'replaceDeps' => link_value('PackageReplaceDeps'),
                    'verifyDeps' => link_value('PackageVerifyDeps'),
                },
            ),
            'PackageMetadata' => struct_type(
                'fields' => {
                    'prefix' => external_value('Maybe<String>', 'JsonSerializers.forStringSerializer(StringSerializer.V0_PREFIX)', 'Maybe.of("")'),
                    'archIndependent' => external_value('Boolean', 'JsonSerializers.forStringSerializer(StringSerializer.BOOLEAN)', 'false'),
                    'qbtEnv' => external_value('ImmutableSet<String>', 'JsonSerializers.forStringSerializer(StringSerializer.V0_QBT_ENV)', 'ImmutableSet.<String>of()'),
                    'buildType' => external_value('PackageBuildType', 'JsonSerializers.forEnum(PackageBuildType.class)', 'PackageBuildType.NORMAL'),
                },
            ),
            'PackageNormalDeps' => map_type(
                'key' => external_key('String', 'StringSerializer.STRING'),
                'value' => external_value('Pair<NormalDependencyType, String>', 'JsonSerializers.NORMAL_DEP_VALUE'),
            ),
            'PackageReplaceDeps' => map_type(
                'key' => external_key('PackageTip', 'PackageTip.TYPE.STRING_SERIALIZER'),
                'value' => external_value('String', 'JsonSerializers.STRING'),
            ),
            'PackageVerifyDeps' => map_type(
                'key' => external_key('Pair<PackageTip, String>', 'StringSerializer.VERIFY_DEP_KEY'),
                'value' => external_value('ObjectUtils.Null', 'JsonSerializers.OU_NULL'),
            ),
        },
        'upgrades' => ['QbtManifest'],
    },
    {
        'prefix' => 'qbt.manifest.current.',
        'types' => {
            'QbtManifest' => map_type(
                'key' => external_key('RepoTip', 'RepoTip.TYPE.STRING_SERIALIZER'),
                'value' => link_value('RepoManifest'),
                'extraFields' => [
                    ['repos', 'ImmutableMap<RepoTip, RepoManifest>', 'map'],
                    ['packageToRepo', 'ImmutableMap<PackageTip, RepoTip>', 'QbtManifestUtils.invertReposMap(repos)'],
                ],
            ),
            'RepoManifest' => struct_type(
                'fields' => {
                    'version' => external_value('VcsVersionDigest', 'JsonSerializers.VCS_VERSION_DIGEST'),
                    'packages' => link_value('RepoManifestPackages'),
                },
                'extraFields' => [
                    ['version', 'VcsVersionDigest', 'get(VERSION)'],
                    ['packages', 'ImmutableMap<String, PackageManifest>', 'get(PACKAGES).map'],
                ],
            ),
            'RepoManifestPackages' => map_type(
                'key' => external_key('String', 'StringSerializer.STRING'),
                'value' => link_value('PackageManifest'),
            ),
            'PackageManifest' => struct_type(
                'fields' => {
                    'metadata' => link_value('PackageMetadata'),
                    'normalDeps' => link_value('PackageNormalDeps'),
                    'replaceDeps' => link_value('PackageReplaceDeps'),
                    'verifyDeps' => link_value('PackageVerifyDeps'),
                },
                'extraFields' => [
                    ['metadata', 'PackageMetadata', 'get(METADATA)'],
                    ['normalDeps', 'ImmutableMap<String, Pair<NormalDependencyType, String>>', 'get(NORMAL_DEPS).map'],
                    ['replaceDeps', 'ImmutableMap<PackageTip, String>', 'get(REPLACE_DEPS).map'],
                    ['verifyDeps', 'ImmutableSet<Pair<PackageTip, String>>', 'get(VERIFY_DEPS).map.keySet()'],
                ],
            ),
            'PackageMetadata' => struct_type(
                'fields' => {
                    'prefix' => external_value('Maybe<String>', 'JsonSerializers.forStringSerializer(StringSerializer.V0_PREFIX)', 'Maybe.of("")'),
                    'archIndependent' => external_value('Boolean', 'JsonSerializers.forStringSerializer(StringSerializer.BOOLEAN)', 'false'),
                    'qbtEnv' => external_value('ImmutableSet<String>', 'JsonSerializers.forStringSerializer(StringSerializer.V0_QBT_ENV)', 'ImmutableSet.<String>of()'),
                    'buildType' => external_value('PackageBuildType', 'JsonSerializers.forEnum(PackageBuildType.class)', 'PackageBuildType.NORMAL'),
                },
            ),
            'PackageNormalDeps' => map_type(
                'key' => external_key('String', 'StringSerializer.STRING'),
                'value' => external_value('Pair<NormalDependencyType, String>', 'JsonSerializers.NORMAL_DEP_VALUE'),
            ),
            'PackageReplaceDeps' => map_type(
                'key' => external_key('PackageTip', 'PackageTip.TYPE.STRING_SERIALIZER'),
                'value' => external_value('String', 'JsonSerializers.STRING'),
            ),
            'PackageVerifyDeps' => map_type(
                'key' => external_key('Pair<PackageTip, String>', 'StringSerializer.VERIFY_DEP_KEY'),
                'value' => external_value('ObjectUtils.Null', 'JsonSerializers.OU_NULL'),
            ),
        },
    },
);

for(my $i = 0; $i < @DATA; ++$i) {
    my $v1 = $DATA[$i];
    my $v2 = ($i + 1 < @DATA ? $DATA[$i + 1] : undef);

    for my $name (sort(keys(%{$v1->{'types'}}))) {
        my $s1 = $v1->{'types'}->{$name};
        my @args = ();
        $s1->gen($v1->{'prefix'}, $name);
    }

    if($v1->{'upgrades'} && defined($v2))
    {
        gen_file($v1->{'prefix'} . "Upgrades", sub {
            my $fh = shift;

            print $fh "public class Upgrades {\n";
            my @q = @{$v1->{'upgrades'}};
            my %done;
            while(@q)
            {
                my $name = shift @q;
                next if($done{$name});
                $done{$name} = 1;

                my $from_type = $v1->{'types'}->{$name};
                die unless($from_type);
                my $to_type = $v2->{'types'}->{$name};
                die unless($to_type);

                # for now only allow identical types, eventually we'll allow
                # custom to supercede this
                die unless(ref($from_type) eq ref($to_type));

                $from_type->gen_upgrade(\@q, $name, $fh, $v2->{'prefix'}, $to_type);
            }
            print $fh "}\n";
        });
    }
}

sub upper {
    my $s = shift;
    $s =~ s/[A-Z]/_$&/g;
    return uc($s);
}

sub gen_file {
    my $type = shift;
    my $cb = shift;

    my $type_path = $type;
    $type_path =~ s/\./\//g;
    my $fn = ".src.gen/$type_path.java";
    my $java_pkg = $type;
    $java_pkg =~ s/\.[^.]*$//;

    system('mkdir', '-p', dirname($fn));
    open(my $fh, '>', $fn) || die "Could not open $fn: $!";
    print $fh "package $java_pkg;\n";
    print $fh "\n";
    for my $import (@IMPORTS) {
        print $fh "import $import;\n";
    }
    print $fh "\n";
    $cb->($fh);
    close($fh) || die "Could not close $fn: $!";
}

package Type::Base;

sub new {
    my $class = shift;

    my $this = { @_ };

    bless $this, $class;

    return $this;
}

sub gen {
    my $this = shift;
    my $prefix = shift;
    my $name = shift;

    my $type = "$prefix$name";

    main::gen_file($type, sub {
        my $fh = shift;
        $this->gen2($name, $fh);
    });
}

sub gen_extraFields_fields {
    my $this = shift;
    my $fh = shift;

    if($this->{'extraFields'}) {
        for my $tuple (@{$this->{'extraFields'}}) {
            my ($name, $type, $init) = @$tuple;
            print $fh "    public final $type $name;\n";
        }
        print $fh "\n";
    }
}

sub gen_extraFields_ctor {
    my $this = shift;
    my $fh = shift;

    if($this->{'extraFields'}) {
        print $fh "\n";
        for my $tuple (@{$this->{'extraFields'}}) {
            my ($name, $type, $init) = @$tuple;
            print $fh "        this.$name = $init;\n";
        }
    }
}

package Type::Map;

use base ('Type::Base');

sub gen2 {
    my $this = shift;
    my $name = shift;
    my $fh = shift;

    my $key = $this->{'key'};
    my $k = $key->k();
    my $value = $this->{'value'};
    my $vs = $value->vs();
    my $vb = $value->vb();

    print $fh "public final class $name extends MapStruct<$name, $name.Builder, $k, $vs, $vb> {\n";
    $this->gen_extraFields_fields($fh);
    print $fh "    private $name(ImmutableMap<$k, $vs> map) {\n";
    print $fh "        super(TYPE, map);\n";
    $this->gen_extraFields_ctor($fh);
    print $fh "    }\n";
    print $fh "\n";
    print $fh "    public static class Builder extends MapStructBuilder<$name, Builder, $k, $vs, $vb> {\n";
    print $fh "        public Builder(ImmutableSalvagingMap<$k, $vb> map) {\n";
    print $fh "            super(TYPE, map);\n";
    print $fh "        }\n";
    print $fh "    }\n";
    print $fh "\n";
    print $fh "    public static final MapStructType<$name, Builder, $k, $vs, $vb> TYPE = new MapStructType<$name, Builder, $k, $vs, $vb>() {\n";
    print $fh "        \@Override\n";
    print $fh "        protected $name create(ImmutableMap<$k, $vs> map) {\n";
    print $fh "            return new $name(map);\n";
    print $fh "        }\n";
    print $fh "\n";
    print $fh "        \@Override\n";
    print $fh "        protected Builder createBuilder(ImmutableSalvagingMap<$k, $vb> map) {\n";
    print $fh "            return new Builder(map);\n";
    print $fh "        }\n";
    print $fh "\n";
    print $fh "        \@Override\n";
    print $fh "        protected $vs toStruct($vb vb) {\n";
    print $fh "            return " . $value->toStruct('vb') . ";\n";
    print $fh "        }\n";
    print $fh "\n";
    print $fh "        \@Override\n";
    print $fh "        protected $vb toBuilder($vs vs) {\n";
    print $fh "            return " . $value->toBuilder('vs') . ";\n";
    print $fh "        }\n";
    print $fh "\n";
    print $fh "        \@Override\n";
    print $fh "        protected Merge<$vs> mergeValue() {\n";
    print $fh "            return " . $value->merge() . ";\n";
    print $fh "        }\n";
    print $fh "    };\n";
    print $fh "\n";
    print $fh "    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {\n";
    print $fh "        \@Override\n";
    print $fh "        public JsonElement toJson(Builder b) {\n";
    print $fh "            JsonObject r = new JsonObject();\n";
    print $fh "            for(Map.Entry<$k, $vb> e : b.map.entries()) {\n";
    print $fh "                r.add(" . $key->serializer() . ".toString(e.getKey()), " . $value->serializer() . ".toJson(e.getValue()));\n";
    print $fh "            }\n";
    print $fh "            return r;\n";
    print $fh "        }\n";
    print $fh "\n";
    print $fh "        \@Override\n";
    print $fh "        public Builder fromJson(JsonElement e) {\n";
    print $fh "            Builder b = TYPE.builder();\n";
    print $fh "            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {\n";
    print $fh "                b = b.with(" . $key->serializer() . ".fromString(e2.getKey()), " . $value->serializer() . ".fromJson(e2.getValue()));\n";
    print $fh "            }\n";
    print $fh "            return b;\n";
    print $fh "        }\n";
    print $fh "    };\n";
    print $fh "}\n";
}

sub gen_upgrade {
    my $this = shift;
    my $qr = shift;
    my $name = shift;
    my $fh = shift;
    my $to_prefix = shift;
    my $to = shift;

    my $k = $this->{'key'}->k();
    my $vs = $this->{'value'}->vs();

    print $fh "    public static $to_prefix$name.Builder upgrade_$name($name old) {\n";
    print $fh "        $to_prefix$name.Builder b = $to_prefix$name.TYPE.builder();\n";
    print $fh "        for(Map.Entry<$k, $vs> e : old.map.entrySet()) {\n";
    print $fh "            b = b.with(e.getKey(), " . $this->{'value'}->gen_upgrade($qr, 'e.getValue()') . ");\n";
    print $fh "        }\n";
    print $fh "        return b;\n";
    print $fh "    }\n";
}

package Type::Struct;

use base ('Type::Base');

sub gen2 {
    my $this = shift;
    my $name = shift;
    my $fh = shift;

    print $fh "public final class $name extends Struct<$name, $name.Builder> {\n";
    $this->gen_extraFields_fields($fh);
    print $fh "    private $name(ImmutableMap<StructKey<$name, ?, ?>, Object> map) {\n";
    print $fh "        super(TYPE, map);\n";
    $this->gen_extraFields_ctor($fh);
    print $fh "    }\n";
    print $fh "\n";
    print $fh "    public static class Builder extends StructBuilder<$name, Builder> {\n";
    print $fh "        public Builder(ImmutableSalvagingMap<StructKey<$name, ?, ?>, Object> map) {\n";
    print $fh "            super(TYPE, map);\n";
    print $fh "        }\n";
    print $fh "    }\n";
    print $fh "\n";
    for my $key (sort(keys(%{$this->{'fields'}}))) {
        my $value = $this->{'fields'}->{$key};
        my $KEY = main::upper($key);
        my $vs = $value->vs();
        my $vb = $value->vb();
        print $fh "    public static final StructKey<$name, $vs, $vb> $KEY;\n";
    }
    print $fh "    public static final StructType<$name, Builder> TYPE;\n";
    print $fh "    static {\n";
    print $fh "        ImmutableList.Builder<StructKey<$name, ?, ?>> b = ImmutableList.builder();\n";
    print $fh "\n";
    for my $key (sort(keys(%{$this->{'fields'}}))) {
        my $value = $this->{'fields'}->{$key};
        my $KEY = main::upper($key);
        my $vs = $value->vs();
        my $vb = $value->vb();
        my $def = $value->default();
        print $fh "        b.add($KEY = new StructKey<$name, $vs, $vb>(\"$key\"" . (defined($def) ? ", $def" : "") . ") {\n";
        print $fh "            \@Override\n";
        print $fh "            public $vs toStruct($vb vb) {\n";
        print $fh "                return " . $value->toStruct('vb') . ";\n";
        print $fh "            }\n";
        print $fh "\n";
        print $fh "            \@Override\n";
        print $fh "            public $vb toBuilder($vs vs) {\n";
        print $fh "                return " . $value->toBuilder('vs') . ";\n";
        print $fh "            }\n";
        print $fh "\n";
        print $fh "            \@Override\n";
        print $fh "            public Merge<$vs> merge() {\n";
        print $fh "                return " . $value->merge() . ";\n";
        print $fh "            }\n";
        print $fh "        });\n";
    }
    print $fh "\n";
    print $fh "        TYPE = new StructType<$name, Builder>(b.build()) {\n";
    print $fh "            \@Override\n";
    print $fh "            protected $name createUnchecked(ImmutableMap<StructKey<$name, ?, ?>, Object> map) {\n";
    print $fh "                return new $name(map);\n";
    print $fh "            }\n";
    print $fh "\n";
    print $fh "            \@Override\n";
    print $fh "            protected Builder createBuilder(ImmutableSalvagingMap<StructKey<$name, ?, ?>, Object> map) {\n";
    print $fh "                return new Builder(map);\n";
    print $fh "            }\n";
    print $fh "        };\n";
    print $fh "    }\n";
    print $fh "\n";
    print $fh "    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {\n";
    print $fh "        \@Override\n";
    print $fh "        public JsonElement toJson(Builder b) {\n";
    print $fh "            JsonObject r = new JsonObject();\n";
    for my $key (sort(keys(%{$this->{'fields'}}))) {
        my $value = $this->{'fields'}->{$key};
        my $KEY = main::upper($key);
        my $vb = $value->vb();
        my $def = $value->default();
        if(defined($def)) {
            print $fh "            $vb $key = b.get($KEY);\n";
            print $fh "            if(!$key.equals($def)) {\n";
            print $fh "                r.add(\"$key\", (" . $value->serializer() . ").toJson($key));\n";
            print $fh "            }\n";
        }
        else {
            print $fh "            r.add(\"$key\", (" . $value->serializer() . ").toJson(b.get($KEY)));\n";
        }
    }
    print $fh "            return r;\n";
    print $fh "        }\n";
    print $fh "\n";
    print $fh "        \@Override\n";
    print $fh "        public Builder fromJson(JsonElement e) {\n";
    print $fh "            Builder b = TYPE.builder();\n";
    print $fh "            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {\n";
    print $fh "                switch(e2.getKey()) {\n";
    for my $key (sort(keys(%{$this->{'fields'}}))) {
        my $value = $this->{'fields'}->{$key};
        my $KEY = main::upper($key);
        print $fh "                    case \"$key\":\n";
        print $fh "                        b = b.set($KEY, (" . $value->serializer() . ").fromJson(e2.getValue()));\n";
        print $fh "                        break;\n";
        print $fh "\n";
    }
    print $fh "                    default:\n";
    print $fh "                        throw new IllegalArgumentException(e2.getKey());\n";
    print $fh "                }\n";
    print $fh "            }\n";
    print $fh "            return b;\n";
    print $fh "        }\n";
    print $fh "    };\n";
    print $fh "}\n";
}

sub gen_upgrade {
    my $this = shift;
    my $qr = shift;
    my $name = shift;
    my $fh = shift;
    my $to_prefix = shift;
    my $to = shift;

    print $fh "    public static $to_prefix$name.Builder upgrade_$name($name old) {\n";
    print $fh "        $to_prefix$name.Builder b = $to_prefix$name.TYPE.builder();\n";
    for my $key (sort(keys(%{$this->{'fields'}}))) {
        my $value = $this->{'fields'}->{$key};
        my $KEY = main::upper($key);
        my $vs = $value->vs();
        print $fh "        b = b.set($to_prefix$name.$KEY, " . $value->gen_upgrade($qr, "old.get($name.$KEY)") . ");\n";
    }
    print $fh "        return b;\n";
    print $fh "    }\n";
}

package Key::External;

sub new {
    my $class = shift;
    my $type = shift;
    my $serializer = shift;

    my $this = {
        'type' => $type,
        'serializer' => $serializer,
    };

    bless $this, $class;

    return $this;
}

sub k {
    my $this = shift;
    return $this->{'type'};
}

sub serializer {
    my $this = shift;
    return $this->{'serializer'};
}

package Value::External;

sub new {
    my $class = shift;
    my $type = shift;
    my $serializer = shift;
    my $default = shift;

    my $this = {
        'type' => $type,
        'serializer' => $serializer,
        'default' => $default,
    };

    bless $this, $class;

    return $this;
}

sub vs {
    my $this = shift;
    return $this->{'type'};
}

sub vb {
    my $this = shift;
    return $this->{'type'};
}

sub toStruct {
    my $this = shift;
    my $e = shift;
    return $e;
}

sub toBuilder {
    my $this = shift;
    my $e = shift;
    return $e;
}

sub merge {
    my $this = shift;
    my $type = $this->{'type'};
    return "Merges.<$type>trivial()";
}

sub serializer {
    my $this = shift;
    return $this->{'serializer'};
}

sub default {
    my $this = shift;
    return $this->{'default'};
}

sub gen_upgrade {
    my $this = shift;
    my $qr = shift;
    my $e = shift;
    return $e;
}

package Value::Link;

sub new {
    my $class = shift;
    my $type = shift;

    my $this = {
        'type' => $type,
    };

    bless $this, $class;

    return $this;
}

sub vs {
    my $this = shift;
    return $this->{'type'};
}

sub vb {
    my $this = shift;
    return $this->{'type'} . '.Builder';
}

sub toStruct {
    my $this = shift;
    my $e = shift;
    return "($e).build()";
}

sub toBuilder {
    my $this = shift;
    my $e = shift;
    return "($e).builder()";
}

sub merge {
    my $this = shift;
    my $type = $this->{'type'};
    return "$type.TYPE.merge()";
}

sub serializer {
    my $this = shift;
    my $type = $this->{'type'};
    return "$type.SERIALIZER";
}

sub default {
    my $this = shift;
    my $type = $this->{'type'};
    return "$type.TYPE.builder()";
}

sub gen_upgrade {
    my $this = shift;
    my $qr = shift;
    my $e = shift;
    my $name = $this->{'type'};
    push @$qr, $name;
    return "upgrade_$name($e)";
}
