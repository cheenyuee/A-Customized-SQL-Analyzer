![](C:\Users\16977\Desktop\calcite\毕业设计\SQL处理流程.png)

## 语法树解析

语法树解析模块将一条SQL解析成对应的抽象语法树（SqlNode节点树）。它会校验语法符不符合规范，但不会检查SQL中用到的列在表中是否存在。后者是Validator去做的事情。

- 通过 JavaCC 模版生成 LL(k) 语法分析器，主模版是 Parser.jj；可对其进行扩展
- 负责处理各个 Token，逐步生成一棵 SqlNode 组成的 AST

## 验证 Validator

使用 Catalog 中的元数据检验 SqlNode AST，如SQL中用到的列是否存在，以及数据类型是否正确。

因为它是和具体数据相关的，而Apache Calcite中不负责数据处理，也不保存任何数据，所以我们需要自定义适配器来适配这部分。

model.json中定义了schemas名称，类型，使用哪个SchemaFactory来进行处理，以及各个表的ddl和data文件的位置。我们实现SchemaFactory,然后在查询时指定正确的model.json文件就可以了。

> **Catalog** – 定义元数据和命名空间，包含 Schema（库）、Table（表）、RelDataType（类型信息）

## 逻辑计划生成

由SqlToRelConverter将SqlNode节点树转化为代表逻辑计划的RelNode（Relational Node）关系代数表达式树；

## 查询优化

Query Optimizer里内置了100+的转换规则，分别用于逻辑计划优化以及物理计划转换；

以下是一些常见的优化规则（Rules）：

- 移除未使用的字段
- 合并多个投影（projection）列表
- 使用 JOIN 来代替子查询
- 对 JOIN 列表重排序
- 下推（push down）投影项
- 下推过滤条件



## 概念

1. **关系代数RelNode（Relational algebra）**：即关系表达式。它们通常以动词命名，例如 Sort, Join, Project, Filter, Scan, Sample.
2. **行表达式RexNode（Row expressions）**：例如 RexLiteral (常量), RexVariable (变量), RexCall (调用) 等，例如投影列表（Project）、过滤规则列表（Filter）、JOIN 条件列表和 ORDER BY 列表、WINDOW 表达式、函数调用等。使用 RexBuilder 来构建行表达式。
3. **表达式有各种特征（Trait）**：使用 Trait 的 satisfies() 方法来测试某个表达式是否符合某 Trait 或 Convention.
4. **转化特征（Convention）**：属于 Trait 的子类，用于转化 RelNode 到具体平台实现（可以将下文提到的 Planner 注册到 Convention 中）. 例如 JdbcConvention，FlinkConventions.DATASTREAM 等。同一个关系表达式的输入必须来自单个数据源，各表达式之间通过 Converter 生成的 Bridge 来连接。
5. **规则（Rules）**：用于将一个表达式转换（Transform）为另一个表达式。它有一个由 RelOptRuleOperand 组成的列表来决定是否可将规则应用于树的某部分。
6. **规划器（Planner）** ：即请求优化器，它可以根据一系列规则和成本模型，来将一个表达式转为语义等价（但效率更优）的另一个表达式。例如：
   - VolcanoPlanner：基于成本的优化模型 （CBO优化器）
   - HepPlanner：基于规则的启发式优化模型（ RBO优化器）

## 参考文献

Calcite中主要数据结构介绍https://zhuanlan.zhihu.com/p/65701467

Calcite中定制SQL解析器https://www.jianshu.com/p/e4f883a3b969

**DogQC**https://github.com/Henning1/dogqc

**逄淑越CalciteDemo**[AlstonWilliams/CalciteDemo: Apache Calcite Demo (github.com)](https://github.com/AlstonWilliams/CalciteDemo)

Apache Calcite 学习文档https://blog.csdn.net/QXC1281/article/details/89070285