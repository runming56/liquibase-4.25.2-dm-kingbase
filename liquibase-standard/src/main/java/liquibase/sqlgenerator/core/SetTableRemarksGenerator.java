package liquibase.sqlgenerator.core;

import liquibase.database.Database;
import liquibase.database.core.*;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.SetTableRemarksStatement;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;
import liquibase.util.StringUtil;

public class SetTableRemarksGenerator extends AbstractSqlGenerator<SetTableRemarksStatement> {

	@Override
	public boolean supports(SetTableRemarksStatement statement, Database database) {
		return (database instanceof MySQLDatabase) || (database instanceof OracleDatabase) || (database instanceof
            PostgresDatabase) || (database instanceof AbstractDb2Database) || (database instanceof MSSQLDatabase) ||
            (database instanceof H2Database) || (database instanceof SybaseASADatabase)|| (database instanceof DmDatabase)
			|| (database instanceof KingBase8Database);
	}

	@Override
    public ValidationErrors validate(SetTableRemarksStatement setTableRemarksStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField("tableName", setTableRemarksStatement.getTableName());
		return validationErrors;
	}

	@Override
    public Sql[] generateSql(SetTableRemarksStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
		String sql;
		String remarksEscaped = database.escapeStringForDatabase(StringUtil.trimToEmpty(statement.getRemarks()));
		if (database instanceof MySQLDatabase) {
			sql = "ALTER TABLE " + database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName()) + " COMMENT = '" + remarksEscaped
					+ "'";
		} else if (database instanceof MSSQLDatabase) {
			String schemaName = statement.getSchemaName();
			if (schemaName == null) {
				schemaName = database.getDefaultSchemaName() != null ? database.getDefaultSchemaName() : "dbo";
			}
			String tableName = statement.getTableName();
			String qualifiedTableName = String.format("%s.%s", schemaName, statement.getTableName());

			sql = "IF EXISTS( " +
					" SELECT extended_properties.value" +
					" FROM sys.extended_properties" +
					" WHERE major_id = OBJECT_ID('" + qualifiedTableName + "')" +
					" AND name = N'MS_DESCRIPTION'" +
					" AND minor_id = 0" +
					" )" +
					" BEGIN " +
					" EXEC sys.sp_updateextendedproperty @name = N'MS_Description'" +
					" , @value = N'" + remarksEscaped + "'" +
					" , @level0type = N'SCHEMA'" +
					" , @level0name = N'" + schemaName + "'" +
					" , @level1type = N'TABLE'" +
					" , @level1name = N'" + tableName + "'" +
					" END " +
					" ELSE " +
					" BEGIN " +
					" EXEC sys.sp_addextendedproperty @name = N'MS_Description'" +
					" , @value = N'" + remarksEscaped + "'" +
					" , @level0type = N'SCHEMA'" +
					" , @level0name = N'" + schemaName + "'" +
					" , @level1type = N'TABLE'" +
					" , @level1name = N'" + tableName + "'" +
					" END";
		} else {
			sql = "COMMENT ON TABLE " + database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName()) + " IS '"
					+ remarksEscaped + "'";
		}

		return new Sql[] { new UnparsedSql(sql, getAffectedTable(statement)) };
	}

    protected Relation getAffectedTable(SetTableRemarksStatement statement) {
        return new Table().setName(statement.getTableName()).setSchema(statement.getCatalogName(), statement.getSchemaName());
    }
}
