@Manifest(
    name = "board",
    label = "看板设计",
    category = "基础模块",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        BoardService.class,
        BoardDesigner.class,
        BoardTemplate.class,
    },
    controllers = {
        BoardController.class,
    },
    data = {
        "data/jwt.xml",
        "views/menus.xml",
        "views/board_designer.xml",
        "views/board_template.xml",
    })
package jmaa.modules.board;

import org.jmaa.sdk.Manifest;
import jmaa.modules.board.controllers.BoardController;
import jmaa.modules.board.models.BoardDesigner;
import jmaa.modules.board.models.BoardService;
import jmaa.modules.board.models.BoardTemplate;
