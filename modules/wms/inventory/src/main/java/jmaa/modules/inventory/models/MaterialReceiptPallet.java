package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "wms.material_receipt_pallet", label = "码盘", inherit = "packing.package", table = "md_package", authModel = "wms.material_receipt")
public class MaterialReceiptPallet extends Model {

}
