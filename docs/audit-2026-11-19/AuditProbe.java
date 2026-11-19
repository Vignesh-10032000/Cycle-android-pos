import dev.vignesh.cycleshop.ui.invoice.*;
import dev.vignesh.cycleshop.data.local.model.*;
import java.util.*;
public class AuditProbe {
 public static void main(String[] args) {
  for(Object[] x:new Object[][]{{"Set",3.0,InventoryUnit.PIECE},{"PC",3.0,InventoryUnit.GROSS},{"Pair",3.0,InventoryUnit.GROSS},{"Dozen",3.0,InventoryUnit.SET},{"PC",0.5,InventoryUnit.PIECE},{"PC",Double.NaN,InventoryUnit.PIECE},{"Gross",Double.POSITIVE_INFINITY,InventoryUnit.PIECE}})
   System.out.println("CONVERSION "+Arrays.toString(x)+" => "+UnitConversionEngine.INSTANCE.convert((String)x[0],(Double)x[1],(InventoryUnit)x[2]));
  for(String text:new String[]{"STRYDER\nInvoice No: AUDIT-999", "M.STEEL\nInvoice No: AUDIT-888"}) {
   ParsedInvoice p=new InvoiceTableParser().parse(new OcrResult(text,Collections.emptyList(),0L,100,100,0));
   System.out.println("HEADER_ONLY "+text+" => rows="+p.getItems().size()+", quantity="+p.getSumItemQuantities()+", total="+p.getTotalAmountExtractedPaise()+", reconciled="+p.isQuantityReconciled());
  }
  for(Double q:new Double[]{Double.NaN,Double.POSITIVE_INFINITY})
   System.out.println("VALIDATION "+q+" => "+PurchaseValidationEngine.INSTANCE.validateLine(1,"Audit product",1L,q,100L,Collections.emptyMap()));
  System.out.println("VALIDATION blank => "+PurchaseValidationEngine.INSTANCE.validateLine(1,"",1L,1.0,100L,Collections.emptyMap()));
 }
}