package com.example.lifesteal.utils;

import com.example.lifesteal.LifeStealPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemUtils {

    private static final NamespacedKey HEART_KEY = new NamespacedKey(LifeStealPlugin.getInstance(), "heart_item");

    public static ItemStack getHeartItem() {
        Material material = Material.valueOf(LifeStealPlugin.getInstance().getConfig().getString("revive.item-material", "TOTEM_OF_UNDYING"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = LifeStealPlugin.getInstance().getConfig().getString("messages.heart-item-name", "&c&lHeart");
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(List.of(
                org.bukkit.ChatColor.GRAY + "Right-click to gain +1 max heart!"
        ));

        meta.getPersistentDataContainer().set(HEART_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHeartItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(HEART_KEY, PersistentDataType.BYTE);
    }
}
