package org.neo4j.extension.firehose.helper;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 01.03.15
 */
public class Rules {

    private final Set<String> skipTables = new HashSet<>();

    public Rules(String... skipTables) {
        this.skipTables.addAll((asList(skipTables)));
    }

    String propertyNameFor(TableInfo table, String field) {
        return camelCase(field);
    }

    private String camelCase(String field) {
        if (field.matches("[A-Z_]+")) {
            String[] parts = field.toLowerCase().split("_");
            String result = parts[0];
            for (int i = 1; i < parts.length; i++) {
                result += capitalize(parts[i]);
            }
            return result;
        }
        return field;
    }

    private String capitalize(String str) {
        if (str==null || str.isEmpty()) return str;
        if (str.length()==1) return str.toUpperCase();
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }

    List<String> propertyNamesFor(TableInfo table) {
        List<String> result = new ArrayList<>();
        for (String field : table.fields) {
            result.add(propertyNameFor(table, field));
        }
        return result;
    }

    String[] labelsFor(TableInfo table) {
        return new String[]{labelFor(table.table)};
    }

    public String labelFor(String table) {
        return capitalize(camelCase(unquote(table)));
    }

    private String unquote(String name) {
        boolean quoted = name.charAt(0) == '`';
        return quoted ? name.substring(1, name.length() - 1) : name;
    }

    public String relTypeFor(TableInfo table) {
        return relTypeFor(table.table);
    }

    public String relTypeFor(String target) {
        return unquote(target).replaceAll("([a-z]) ?([A-Z])", "$1_$2").toUpperCase().replace(' ', '_');
    }

    boolean isNode(TableInfo table) {
        return table.hasPk() || table.fks.size() != 2;
    }

    public Object transformPk(Object pk) {
        if (pk == null) return null;
        else return pk.toString();
    }

    public Object convertValue(TableInfo table, String field, Object value) throws SQLException, IOException {
        if (value instanceof Date) {
//            return 0;
            return ((Date) value).getTime();
        }
        if (value instanceof BigDecimal) return ((BigDecimal) value).doubleValue(); // or string??
        if (value instanceof Blob) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            IOUtils.copy(((Blob) value).getBinaryStream(), bo);
            return bo.toByteArray(); // or string??
        }
        // todo importer should ignore null values
        if (value == null) return "";
        return value;
    }


    public boolean skipTable(String tableName) {
        return skipTables.contains(tableName);
    }

    public boolean skipPrimaryKey(String tableName, String columnName) {
        return false;
    }
}
