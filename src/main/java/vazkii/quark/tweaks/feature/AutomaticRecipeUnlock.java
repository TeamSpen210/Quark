package vazkii.quark.tweaks.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.RecipeToast;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import scala.actors.threadpool.Arrays;
import vazkii.quark.base.lib.LibObfuscation;
import vazkii.quark.base.module.Feature;

public class AutomaticRecipeUnlock extends Feature {

	List<String> ignored;

	boolean forceLimitedCrafting;
	boolean disableRecipeBook;

	@Override
	public void setupConfig() {
		String[] ignoredArr = loadPropStringList("Ignored Recipes", "A list of recipe names that should NOT be added in by default", new String[0]);
		ignored = Arrays.asList(ignoredArr);

		forceLimitedCrafting = loadPropBool("Force Limited Crafting", "Set to true to force the doLimitedCrafting gamerule to true.\n"
				+ "Combine this with the Ignored Recipes list to create a system where only a few selected recipes are locked.", false);

		disableRecipeBook = loadPropBool("Disable Recipe Book", "Set this to true to disable the vanilla recipe book altogether.", false);
	}

	@SubscribeEvent 
	public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		if(event.player instanceof EntityPlayerMP) {
			ArrayList<IRecipe> recipes = Lists.newArrayList(CraftingManager.REGISTRY);
			recipes.removeIf((recipe) -> ignored.contains(recipe.getRegistryName().toString()) || recipe.getRecipeOutput().isEmpty());
			((EntityPlayerMP) event.player).unlockRecipes(recipes);

			if(forceLimitedCrafting)
				event.player.world.getGameRules().setOrCreateGameRule("doLimitedCrafting", "true");
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onInitGui(InitGuiEvent.Post event) {
		GuiScreen gui = event.getGui();
		if(disableRecipeBook && (gui instanceof GuiInventory || gui instanceof GuiCrafting)) {
			Minecraft.getMinecraft().player.getRecipeBook().setGuiOpen(false);
			event.getButtonList().removeIf((b) -> b.id == 10);
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void clientTick(ClientTickEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.player != null && mc.player.ticksExisted < 20) {
			GuiToast toasts = mc.getToastGui();
			Queue<IToast> toastQueue = ReflectionHelper.getPrivateValue(GuiToast.class, toasts, LibObfuscation.TOASTS_QUEUE);
			for(IToast toast : toastQueue)
				if(toast instanceof RecipeToast) {
					RecipeToast rtoast = (RecipeToast) toast;
					List<ItemStack> stacks = ReflectionHelper.getPrivateValue(RecipeToast.class, rtoast, LibObfuscation.RECIPES_OUTPUTS);
					if(stacks.size() > 100) {
						toastQueue.remove(toast);
						return;
					}
				}
		}

	}

	@Override
	public boolean hasSubscriptions() {
		return true;
	}

}
