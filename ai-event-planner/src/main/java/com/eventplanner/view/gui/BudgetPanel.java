package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import java.util.*;
final class BudgetPanel {
 private final Dashboard d; BudgetPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();Event e=d.session.active();var names=new DisplayNames(e);boolean exists=d.controller.hasBudgetDraft(id)||e.getBudget()!=null;Budget budget=exists?d.controller.viewBudget(id):null;
  var table=Ui.table(exists?budget.items():List.<BudgetItem>of());Ui.column(table,"Category",BudgetItem::category);Ui.column(table,"Description",BudgetItem::description);Ui.column(table,"Quantity",BudgetItem::quantity);Ui.column(table,"Unit price",BudgetItem::unitCost);Ui.column(table,"Total",BudgetItem::total);Ui.column(table,"Price status",BudgetItem::priceStatus);d.remember(table,"budgetItems",BudgetItem::id);
  var priority=Ui.choice(List.of("budget","community","attendance"));
  d.rememberChoice(priority,"budgetPriority",p->p);
  var edit=Ui.button("Edit allocation",()->d.action(()->{BudgetItem item=Ui.selected(table);
   new Ui.Form().field("Category",item.category()).field("Description",item.description()).field("Quantity",item.quantity()).field("Unit cost (blank if UNKNOWN)",item.unitCost()).field("Price status (CONFIRMED / ESTIMATED / UNKNOWN)",item.priceStatus()).field("Source / quote / assumption",item.source()).show("Edit budget allocation",f->{
    var status=GuiInputs.enumeration(BudgetItem.PriceStatus.class,f.get("Price status (CONFIRMED / ESTIMATED / UNKNOWN)"));
    var cost=f.get("Unit cost (blank if UNKNOWN)").isBlank()?null:GuiInputs.money(f.get("Unit cost (blank if UNKNOWN)"));
    d.controller.editBudget(id,new BudgetItem(item.id(),f.get("Category"),f.get("Description"),GuiInputs.money(f.get("Quantity")),cost,status,f.get("Source / quote / assumption")));d.edited();
   });
  }));
  boolean draft=d.controller.hasBudgetDraft(id);var approve=Ui.button("Approve budget",()->d.action(()->{if(Ui.confirm("Approve budget?","Approve the displayed allocations? Estimates remain estimates."))d.background("Approving budget",()->{d.controller.approveBudget(id);return null;});}));approve.setDisable(!draft);
  var discard=Ui.button("Discard draft",()->d.action(()->{if(Ui.confirm("Discard budget draft?","Retain the approved budget?"))d.controller.discardBudget(id);}));discard.setDisable(!draft);
  String totals=exists?"Known subtotal: "+d.controller.budgetTotal(id)+" | Remaining against known subtotal: "+d.controller.budgetRemaining(id)+" "+budget.currency():"No budget yet.";
  return Ui.page("Budget & catering","Unknown costs are never treated as free. Approved estimates are not verified supplier quotes.",
   Ui.row(Ui.button("Configure catering needs",()->needs(id,e)),priority,Ui.button("Generate budget",()->{String p=priority.getValue();d.background("Generating budget and catering",()->d.controller.generateBudget(id,p));})),
   Ui.card(draft?"DRAFT budget":exists?"APPROVED budget":"No budget yet",exists?Ui.metrics(Ui.metric("Spending limit",DisplayNames.money(e.getTotalBudget(),budget.currency())),Ui.metric("Known subtotal",DisplayNames.money(d.controller.budgetTotal(id),budget.currency())),Ui.metric("Remaining vs known subtotal",DisplayNames.money(d.controller.budgetRemaining(id),budget.currency()))):Ui.note(totals),Ui.row(edit,Ui.button("Recalculate & validate",()->d.validate(id,"budget")),approve,discard),Ui.inspectView(table,item->ResultViews.budgetItem(item,budget.currency())),ResultViews.warnings("Budget review",exists?d.diagnostics(id,"budget"):"Generate a budget proposal to begin.",names)),
   Ui.card("Catering recommendations & assumptions",exists?ResultViews.catering(budget,names):Ui.note("No catering recommendations yet.")));
 }
 private void needs(String id,Event e){new Ui.Form().field("Currency (ISO code)",e.getPlanningCurrency()).field("Dietary requirements (comma-separated)",String.join(", ",e.getDietaryRequirements())).field("Catering needs",e.getCateringNeeds()).show("Budget & catering needs",f->{
  d.controller.configureBudgetNeeds(id,f.get("Currency (ISO code)").toUpperCase(Locale.ROOT),GuiInputs.csv(f.get("Dietary requirements (comma-separated)")),f.get("Catering needs"));d.edited();
 });}
}
