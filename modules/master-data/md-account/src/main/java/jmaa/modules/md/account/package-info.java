@Manifest(
    name = "md-account",
    label = "会计基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Bank.class,
        Currency.class,
        PaymentTerm.class,
    },
    demo = {
    },
    data = {
        "views/menus.xml",
        "views/bank.xml",
        "views/currency.xml",
        "views/payment_term.xml",
    },
    depends = {
    },
    application = false)
package jmaa.modules.md.account;

import jmaa.modules.md.account.models.*;
import org.jmaa.sdk.Manifest;
