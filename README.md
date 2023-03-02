# A-Customized-SQL-Analyzer
This is a CustomizedSQL analyzer based on Apache-Calcite.

这是一个基于Apache-Calcite的定制SQL解释器。

## Tools

To use this SQL-analyzer, you may need the following tools:

- Javacc
- FreeMarker 

## RUN

You can simply run it by implementing the main method.

If you want to design your own SQL-parser, you can do the following steps:

- edit the files in the directory called codegen.

  ```java
  // 设置生成的包目录和类名
  parser: {
      # 包目录
      package: "Customized.parser.impl",
      # 类名
      class: "CustomizedSqlParserImpl",
  }
  ```

- edit pom.xml and set the file directory.

  ```markdown
  <!-- 配置文件地址 -->
  <cfgFile>src/main/codegen/config.fmpp</cfgFile>
  <!-- 文件模板存放目录 -->
  <templateDirectory>src/main/codegen/templates</templateDirectory>
  <!-- 文件输出目录 -->
  <outputDirectory>target/generated-sources/fmpp</outputDirectory>
  ```

- use the maven command fmpp:generate. Then, a new parser.jj file will be generated in outputDirectory.

- use the Javacc command. 

  ```shell
  javacc parser.jj
  ```

  Then, Javacc will generate seven files, like:

  - CustomizedSqlParserImpl
  - CustomizedSqlParserImplConstants
  - CustomizedSqlParserImplTokenManager
  - ParseException
  - SimpleCharStream
  - Token
  - TokenMgrError

- move these files into package *org.apache.calcite.sql.parser.impl*. 

  Then, you can import the *CustomizedSqlParserImpl* as your own SQL-parser in your code.

  ```java
  import Customized.parser.impl.CustomizedSqlParserImpl;
  SqlParser.Config mysqlConfig = SqlParser.configBuilder().setParserFactory(CustomizedSqlParserImpl.FACTORY).setLex(Lex.MYSQL).build();
  // CustomizedSqlParserImpl是自定义的解析工厂
  ```

## Repository structure

We select some important files for detailed description.

```
|-- Project/ # the Java project
    |-- src/ # 
    	|-- main/ # 
    		|-- codegen/ # the freemarker files
    			|-- includes # 
    			|-- templates # 
    			|-- config.fmpp # 
    		|-- java/ # the java code files
    			|-- CustomizedAnalyzer # the main class
    			|-- Customized.parser.impl # the customized SQL parser package
 
|-- Documents/  # the documents and references
```

