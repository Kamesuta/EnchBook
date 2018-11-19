package tk.shanebee.enchBook.events;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import tk.shanebee.enchBook.EnchBook;

public class AnvilPrepare implements Listener {

    private static EnchBook plugin;
    public AnvilPrepare(EnchBook instance) {
        plugin = instance;
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent e) {
        if (!(plugin.getConfig().getBoolean("Options.Safe Enchants"))) {
            if ((e.getInventory().getItem(0) != null) && (e.getInventory().getItem(1) != null)) {
                if (e.getInventory().getItem(1).getType() == Material.ENCHANTED_BOOK) {
                    ItemStack item = e.getInventory().getItem(0);
                    ItemStack result = item.clone();
                    ItemMeta meta = e.getInventory().getItem(1).getItemMeta();
                    for (Enchantment enchantment : ((EnchantmentStorageMeta) meta).getStoredEnchants().keySet()) {
                        if (enchantment.canEnchantItem(item)) {
                            int lvl = ((EnchantmentStorageMeta) meta).getStoredEnchantLevel(enchantment);
                            if (item.getEnchantmentLevel(enchantment) < lvl) {
                                result.addUnsafeEnchantment(enchantment, lvl);
                            }
                        }
                    }
                    e.setResult(result);
                }
            }
        }
    }

}