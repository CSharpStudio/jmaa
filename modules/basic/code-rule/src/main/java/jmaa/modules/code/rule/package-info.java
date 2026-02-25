@Manifest(
    name = "code-rule",
    label = "编码规则",
    category = "基础模块",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        //编码规则
        CodeCoding.class,
        CodePart.class,
        CodeRule.class,
        CodeMatcher.class,
        PartCharsCode.class,
        PartDateTemplate.class,
        PartGlobalSequence.class,
        PartDaySequence.class,
        PartMonthSequence.class,
        AutoCodeMixin.class,
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/code_coding.xml",
        "views/code_matcher.xml",
    })
package jmaa.modules.code.rule;

import org.jmaa.sdk.Manifest;
import jmaa.modules.code.rule.models.*;
