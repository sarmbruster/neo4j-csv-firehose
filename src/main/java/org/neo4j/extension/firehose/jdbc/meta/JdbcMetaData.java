package org.neo4j.extension.firehose.jdbc.meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class JdbcMetaData {
    public List<Map> nodes = new ArrayList<>();
    public List<Map> relationships = new ArrayList<>();

    public Map<String, Object> toMap() {
        return map("nodes", nodes, "relationships", relationships);
    }

    public enum Types {
        integer {
            @Override
            public String toString() {
                return "int";
            }
        },
        numeric {
            @Override
            public String toString() {
                return "float";
            }
        }, string, bool {
            @Override
            public String toString() {
                return "boolean";
            }
        };

        public static Types from(String value) {
            if (value == null) return string;
            for (Types types : values()) {
                if (types.toString().equalsIgnoreCase(value)) return types;
            }
            return string;
        }
    }

    public Map<String, Object> add(TableInfo table, Rules rules) {
        if (rules.isNode(table)) {
            addNode(table, rules);
        } else {
            addRelationship(table, rules);
        }
        return map("nodes", nodes, "relationships", relationships);
    }

    private void addRelationship(TableInfo table, Rules rules) {
        if (table.fks.isEmpty()) return;
        String tableName = table.table;
        List<Map.Entry<List<String>, String>> fks = new ArrayList<>(table.fks.entrySet());
        // todo multi-key
        Map.Entry<List<String>, String> from = fks.get(0);
        Map.Entry<List<String>, String> to = fks.get(1);

        Map rel = map(
                "filename", tableName,
                "name", rules.relTypeFor(tableName),
                "from", createRelationshipNode(from.getValue(), from.getKey().get(0), rules),
                "to", createRelationshipNode(to.getValue(), to.getKey().get(0), rules));
        relationships.add(rel);
    }

    private void addNode(TableInfo table, Rules rules) {
        String tableName = table.table;
        List<Map> properties = new ArrayList<>();
        Map node = map(
                "filename", tableName,
                "labels", asList(rules.labelsFor(table)),
                "properties", properties);
        for (String field : table.pk) {
            properties.add(createJdbcMetaDataProperty(table, rules, field, true));
        }
        for (String field : table.fields) {
            if (!rules.skipPrimaryKey(tableName, field) && table.pk.contains(field)) continue;
            properties.add(createJdbcMetaDataProperty(table, rules, field, false));
        }

        for (Map.Entry<List<String>, String> entry : table.fks.entrySet()) {
            List<String> fields = entry.getKey();
            String target = entry.getValue();
            // todo do we really have to add them as properties in neo?
            for (String field : fields) {
                properties.add(createJdbcMetaDataProperty(table, rules, field, false));

                addRelationship(tableName, target, field, rules);
            }
        }
        nodes.add(node);
    }

    private void addRelationship(String tableName, String target, String field, Rules rules) {
        Map rel = map(
                "filename", tableName,
                "name", rules.relTypeFor(field),
                "from", createRelationshipNode(tableName, field, rules),
                "to", createRelationshipNode(target, field, rules));
        relationships.add(rel);
    }

    private Map createRelationshipNode(String tableName, String field, Rules rules) {
        return map(
                "fileKey", field,
                "neoKey", field,
                "filename", tableName,
                "label", rules.labelFor(tableName));
    }

    private Map createJdbcMetaDataProperty(TableInfo table, Rules rules, String field, boolean pk) {
        String[] parts = field.split(":");
        return map(
                "headerKey", parts[0],
                "dataType", (parts.length == 1 ? Types.string : Types.from(parts[1])).toString(),
                "neoKey", rules.propertyNameFor(table, field),
                "primaryKey", pk,
                "index", pk);
    }

    public static Map<String, Object> map(Object... objects) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < objects.length; i += 2) {
            map.put((String) objects[i], objects[i + 1]);
        }
        return map;
    }
}

/*
{
    "nodes": [
        {
            "filename": "legislators.csv",
            "labels": ["Legislator"],
            "properties": [
                {
                    "headerKey": "thomasID",
                    "neoKey": "thomasID",
                    "dataType": "int",
                    "index": true,
                    "primaryKey": true,
                    "foreignKey": false,
                    "skip": false
                }
            ]
        }
    ],
    "relationships": [
        {
            "filename": "committee-members.csv",
            "from": {
                "filename": "legislators.csv",
                "neoKey": "thomasID",
                "fileKey": "legislatorID",
                "label": "Legislator"
            },
            "to": {
                "filename": "committees.csv",
                "neoKey": "thomasID",
                "fileKey": "committeeID",
                "label": "Committee"
            },
            "name": "SERVES_ON"
        }
    ]

}
*/
