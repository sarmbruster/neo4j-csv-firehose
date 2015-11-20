package org.neo4j.extension.firehose.jdbc.meta;

import schemacrawler.schema.*;
import schemacrawler.schemacrawler.InclusionRule;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevel;
import schemacrawler.utility.SchemaCrawlerUtility;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * @author mh
 * @since 01.03.15
 */
public class MetaDataReader {
    static TableInfo[] extractTables(Connection conn, final String schemaName, Rules rules) throws SchemaCrawlerException, SQLException {
        ArrayList<TableInfo> tableList = new ArrayList<TableInfo>(100);

        final SchemaCrawlerOptions options = new SchemaCrawlerOptions();
        options.setSchemaInfoLevel(SchemaInfoLevel.standard());
        System.out.println("my catalog =" + conn.getCatalog());

        final Catalog catalog = SchemaCrawlerUtility.getCatalog(conn, options);

        for (Schema schema : catalog.getSchemas()) {
            System.err.println(schema.getName());
        }
        boolean useSchema = schemaName != null && !catalog.getSchemas().isEmpty() && catalog.getSchema(schemaName) != null;
        Collection<Table> tables;
        if (useSchema) {
            options.setSchemaInclusionRule(new InclusionRule() {
                @Override public boolean test(String aSchema) {
                    return schemaName.equals(aSchema);
                }
            });

            final Schema schema = catalog.getSchema(schemaName);
            tables = catalog.getTables(schema);
        }
        else {
            tables = catalog.getTables();
        }

        for (final Table table : tables) {
            String tableName = table.getName();

            System.out.println(table + " pk " + table.getPrimaryKey() + " fks " + table.getForeignKeys() + " type " + table.getTableType());
            if (rules.skipTable(tableName) || table.getTableType().isView()) {
                System.out.println("SKIPPED");
                continue;
            }
            List<Column> columns = table.getColumns();
            List<String> fields = new ArrayList<>(columns.size());
            for (final Column column : columns) {
//                    System.out.println("     o--> " + column + " pk: "+ column.isPartOfPrimaryKey() + " fk: " + column.isPartOfForeignKey());
                String columnName = column.getName();
                if (column.isPartOfPrimaryKey() && rules.skipPrimaryKey(tableName, columnName)) {
                    // skip, todo strategy
                } else if (column.isPartOfForeignKey()) {
                    // skip, todo strategy
                } else {
                    fields.add(columnName);
                }
            }
            Map<List<String>, String> fks = extractForeignKeys(table);
            tableList.add(TableInfo.add(tableName, extractPrimaryKeys(table, fks), fields, fks));
        }

        return tableList.toArray(new TableInfo[tableList.size()]);
    }

    private static Map<List<String>, String> extractForeignKeys(Table table) {
        Collection<ForeignKey> foreignKeys = table.getForeignKeys();
        Map<List<String>, String> fks = fks = new LinkedHashMap<>(10);
        if (foreignKeys != null) {
            for (ForeignKey foreignKey : foreignKeys) {
                // todo handle composite keys
                List<ForeignKeyColumnReference> columnReferences = foreignKey.getColumnReferences();
                if (columnReferences.isEmpty()) continue;
                ForeignKeyColumnReference firstReference = columnReferences.get(0);
                String otherTableName = firstReference.getPrimaryKeyColumn().getParent().getName();
                List<String> keys = new ArrayList<>(3);
                for (ForeignKeyColumnReference reference : columnReferences) {
                    // todo assert that all have the same parent
                    Table otherTable = reference.getPrimaryKeyColumn().getParent();
                    Table thisTable = reference.getForeignKeyColumn().getParent();
                    if (otherTable.equals(table) && !thisTable.equals(table)) continue;
                    if (!otherTable.getName().equals(otherTableName)) {
                        throw new IllegalStateException("Foreign Key to different tables " + reference + " inconsistent with " + firstReference);
                    }
                    keys.add(reference.getForeignKeyColumn().getName());
                }
                if (!keys.isEmpty()) fks.put(keys, otherTableName);
            }
        }
        return fks;
    }

    private static List<String> extractPrimaryKeys(Table table, Map<List<String>, String> fks) {
        // todo handle composite keys
        List<String> pks = new ArrayList<>();
//        String pk = null;
        if (table.getPrimaryKey() != null) {
            List<IndexColumn> pkColumns = table.getPrimaryKey().getColumns();
            for (IndexColumn pkColumn : pkColumns) {
                String pkName = pkColumn.getName();
//TODO check and think
//                if (fks.containsKey(pkName)) continue;
//TODO rule
//                if (isForeignKeyPart(fks, pkName)) continue;
                pks.add(pkName);
            }
//            System.out.println("Real? Primary Keys " + pks);
        }
        return pks;
    }

    private static boolean isForeignKeyPart(Map<List<String>, String> fks, String columnName) {
        for (List<String> keys : fks.keySet()) {
            if (keys.contains(columnName)) return true;
        }
        return false;
    }
}
