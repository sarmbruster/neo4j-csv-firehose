package org.neo4j.extension.firehose.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class JdbcMetaData {
	public List<JdbcMetaDataNode> nodes = new ArrayList<>();
	public List<JdbcMetaDataRelationship> relationships = new ArrayList<>();

	public static class JdbcMetaDataNode {
		public String filename;
        public List<String> labels = new ArrayList<>();
        public List<JdbcMetaDataProperty> properties = new ArrayList<>();
	}
    public static class JdbcMetaDataProperty {
        public String headerKey;
        public String neoKey;
        public Types dataType;
        public boolean index;
        public boolean primaryKey;
        public boolean foreignKey;
        public boolean skip;
    }
    public static class JdbcMetaDataRelationship {
        public String filename;
        public String name;
        public JdbcMetaDataRelationshipNode from;
        public JdbcMetaDataRelationshipNode to;
    }
	public static class JdbcMetaDataRelationshipNode {
        public String filename;
        public String neoKey;
        public String fileKey;
        public String label;
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

    public void add(TableInfo table, Rules rules) {
        if (rules.isNode(table)) {
            addNode(table, rules);
        } else {
            addRelationship(table, rules);
        }
    }

    private void addRelationship(TableInfo table, Rules rules) {
        if (table.fks.isEmpty()) return;
        JdbcMetaDataRelationship rel = new JdbcMetaDataRelationship();
        String tableName = table.table;
        relationships.add(rel);
        rel.filename = tableName;
        rel.name = rules.relTypeFor(tableName);
        List<Map.Entry<List<String>, String>> fks = new ArrayList<>(table.fks.entrySet());
        Map.Entry<List<String>, String> from = fks.get(0);
        // todo multi-key
        rel.from = createRelationshipNode(from.getValue(), from.getKey().get(0), rules);
        Map.Entry<List<String>, String> to = fks.get(1);
        // todo multi-key
        rel.to = createRelationshipNode(to.getValue(), to.getKey().get(0), rules);
        relationships.add(rel);
    }

    private void addNode(TableInfo table, Rules rules) {
        JdbcMetaDataNode node = new JdbcMetaDataNode();
        String tableName = table.table;
        node.filename = tableName;
        node.labels = asList(rules.labelsFor(table));
        for (String field : table.pk) {
            node.properties.add(createJdbcMetaDataProperty(table, rules, field, true));
        }
        for (String field : table.fields) {
            if (!rules.skipPrimaryKey(tableName,field) && table.pk.contains(field)) continue;
            node.properties.add(createJdbcMetaDataProperty(table, rules, field, false));
        }

        for (Map.Entry<List<String>, String> entry : table.fks.entrySet()) {
            List<String> fields = entry.getKey();
            String target = entry.getValue();
            // todo do we really have to add them as properties in neo?
            for (String field : fields) {
                node.properties.add(createJdbcMetaDataProperty(table, rules, field, false));

                addRelationship(tableName, target, field, rules);
            }
        }
        nodes.add(node);
    }

    private void addRelationship(String tableName, String target, String field, Rules rules) {
        JdbcMetaDataRelationship rel = new JdbcMetaDataRelationship();
        rel.filename = tableName;
        rel.name = rules.relTypeFor(field);
        rel.from = createRelationshipNode(tableName, field, rules);
        rel.to = createRelationshipNode(target, field, rules);
        relationships.add(rel);
    }

    private JdbcMetaDataRelationshipNode createRelationshipNode(String tableName, String field, Rules rules) {
        JdbcMetaDataRelationshipNode node = new JdbcMetaDataRelationshipNode();
        node.fileKey = field;
        node.neoKey = field;
        node.filename = tableName;
        node.label = rules.labelFor(tableName);
        return node;
    }

    private JdbcMetaDataProperty createJdbcMetaDataProperty(TableInfo table, Rules rules, String field, boolean pk) {
        JdbcMetaDataProperty prop = new JdbcMetaDataProperty();
        String[] parts = field.split(":");
        prop.headerKey = parts[0];
        prop.dataType = parts.length == 1 ? Types.string : Types.from(parts[1]);
        prop.neoKey = rules.propertyNameFor(table,field);
        prop.primaryKey = pk;
        prop.index = pk;
        return prop;
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
