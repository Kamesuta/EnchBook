package tk.shanebee.enchBook.events;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import tk.shanebee.enchBook.Config;
import tk.shanebee.enchBook.EnchBook;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("FieldCanBeLocal")
public class AnvilPrepare implements Listener {

    private final EnchBook plugin;
    private final boolean SAFE_ENCHANTS;
    private final boolean SAFE_BOOKS;
    private final boolean IGNORE_CONFLICTS;
    private final Map<Enchantment, Integer> MAX_LEVELS;
    private final boolean REQ_PERM;
    private final boolean REQ_PERM_ITEM;
    private final String PERM_BYPASS_SAFE = "enchbook.bypass.safe";
    private final String PERM_BYPASS_VANILLA = "enchbook.bypass.vanilla_max_level";
    private final String PERM_BYPASS_VANILLA_ITEM = "enchbook.bypass.vanilla_max_level_item";
    private final String PERM_BYPASS_MAX = "enchbook.bypass.max_level";

    public AnvilPrepare(EnchBook instance) {
        this.plugin = instance;
        Config config = plugin.getPluginConfig();
        SAFE_ENCHANTS = config.SAFE_ENCHANTS;
        SAFE_BOOKS = config.SAFE_BOOKS;
        IGNORE_CONFLICTS = config.IGNORE_CONFLICTS;
        MAX_LEVELS = config.MAX_LEVELS;
        REQ_PERM = config.ABOVE_VAN_REQUIRES_PERM;
        REQ_PERM_ITEM = config.ABOVE_VAN_REQUIRES_PERM_ITEM;
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        List<HumanEntity> viewers = event.getViewers();
        if (viewers.size() == 0) return;

        Player player = (Player) viewers.get(0);
        AnvilInventory inventory = event.getInventory();
        if (!SAFE_ENCHANTS || player.hasPermission(PERM_BYPASS_SAFE)) {
            ItemStack FIRST_ITEM = inventory.getItem(0);
            ItemStack SECOND_ITEM = inventory.getItem(1);

            if ((FIRST_ITEM == null) || (SECOND_ITEM == null)) return;

            if (SECOND_ITEM.getType() == Material.ENCHANTED_BOOK) {
                if (FIRST_ITEM.getType() != Material.ENCHANTED_BOOK) {

                    ItemStack result = FIRST_ITEM.clone();
                    ItemMeta bookMeta = SECOND_ITEM.getItemMeta();
                    assert bookMeta != null;
                    for (Enchantment enchantment : ((EnchantmentStorageMeta) bookMeta).getStoredEnchants().keySet()) {
                        if (canEnchant(FIRST_ITEM, enchantment)) {
                            int bookLevel = ((EnchantmentStorageMeta) bookMeta).getStoredEnchantLevel(enchantment);
                            int itemLevel = FIRST_ITEM.getEnchantmentLevel(enchantment);
                            if (itemLevel < bookLevel) {
                                result.addUnsafeEnchantment(enchantment, bookLevel);
                            } else if (itemLevel == bookLevel) {
                                if (itemLevel >= enchantment.getMaxLevel() && REQ_PERM_ITEM && !player.hasPermission(PERM_BYPASS_VANILLA_ITEM)) {
                                    continue;
                                }
                                result.addUnsafeEnchantment(enchantment, bookLevel + 1);
                            }
                        }
                    }
                    event.setResult(result);
                    if (inventory.getRepairCost() < 0) {
                        inventory.setRepairCost(5);
                    }

                } else {
                    ItemStack result = new ItemStack(Material.ENCHANTED_BOOK);
                    ItemMeta newMeta = result.getItemMeta();
                    ItemMeta bookMeta = SECOND_ITEM.getItemMeta();
                    ItemMeta itemMeta = FIRST_ITEM.getItemMeta();

                    for (Enchantment enchantment : Enchantment.values()) {
                        assert bookMeta != null;
                        assert itemMeta != null;
                        int lvl1 = ((EnchantmentStorageMeta) bookMeta).getStoredEnchantLevel(enchantment);
                        int lvl2 = ((EnchantmentStorageMeta) itemMeta).getStoredEnchantLevel(enchantment);
                        if (lvl1 == lvl2 && lvl1 > 0) {
                            int newLvl = canEnchantAboveMax(lvl1 + 1, enchantment, player) ? lvl1 + 1 : lvl1;
                            assert newMeta != null;
                            ((EnchantmentStorageMeta) newMeta).addStoredEnchant(enchantment, newLvl, true);
                        } else {
                            int newLvl = Math.max(lvl1, lvl2);
                            if (newLvl > 0) {
                                assert newMeta != null;
                                ((EnchantmentStorageMeta) newMeta).addStoredEnchant(enchantment, newLvl, true);
                            }
                        }
                    }
                    result.setItemMeta(newMeta);
                    event.setResult(result);
                }
            }
        }
    }

    private boolean canEnchantAboveMax(int level, Enchantment enchantment, Player player) {
        if (SAFE_BOOKS && level > enchantment.getMaxLevel() && !player.hasPermission(PERM_BYPASS_SAFE)) {
            return false;
        }
        if (!SAFE_BOOKS) {
            if (level > enchantment.getMaxLevel() && REQ_PERM) {
                return player.hasPermission(PERM_BYPASS_VANILLA);
            }
            Integer integer = MAX_LEVELS.get(enchantment);
            if (integer == null) {
                return false;
            }
            if (level > integer) {
                return player.hasPermission(PERM_BYPASS_MAX);
            }
        }
        return true;
    }

    // Check if the ItemStack can accept this enchant
    public boolean canEnchant(ItemStack itemStack, Enchantment enchantment) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        AtomicBoolean canEnchant = new AtomicBoolean(enchantment.canEnchantItem(itemStack));

        if (itemMeta != null && !IGNORE_CONFLICTS) {
            itemMeta.getEnchants().keySet().forEach(ench -> {
                if (ench != enchantment && ench.conflictsWith(enchantment)) {
                    canEnchant.set(false);
                }
            });
        }
        return canEnchant.get();
    }

}
