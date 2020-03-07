package dev.itsmeow.twitchplays.client.task;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class TaskEquip extends Task {
    private String itemName;
    private int meta = 0;

    public TaskEquip(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public boolean parse(String... args) {
        if(args.length == 2 || args.length == 3) {
            itemName = args[1];
            if(itemName.indexOf(":") == -1) {
                itemName = "minecraft:" + itemName;
            }
            if(args.length == 3) {
                try {
                    meta = Integer.parseInt(args[2]);
                } catch(NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int maxActiveTime() {
        return 0;
    }

    @Override
    protected void update() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
        if(item == null) {
            return;
        }
        ItemStack query = new ItemStack(item, 1, meta);
        if(query != null && query.getItem() != null) {
            query.setItemDamage(meta);
            int slot = searchPlayerInventory(query);
            if(slot != -1) {
                if(slot < 9) {
                    player.inventory.currentItem = slot;
                } else {
                    ItemStack tempStack = player.getHeldItemMainhand();
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, player.inventory.getStackInSlot(slot));
                    player.inventory.setInventorySlotContents(slot, tempStack);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "equip";
    }

    private int searchPlayerInventory(ItemStack stack) {
        for(int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack item = player.inventory.getStackInSlot(i);
            if(ItemStack.areItemsEqual(item, stack)) {
                return i;
            }
        }
        return -1;
    }
}
