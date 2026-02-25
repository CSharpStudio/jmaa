package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "stock.onhand")
@Model.Service(remove = "@edit")
public class Onhand extends Model {
}
