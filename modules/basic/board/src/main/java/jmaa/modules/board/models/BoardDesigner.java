package jmaa.modules.board.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.util.FileInfo;
import org.jmaa.sdk.util.KvMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "board.designer", label = "看板设计器", inherit = "board.service")
public class BoardDesigner extends Model {
    static Field title = Field.Char().label("标题").required();
    static Field state = Field.Selection(new Options() {{
        put("0", "未发布");
        put("1", "已发布");
    }}).defaultValue("0").label("发布状态");
    static Field image = Field.Image().label("缩略图");
    static Field content = Field.Text().label("画布");
    static Field menu_id = Field.Many2one("ir.ui.menu").label("菜单").ondelete(DeleteMode.SetNull);

    @ServiceMethod(label = "更新状态")
    public Object updateState(Records records, String state) {
        records.set("state", state);
        return Action.success();
    }

    @OnSaved("state")
    public void onStateSave(Records records) {
        for (Records record : records) {
            String state = record.getString("state");
            Records menu = record.getRec("menu_id");
            if ("1".equals(state)) {
                if (menu.any()) {
                    menu.set("active", true);
                    menu.set("url", "/" + record.getEnv().getRegistry().getTenant().getKey() + "/board#/chart/preview/" + record.getId());
                } else {
                    menu = menu.create(new KvMap()
                        .set("name", record.get("title"))
                        .set("parent_id", record.getEnv().getRef("board.board_menus").getId())
                        .set("url", "/" + record.getEnv().getRegistry().getTenant().getKey() + "/board#/chart/preview/" + record.getId())
                        .set("icon", "/web/jmaa/modules/board/statics/menu.png")
                        .set("target", "blank"));
                    record.set("menu_id", menu.getId());
                }
            } else {
                menu.set("active", false);
            }
        }
    }

    @Override
    public void update(Records records, Map<String, Object> values) {
        if (values.containsKey("image")) {
            for (Records record : records) {
                List<Map<String, Object>> images = (List<Map<String, Object>>) record.get("image");
                if (Utils.isNotEmpty(images)) {
                    List<String> imageIds = images.stream().map(m -> (String) m.get("id")).collect(Collectors.toList());
                    records.getEnv().get("ir.attachment", imageIds).delete();
                }
            }
        }
        callSuper(records, values);
    }

    @ServiceMethod(label = "保存为模板")
    public Object saveAsTemplate(Records records, String name, String remark) {
        Records attachment = records.getEnv().get("ir.attachment");
        List<Map<String, Object>> toCreate = new ArrayList<>();
        for (Records record : records) {
            String content = record.getString("content");
            byte[] image = (byte[]) attachment.call("getFileDataByRes", record.getMeta().getName(), "image", record.getId());
            toCreate.add(new KvMap()
                .set("name", name)
                .set("remark", remark)
                .set("content", content)
                .set("image", new FileInfo().setName("preview.png").setData(image)));
        }
        records.getEnv().get("board.template").createBatch(toCreate);
        return Action.success();
    }

    @ServiceMethod(label = "查询模板", auth = "read")
    public Object searchTemplate(Records records, List<String> fields, String keyword, Integer offset, Integer limit) {
        Criteria criteria = new Criteria();
        if (Utils.isNotEmpty(keyword)) {
            criteria.and("name", "like", keyword).or("remark", "like", keyword);
        }
        return records.getEnv().get("board.template").searchLimit(fields, criteria, offset, limit, "");
    }

    @ServiceMethod(label = "查询模板数量", auth = "read")
    public Object countTemplate(Records records, String keyword) {
        Criteria criteria = new Criteria();
        if (Utils.isNotEmpty(keyword)) {
            criteria.and("name", "like", keyword).or("remark", "like", keyword);
        }
        return records.getEnv().get("board.template").count(criteria);
    }

    @ServiceMethod(label = "查询模板数量", auth = "read")
    public Object readTemplate(Records records, String templateId, List<String> fields) {
        return records.getEnv().get("board.template", templateId).read(fields);
    }

    @ServiceMethod(label = "查询模板数量", auth = "read")
    public Object deleteTemplate(Records records, String templateId) {
        records.getEnv().get("board.template", templateId).delete();
        return Action.success();
    }
}
