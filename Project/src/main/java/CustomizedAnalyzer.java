import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.config.Lex;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rel.rules.CoreRules;  // 基于规则的优化
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.OracleSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.impl.CustomizedSqlParserImpl;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.tools.*;
import java.util.Properties;


public class CustomizedAnalyzer {

    public static void main(String[] args) throws SqlParseException {
        String sql = "select name from student where id = 857 and age = 888 group by name";

        SqlNode sqlNode = parserSql(sql);  // SQL解析
        System.out.println(sqlNode.toSqlString(OracleSqlDialect.DEFAULT));// 还原某个方言的SQL

        RelNode relNode = parserSqlNode(sqlNode);  // SqlNode转换RelNode
        System.out.println(RelOptUtil.toString(relNode));

        RelNode optimizedRelNode = optimizeRelNode(relNode);  // 查询优化
        System.out.println(RelOptUtil.toString(optimizedRelNode));
    }


    /**
     * 解析SQL
     **/
    public static SqlNode parserSql(String sql) throws SqlParseException {
        // 解析配置 - mysql设置
        // SqlParserImpl是默认的的解析工厂，CustomizedSqlParserImpl是定制的的解析工厂
        SqlParser.Config mysqlConfig = SqlParser.configBuilder().setParserFactory(CustomizedSqlParserImpl.FACTORY).setLex(Lex.MYSQL).build();
        // 创建解析器
        SqlParser parser = SqlParser.create("", mysqlConfig);
        // 解析sql
        SqlNode sqlNode = parser.parseQuery(sql);
        return sqlNode;
    }

    /**
     * 解析SQL Node
     **/
    public static RelNode parserSqlNode(SqlNode sqlNode) {
        SchemaPlus rootSchema = Frameworks.createRootSchema(true);

        rootSchema.add("student", new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                RelDataTypeFactory.Builder builder = typeFactory.builder();

                builder.add("id", typeFactory.createTypeWithNullability(typeFactory.createSqlType(SqlTypeName.BIGINT), true));
                builder.add("name", typeFactory.createTypeWithNullability(typeFactory.createSqlType(SqlTypeName.VARCHAR), true));
                builder.add("class", typeFactory.createTypeWithNullability(typeFactory.createSqlType(SqlTypeName.VARCHAR), true));
                builder.add("age", typeFactory.createTypeWithNullability(typeFactory.createSqlType(SqlTypeName.BIGINT), true));

                return builder.build();
            }
        });

        FrameworkConfig frameworkConfig = Frameworks.newConfigBuilder().defaultSchema(rootSchema).build();

        Properties cxnConfig = new Properties();
        cxnConfig.setProperty(
                CalciteConnectionProperty.CASE_SENSITIVE.camelName(),
                String.valueOf(frameworkConfig.getParserConfig().caseSensitive()));

        CalciteCatalogReader catalogReader = new CalciteCatalogReader(
                CalciteSchema.from(rootSchema),
                CalciteSchema.from(frameworkConfig.getDefaultSchema()).path(null),
                new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT),
                new CalciteConnectionConfigImpl(cxnConfig)
        );

        SqlValidator validator = SqlValidatorUtil.newValidator(
                frameworkConfig.getOperatorTable(),
                catalogReader,
                new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT)
        );

        SqlNode validatedSqlNode = validator.validate(sqlNode);
        RelOptCluster relOptCluster = RelOptCluster.create(new VolcanoPlanner(), new RexBuilder(new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT)));


        SqlToRelConverter relConverter = new SqlToRelConverter(
                null,
                validator,
                catalogReader,
                relOptCluster,
                frameworkConfig.getConvertletTable()
        );
        RelRoot relRoot = relConverter.convertQuery(validatedSqlNode, false, true);
        RelNode relNode = relRoot.rel;// RelNode relNode = relRoot.project();
        return relNode;
    }

    public static RelNode optimizeRelNode(RelNode relNode) {
        HepProgramBuilder builder = new HepProgramBuilder();
        // 优化规则
        builder.addRuleInstance(CoreRules.AGGREGATE_REMOVE);
        builder.addRuleInstance(CoreRules.PROJECT_FILTER_TRANSPOSE);
        builder.addRuleInstance(CoreRules.FILTER_PROJECT_TRANSPOSE);
        HepPlanner hepPlanner = new HepPlanner(builder.build());
        hepPlanner.setRoot(relNode);
        RelNode bestRelNode = hepPlanner.findBestExp();
        return bestRelNode;

    }


}